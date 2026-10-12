#!/usr/bin/env python3
"""
Verifies that every hand-written Room Migration actually produces the schema Room expects.

Room validates the database after a migration and crashes on any mismatch, including details that
are easy to miss by eye: an index that exists but is not UNIQUE, a column default that differs,
or a primary key declared at the wrong position. Those failures only show up on a real device
running an upgrade, which is a slow way to learn about a typo.

This script seeds a database at the source version, runs the SQL lifted out of the Kotlin
migration (not a retype, so the two cannot drift), then diffs the result against the exported
target schema.

Usage:  python3 tools/verify_migrations.py
Exit code is non-zero if anything mismatches.
"""

from __future__ import annotations

import json
import re
import sqlite3
import sys
from pathlib import Path

PROJECT = Path(__file__).resolve().parent.parent
SCHEMA_DIR = PROJECT / "app" / "schemas" / "com.example.a48lawsofpower.data.LawDatabase"
DATABASE_KT = PROJECT / "app" / "src" / "main" / "java" / "com" / "example" / "a48lawsofpower" / "data" / "LawDatabase.kt"

# The three migrations declared in LawDatabase.Companion, oldest first.
MIGRATIONS = [("MIGRATION_3_4", 3, 4), ("MIGRATION_4_5", 4, 5)]


def load(version: int) -> dict:
    return json.loads((SCHEMA_DIR / f"{version}.json").read_text())


def migration_sql(name: str) -> list[str]:
    """Pulls the raw execSQL statements for a migration out of the Kotlin source."""
    src = DATABASE_KT.read_text()
    if f"val {name}" not in src:
        sys.exit(f"could not find `val {name}` in {DATABASE_KT}")
    block = src.split(f"val {name}")[1]
    block = block.split("\n        val ")[0]  # stop at the next declaration
    statements = re.findall(r'execSQL\(\s*"""(.*?)"""', block, re.S)
    statements += re.findall(r'execSQL\(\s*"(.*?)"', block, re.S)
    cleaned = []
    for s in statements:
        # Drop Kotlin string concatenation artefacts and collapse whitespace.
        s = s.replace('"', "").replace("'''", "")
        s = " ".join(s.split())
        if s:
            cleaned.append(s)
    return cleaned


def materialise(conn: sqlite3.Connection, schema: dict) -> None:
    conn.execute("PRAGMA foreign_keys = ON")
    for entity in schema["database"]["entities"]:
        table = entity["tableName"]
        conn.execute(entity["createSql"].replace("${TABLE_NAME}", table))
        for index in entity.get("indices", []):
            conn.execute(index["createSql"].replace("${TABLE_NAME}", table))


def actual_tables(conn: sqlite3.Connection) -> dict:
    out = {}
    for (table,) in conn.execute(
        "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'"
    ):
        cols = {}
        for cid, name, ctype, notnull, default, pk in conn.execute(f'PRAGMA table_info("{table}")'):
            cols[name] = {
                "type": ctype.upper(),
                "notNull": bool(notnull),
                "default": default,
                "pkPosition": pk,
            }
        indices = {}
        for row in conn.execute(f'PRAGMA index_list("{table}")'):
            iname, uniq = row[1], row[2]
            if str(iname).startswith("sqlite_autoindex"):
                continue  # implicit index behind a PRIMARY KEY / UNIQUE constraint
            idx_cols = [r[2] for r in conn.execute(f'PRAGMA index_info("{iname}")')]
            indices[iname] = {"unique": bool(uniq), "columns": idx_cols}
        fks = [
            {"table": r[2], "from": r[3], "to": r[4], "onDelete": r[6]}
            for r in conn.execute(f'PRAGMA foreign_key_list("{table}")')
        ]
        out[table] = {"columns": cols, "indices": indices, "foreignKeys": fks}
    return out


def expected_tables(schema: dict) -> dict:
    out = {}
    for entity in schema["database"]["entities"]:
        table = entity["tableName"]
        # Room records the primary key once per entity, as an ordered list of column names.
        pk_columns = entity.get("primaryKey", {}).get("columnNames", [])
        cols = {}
        for f in entity["fields"]:
            default = f.get("defaultValue")
            cols[f["columnName"]] = {
                "type": f["affinity"].upper(),
                "notNull": bool(f["notNull"]),
                # NB: do not strip quotes here. Room's exported defaultValue for a TEXT column declared
# `defaultValue = "''"` is the two-character string "''", and SQLite reports the same two
# characters back. Stripping would make every such column look like a mismatch.
                "default": None if default is None else str(default),
                "pkPosition": (
                    pk_columns.index(f["columnName"]) + 1
                    if f["columnName"] in pk_columns
                    else 0
                ),
            }
        indices = {}
        for index in entity.get("indices", []):
            indices[index["name"]] = {
                "unique": bool(index["unique"]),
                "columns": list(index["columnNames"]),
            }
        fks = [
            {
                "table": f["table"],
                "from": f["columns"][0],
                "to": f["referencedColumns"][0],
                "onDelete": f["onDelete"],
            }
            for f in entity.get("foreignKeys", [])
        ]
        out[table] = {"columns": cols, "indices": indices, "foreignKeys": fks}
    return out


def diff(expected: dict, actual: dict, problems: list[str]) -> None:
    for table in sorted(set(expected) | set(actual)):
        if table not in actual:
            problems.append(f"{table}: missing after migration")
            continue
        if table not in expected:
            problems.append(f"{table}: unexpected extra table")
            continue
        e, a = expected[table], actual[table]

        for col in sorted(set(e["columns"]) | set(a["columns"])):
            if col not in a["columns"]:
                problems.append(f"{table}.{col}: missing")
            elif col not in e["columns"]:
                problems.append(f"{table}.{col}: extra column")
            else:
                for field in ("type", "notNull", "default", "pkPosition"):
                    if e["columns"][col][field] != a["columns"][col][field]:
                        problems.append(
                            f"{table}.{col}.{field}: expected {e['columns'][col][field]!r}, "
                            f"got {a['columns'][col][field]!r}"
                        )

        for idx in sorted(set(e["indices"]) | set(a["indices"])):
            if idx not in a["indices"]:
                problems.append(f"{table}: index {idx} missing")
            elif idx not in e["indices"]:
                problems.append(f"{table}: unexpected index {idx}")
            else:
                for field in ("unique", "columns"):
                    if e["indices"][idx][field] != a["indices"][idx][field]:
                        problems.append(
                            f"{table}: index {idx}.{field}: expected "
                            f"{e['indices'][idx][field]!r}, got {a['indices'][idx][field]!r}"
                        )

        if len(e["foreignKeys"]) != len(a["foreignKeys"]):
            problems.append(
                f"{table}: expected {len(e['foreignKeys'])} foreign keys, got {len(a['foreignKeys'])}"
            )


def main() -> None:
    problems: list[str] = []
    for name, from_v, to_v in MIGRATIONS:
        source, target = load(from_v), load(to_v)
        conn = sqlite3.connect(":memory:")
        materialise(conn, source)
        conn.execute(f"PRAGMA user_version = {from_v}")
        for statement in migration_sql(name):
            conn.execute(statement)
        diff(expected_tables(target), actual_tables(conn), problems)
        conn.close()
        status = "FAIL" if problems else "ok"
        print(f"{name} ({from_v} -> {to_v}): {status}")

    if problems:
        print("\nRoom would reject these results:\n  " + "\n  ".join(problems))
        sys.exit(1)
    print("\nAll migrations reproduce the exported schema exactly.")


if __name__ == "__main__":
    main()
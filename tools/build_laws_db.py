#!/usr/bin/env python3
"""
Rebuilds the pre-packaged Room database at app/src/main/assets/database/laws.db.

The file MUST match the schema Room generates from LawEntity exactly - same columns, same
defaults, same identity hash - or Room refuses to open it and the app crashes on launch. The
authoritative copy of that schema is the JSON Room exports to
app/schemas/com.example.a48lawsofpower.data.LawDatabase/<version>.json, so this script reads the
createSql and identityHash straight out of it.

Content comes from the research batches in content/laws-*.json.

Usage:  python3 tools/build_laws_db.py [--content-dir DIR] [--version N]
"""

from __future__ import annotations

import argparse
import json
import sqlite3
import sys
from pathlib import Path

PROJECT = Path(__file__).resolve().parent.parent
SCHEMA_DIR = PROJECT / "app" / "schemas" / "com.example.a48lawsofpower.data.LawDatabase"
ASSET_DB = PROJECT / "app" / "src" / "main" / "assets" / "database" / "laws.db"

EXPECTED_LAWS = 48


REQUIRED_FIELDS = (
    "lawNumber",
    "title",
    "category",
    "summary",
    "core",
    "observance",
    "transgression",
    "reversal",
)


def load_content(content_dir: Path) -> list[dict]:
    laws: dict[int, dict] = {}
    for path in sorted(content_dir.glob("laws-*.json")):
        for entry in json.loads(path.read_text()):
            laws[entry["lawNumber"]] = entry

    missing = [n for n in range(1, EXPECTED_LAWS + 1) if n not in laws]
    if missing:
        sys.exit(f"content incomplete, missing laws: {missing}")
    if len(laws) != EXPECTED_LAWS:
        sys.exit(f"expected {EXPECTED_LAWS} laws, found {len(laws)}")

    ordered = [laws[n] for n in range(1, EXPECTED_LAWS + 1)]

    # A silently empty section ships as a blank page in the reader, which is far harder to spot
    # than a failed build.
    problems: list[str] = []
    for law in ordered:
        number = law["lawNumber"]
        for field in REQUIRED_FIELDS:
            if field not in law:
                problems.append(f"law {number}: missing field '{field}'")
            elif not str(law[field]).strip():
                problems.append(f"law {number}: '{field}' is empty")
        if len(law.get("summary", "")) > 140:
            problems.append(f"law {number}: summary is {len(law['summary'])} chars, max 140")

    expected_categories = {
        "Strategic action",
        "Managing relationships",
        "Controlling self-presentation",
        "Ultimate adaptability",
    }
    for law in ordered:
        if law.get("category") not in expected_categories:
            problems.append(f"law {law['lawNumber']}: unknown category {law.get('category')!r}")

    if problems:
        sys.exit("content validation failed:\n  " + "\n  ".join(problems))

    return ordered


def build(laws: list[dict], version: int) -> None:
    schema = json.loads((SCHEMA_DIR / f"{version}.json").read_text())
    identity_hash = schema["database"]["identityHash"]

    entities = schema["database"]["entities"]
    by_table = {e["tableName"]: e for e in entities}
    if "laws" not in by_table:
        sys.exit("schema has no 'laws' table")

    laws_entity = by_table["laws"]
    columns = [f["columnName"] for f in laws_entity["fields"]]

    if ASSET_DB.exists():
        ASSET_DB.unlink()

    conn = sqlite3.connect(ASSET_DB)
    try:
        # Create every table the schema declares, not just `laws`. Room validates the whole
        # database against the identity hash after opening it, so a pre-packaged asset that is
        # missing `highlights` or `reading_days` fails validation on a fresh install even though
        # every row of content is present. These stay empty; the app fills them at runtime.
        conn.execute("PRAGMA foreign_keys = ON")
        for entity in entities:
            table = entity["tableName"]
            conn.execute(entity["createSql"].replace("${TABLE_NAME}", table))
            for index in entity.get("indices", []):
                conn.execute(index["createSql"].replace("${TABLE_NAME}", table))

        conn.execute(
            "CREATE TABLE IF NOT EXISTS room_master_table "
            "(id INTEGER PRIMARY KEY, identity_hash TEXT)"
        )
        conn.execute(
            "INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, ?)",
            (identity_hash,),
        )

        rows = [
            (
                law["lawNumber"],
                law["category"],
                law["title"],
                law["summary"],
                law.get("core", ""),
                law.get("observance", ""),
                law.get("transgression", ""),
                law.get("reversal", ""),
                0,
                0,
            )
            for law in laws
        ]
        # Column order must match the entity declaration order in the exported schema. `id` is
        # left out so SQLite assigns it via AUTOINCREMENT.
        expected_order = [
            "law_number",
            "category",
            "title",
            "summary",
            "core",
            "observance",
            "transgression",
            "reversal",
            "bookmarked",
            "read",
        ]
        if columns != ["id", *expected_order]:
            sys.exit(f"schema column order changed, update this script:\n  {columns}")

        insert_columns = ", ".join(f"`{c}`" for c in expected_order)
        insert_slots = ", ".join("?" * len(expected_order))
        conn.executemany(
            f"INSERT INTO laws ({insert_columns}) VALUES ({insert_slots})", rows
        )
        conn.execute(f"PRAGMA user_version = {version}")
        conn.commit()

        stored = conn.execute("SELECT identity_hash FROM room_master_table").fetchone()[0]
        if stored != identity_hash:
            sys.exit("identity hash mismatch")
        count = conn.execute("SELECT COUNT(*) FROM laws").fetchone()[0]
        tables = [
            r[0]
            for r in conn.execute(
                "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name"
            )
        ]
        print(f"wrote {count} laws to {ASSET_DB.relative_to(PROJECT)}")
        print(f"schema v{version}, identity {identity_hash}")
        print(f"tables: {', '.join(tables)}")
    finally:
        conn.close()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--content-dir", type=Path, default=PROJECT / "content")
    parser.add_argument("--version", type=int, default=6)
    args = parser.parse_args()

    build(load_content(args.content_dir), args.version)


if __name__ == "__main__":
    main()

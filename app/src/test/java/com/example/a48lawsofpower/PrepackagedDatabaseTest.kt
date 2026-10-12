package com.example.a48lawsofpower

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/**
 * The app used to crash on launch because the pre-packaged database did not match the schema Room
 * generates from `LawEntity`. Room only copies the asset when the database file is absent, so a
 * mismatch stays invisible until a device actually opens it. These checks run on every build.
 *
 * Gradle runs unit tests with the module directory as the working directory.
 */
class PrepackagedDatabaseTest {

    private val schemaFile = File("schemas/com.example.a48lawsofpower.data.LawDatabase/4.json")
    private val assetFile = File("src/main/assets/database/laws.db")

    private val schemaJson: String
        get() {
            assertTrue(
                "app/schemas is missing - build once so Room can export it",
                schemaFile.exists(),
            )
            return schemaFile.readText()
        }

    private fun schemaValue(name: String): String =
        Regex("\"$name\"\\s*:\\s*\"([^\"]+)\"").find(schemaJson)?.groupValues?.get(1)
            ?: error("'$name' not found in ${schemaFile.path}")

    private fun schemaVersion(): Int =
        Regex("\"version\"\\s*:\\s*(\\d+)").find(schemaJson)?.groupValues?.get(1)?.toInt()
            ?: error("'version' not found in ${schemaFile.path}")

    @Test
    fun `asset is a sqlite database`() {
        assertTrue(
            "missing ${assetFile.path} - run tools/build_laws_db.py",
            assetFile.exists(),
        )
        val header = String(assetFile.readBytes().copyOfRange(0, 15), StandardCharsets.UTF_8)
        assertEquals("SQLite format 3", header)
    }

    @Test
    fun `asset user_version matches the declared database version`() {
        // SQLite stores user_version as a 4 byte big-endian int at offset 60.
        val userVersion = ByteBuffer.wrap(assetFile.readBytes(), 60, 4).int
        assertEquals(schemaVersion(), userVersion)
    }

    @Test
    fun `asset identity hash matches the generated schema`() {
        val expected = schemaValue("identityHash")
        val asset = String(assetFile.readBytes(), StandardCharsets.ISO_8859_1)
        assertTrue(
            "identity hash $expected is not in the asset; rerun tools/build_laws_db.py",
            asset.contains(expected),
        )
    }

    @Test
    fun `asset table has every column the schema declares`() {
        val createSql = schemaValue("createSql").replace("\${TABLE_NAME}", "laws")
        val asset = String(assetFile.readBytes(), StandardCharsets.ISO_8859_1)

        // A SQLite file is binary and the DDL is not guaranteed to start on a line boundary, so
        // slice it out of the raw bytes rather than scanning line by line.
        val marker = "CREATE TABLE `laws`"
        val start = asset.indexOf(marker)
        assertTrue("no `laws` table found in the asset", start >= 0)
        val end = asset.indexOf(';', start)
        val tableDefinition = asset.substring(start, end)

        // A column Room expects but the asset lacks is exactly the failure that crashed the app.
        val columns = Regex("`([^`]+)`").findAll(createSql).map { it.groupValues[1] }.toList()
        assertTrue("no columns parsed from the schema", columns.isNotEmpty())
        columns.forEach { column ->
            assertTrue(
                "column '$column' is missing from the pre-packaged table",
                Regex("`$column`").containsMatchIn(tableDefinition),
            )
        }
    }

    @Test
    fun `content batches are complete and non-empty`() {
        val contentDir = File("../content")
        assertTrue("content directory is missing", contentDir.isDirectory)

        val entries = contentDir.listFiles { f -> f.name.startsWith("laws-") && f.name.endsWith(".json") }
            ?.sumOf { file -> Regex("\"lawNumber\"\\s*:\\s*(\\d+)").findAll(file.readText()).count() }
            ?: 0
        assertEquals(48, entries)
    }
}

package com.example.a48lawsofpower.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room rejects the pre-packaged `assets/database/laws.db` unless it matches the schema below
 * exactly (same columns, same defaults, same identity hash). If you change [LawEntity], rebuild
 * `app/schemas/com.example.a48lawsofpower.data.LawDatabase/<version>.json` and recreate the asset
 * from the `createSql` and `identityHash` recorded there.
 *
 * Room copies the asset only when the database file does not exist yet, so a device that already
 * has a stale copy installed must uninstall the app before reinstalling.
 */
/**
 * The Room schema version, in one place.
 *
 * This exists so the version cannot drift between the `@Database` annotation, the exported schema
 * JSON, the pre-packaged asset, and `PrepackagedDatabaseTest`. That test previously hardcoded
 * "4.json" and silently compared the v5 asset against the v4 identity hash, which failed on every
 * build for a reason that had nothing to do with the asset.
 *
 * Bump this, add the matching `MIGRATION_n_n`, run the build so Room exports `<version>.json`,
 * then regenerate the asset with `tools/build_laws_db.py`.
 */
const val LAW_DATABASE_SCHEMA_VERSION = 7

@Database(
    entities = [LawEntity::class, AnnotationEntity::class, ReadingDayEntity::class],
    version = LAW_DATABASE_SCHEMA_VERSION,
    exportSchema = true,
)
abstract class LawDatabase : RoomDatabase() {
    abstract fun lawDao(): LawDao
    abstract fun annotationDao(): AnnotationDao
    abstract fun progressDao(): ProgressDao

    companion object {
        private const val NAME = "laws_database"
        private const val ASSET = "database/laws.db"

        @Volatile
        private var INSTANCE: LawDatabase? = null

        /**
         * Upgrades an install created from the version 3 schema.
         *
         * The table is rebuilt rather than patched with ALTER TABLE because version 4 also
         * *drops* the old `full_text` column, and Room validates the post-migration schema
         * exactly - a leftover column fails that check and crashes the app on update. SQLite only
         * grew `ALTER TABLE ... DROP COLUMN` in 3.35, which is well above what minSdk 24 devices
         * ship, so the portable route is create-copy-drop-rename.
         *
         * Mastery and bookmarks are carried across. The new editorial blocks cannot be: the
         * pre-packaged asset is only copied when the database file does not exist, so an install
         * that upgrades in place keeps its progress but starts with empty analysis. Uninstalling
         * and reinstalling pulls the fully populated asset instead.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `laws_upgrade` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `law_number` INTEGER NOT NULL,
                        `category` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `summary` TEXT NOT NULL,
                        `core` TEXT NOT NULL DEFAULT '',
                        `observance` TEXT NOT NULL DEFAULT '',
                        `transgression` TEXT NOT NULL DEFAULT '',
                        `reversal` TEXT NOT NULL DEFAULT '',
                        `bookmarked` INTEGER NOT NULL DEFAULT 0,
                        `read` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO `laws_upgrade`
                        (`id`, `law_number`, `category`, `title`, `summary`,
                         `core`, `observance`, `transgression`, `reversal`, `bookmarked`, `read`)
                    SELECT
                        `id`, `law_number`, `category`, `title`, `summary`,
                        '', '', '', '', 0, `read`
                    FROM `laws`
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE `laws`")
                db.execSQL("ALTER TABLE `laws_upgrade` RENAME TO `laws`")
            }
        }

        /**
         * Version 4 -> 5: annotations and reading history.
         *
         * Pure additions. `laws` is untouched, so existing mastery, bookmarks, and the editorial
         * blocks all carry across untouched - this is the first migration that does not have to
         * accept losing content.
         *
         * The new tables must be created here by hand *and* exist in the pre-packaged asset. Room
         * only copies the asset when no database file exists yet, so an install that upgrades in
         * place runs this migration while a fresh install gets the tables from the asset. Both
         * paths have to land on the same schema or Room's post-migration validation crashes.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `highlights` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `law_id` INTEGER NOT NULL,
                        `section` TEXT NOT NULL,
                        `start` INTEGER NOT NULL,
                        `end` INTEGER NOT NULL,
                        `color` TEXT NOT NULL,
                        `note` TEXT NOT NULL DEFAULT '',
                        `created_at` INTEGER NOT NULL,
                        FOREIGN KEY(`law_id`) REFERENCES `laws`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_highlights_law_id` ON `highlights` (`law_id`)")
                // `reading_days` deliberately gets no explicit index. `day` is an
                // INTEGER PRIMARY KEY, which SQLite stores as the rowid: already unique and
                // already indexed by definition. Declaring one as well is redundant, and Room
                // then demands the migration reproduce its uniqueness exactly.
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `reading_days` (
                        `day` INTEGER PRIMARY KEY NOT NULL,
                        `laws_read` INTEGER NOT NULL DEFAULT 0,
                        `seconds_read` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent(),
                )
            }
        }

        /**
         * Version 5 -> 6: highlights become annotations.
         *
         * The old `highlights` table stored only ranges, which cannot be explained in a review
         * list and cannot be re-found after a re-encode. `annotations` adds the covered text and a
         * type discriminator so highlights, notes, and bookmarks share one table.
         *
         * Rows are copied across in SQL before the old table is dropped. `anchor_text` is written
         * empty: computing it needs the law prose the range points into, and the section is stored
         * as a *name*, so `substr()` has nothing to read. The backfill happens on read instead -
         * see [AnnotationDao.backfillAnchorText] - because that also repairs any mark whose anchor
         * was lost, not just the ones this migration creates.
         *
         * `reading_days` is untouched.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `annotations` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `law_id` INTEGER NOT NULL,
                        `section` TEXT NOT NULL,
                        `start` INTEGER NOT NULL,
                        `end` INTEGER NOT NULL,
                        `anchor_text` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `color` TEXT NOT NULL,
                        `note` TEXT NOT NULL DEFAULT '',
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(`law_id`) REFERENCES `laws`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_annotations_law_id` ON `annotations` (`law_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_annotations_type` ON `annotations` (`type`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_annotations_created_at` ON `annotations` (`created_at`)")

                db.execSQL(
                    """
                    INSERT INTO `annotations`
                        (`law_id`, `section`, `start`, `end`, `anchor_text`,
                         `type`, `color`, `note`, `created_at`, `updated_at`)
                    SELECT
                        `law_id`, `section`, `start`, `end`, '',
                        'highlight', `color`, `note`, `created_at`, `created_at`
                    FROM `highlights`
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE `highlights`")
            }
        }

        /**
         * Adds the `key_takeaways` column.
         *
         * A plain `ALTER TABLE ... ADD COLUMN` rather than the create-copy-drop dance used by
         * `MIGRATION_3_4`: nothing is being dropped or renamed, and an added NOT NULL column with a
         * declared default is exactly the case SQLite handles in place. The default is the empty
         * string, so an install that upgrades in place starts with no takeaways rather than crashing.
         *
         * That empty state is the reason the reader has to tolerate a blank takeaways block: a
         * reader who upgrades will have the full text again only after a reinstall pulls the
         * pre-packaged asset, because Room copies that asset only when the database file is absent.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `laws` ADD COLUMN `key_takeaways` TEXT NOT NULL DEFAULT ''",
                )
            }
        }

        /** Migrations in order. Add each one here or upgrades from older installs crash on launch. */
        val ALL_MIGRATIONS =
            arrayOf(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)

        fun getDatabase(context: Context): LawDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LawDatabase::class.java,
                    NAME,
                )
                    .createFromAsset(ASSET)
                    .addMigrations(*ALL_MIGRATIONS)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

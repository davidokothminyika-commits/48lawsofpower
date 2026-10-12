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
@Database(entities = [LawEntity::class], version = 4, exportSchema = true)
abstract class LawDatabase : RoomDatabase() {
    abstract fun lawDao(): LawDao

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

        fun getDatabase(context: Context): LawDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LawDatabase::class.java,
                    NAME,
                )
                    .createFromAsset(ASSET)
                    .addMigrations(MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

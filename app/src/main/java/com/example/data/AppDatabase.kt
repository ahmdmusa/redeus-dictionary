package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [WordEntity::class, ReviewLogEntity::class, ReadingSessionEntity::class, MinedSentenceEntity::class],
    version = 12,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun readingSessionDao(): ReadingSessionDao
    abstract fun minedSentenceDao(): MinedSentenceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `reading_sessions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `date` INTEGER NOT NULL,
                        `score` INTEGER NOT NULL,
                        `total` INTEGER NOT NULL,
                        `levelBefore` INTEGER NOT NULL,
                        `levelAfter` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE `words` ADD COLUMN `lastUsedInReadingDate` INTEGER DEFAULT NULL
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_3_5 = object : Migration(3, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `reading_sessions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `date` INTEGER NOT NULL,
                        `score` INTEGER NOT NULL,
                        `total` INTEGER NOT NULL,
                        `levelBefore` INTEGER NOT NULL,
                        `levelAfter` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    ALTER TABLE `words` ADD COLUMN `lastUsedInReadingDate` INTEGER DEFAULT NULL
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE `words` ADD COLUMN `audioPath` TEXT DEFAULT NULL
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_4_6 = object : Migration(4, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE `words` ADD COLUMN `lastUsedInReadingDate` INTEGER DEFAULT NULL
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    ALTER TABLE `words` ADD COLUMN `audioPath` TEXT DEFAULT NULL
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_3_6 = object : Migration(3, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `reading_sessions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `date` INTEGER NOT NULL,
                        `score` INTEGER NOT NULL,
                        `total` INTEGER NOT NULL,
                        `levelBefore` INTEGER NOT NULL,
                        `levelAfter` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    ALTER TABLE `words` ADD COLUMN `lastUsedInReadingDate` INTEGER DEFAULT NULL
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    ALTER TABLE `words` ADD COLUMN `audioPath` TEXT DEFAULT NULL
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE `words` ADD COLUMN `lapses` INTEGER NOT NULL DEFAULT 0
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    ALTER TABLE `words` ADD COLUMN `isLeech` INTEGER NOT NULL DEFAULT 0
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `words` ADD COLUMN `fsrsStability` REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE `words` ADD COLUMN `fsrsDifficulty` REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE `words` ADD COLUMN `fsrsDue` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `words` ADD COLUMN `fsrsState` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `words` ADD COLUMN `fsrsStep` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `words` ADD COLUMN `fsrsLastReview` INTEGER DEFAULT NULL")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `words` ADD COLUMN `distinctReviewDays` INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `words` ADD COLUMN `lastSuccessfulReviewDay` INTEGER DEFAULT NULL")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `words` ADD COLUMN `tags` TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `mined_sentences` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sentence` TEXT NOT NULL,
                        `targetWord` TEXT NOT NULL,
                        `meaning` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `notes` TEXT NOT NULL DEFAULT '',
                        `isFavorite` INTEGER NOT NULL DEFAULT 0,
                        `practiceCount` INTEGER NOT NULL DEFAULT 0,
                        `lastPracticed` INTEGER DEFAULT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lexilearn_database"
                )
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_3_5, MIGRATION_5_6, MIGRATION_4_6, MIGRATION_3_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

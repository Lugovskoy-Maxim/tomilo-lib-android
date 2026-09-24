package ru.tomilo.lib.mobile.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [OfflineChapterEntity::class, OfflineTitleEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class OfflineDatabase : RoomDatabase() {
    abstract fun offlineDao(): OfflineDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `offline_titles` (
                        `titleId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `slug` TEXT NOT NULL,
                        `coverImage` TEXT,
                        `type` TEXT,
                        `status` TEXT,
                        `description` TEXT,
                        `totalChapters` INTEGER,
                        `averageRating` REAL,
                        `releaseYear` INTEGER,
                        `chaptersJson` TEXT NOT NULL,
                        `lastSyncedAt` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`titleId`)
                    )""".trimIndent(),
                )
            }
        }

        fun create(context: Context): OfflineDatabase =
            Room.databaseBuilder(context, OfflineDatabase::class.java, "tomilo_offline.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}

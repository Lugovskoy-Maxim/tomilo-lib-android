package ru.tomilo.lib.mobile.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [OfflineChapterEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class OfflineDatabase : RoomDatabase() {
    abstract fun offlineDao(): OfflineDao

    companion object {
        fun create(context: Context): OfflineDatabase =
            Room.databaseBuilder(context, OfflineDatabase::class.java, "tomilo_offline.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}

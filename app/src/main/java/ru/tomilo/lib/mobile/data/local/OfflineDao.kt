package ru.tomilo.lib.mobile.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineDao {
    @Query("SELECT * FROM offline_chapters ORDER BY downloadedAt DESC")
    fun observeAll(): Flow<List<OfflineChapterEntity>>

    @Query("SELECT * FROM offline_chapters WHERE titleId = :titleId ORDER BY downloadedAt DESC")
    fun observeByTitle(titleId: String): Flow<List<OfflineChapterEntity>>

    @Query("SELECT * FROM offline_chapters WHERE chapterId = :chapterId LIMIT 1")
    suspend fun get(chapterId: String): OfflineChapterEntity?

    @Query("SELECT chapterId FROM offline_chapters WHERE titleId = :titleId")
    suspend fun chapterIdsForTitle(titleId: String): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM offline_chapters WHERE chapterId = :chapterId)")
    suspend fun isDownloaded(chapterId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: OfflineChapterEntity)

    @Query("DELETE FROM offline_chapters WHERE chapterId = :chapterId")
    suspend fun delete(chapterId: String)

    @Query("DELETE FROM offline_chapters WHERE titleId = :titleId")
    suspend fun deleteTitle(titleId: String)
}

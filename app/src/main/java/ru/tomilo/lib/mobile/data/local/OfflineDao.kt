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

    @Query("SELECT * FROM offline_chapters")
    suspend fun allChapters(): List<OfflineChapterEntity>

    @Query("SELECT DISTINCT titleId FROM offline_chapters WHERE titleId != ''")
    suspend fun distinctTitleIds(): List<String>

    @Query("SELECT * FROM offline_chapters WHERE titleId = :titleId ORDER BY downloadedAt DESC")
    fun observeByTitle(titleId: String): Flow<List<OfflineChapterEntity>>

    @Query("SELECT * FROM offline_chapters WHERE titleId = :titleId")
    suspend fun chaptersForTitle(titleId: String): List<OfflineChapterEntity>

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

    @Query("DELETE FROM offline_chapters")
    suspend fun clearAll()

    // ── Title snapshots ─────────────────────────────────────────
    @Query("SELECT * FROM offline_titles ORDER BY lastSyncedAt DESC")
    fun observeTitles(): Flow<List<OfflineTitleEntity>>

    @Query("SELECT * FROM offline_titles WHERE titleId = :titleId LIMIT 1")
    suspend fun getTitle(titleId: String): OfflineTitleEntity?

    @Query("SELECT * FROM offline_titles")
    suspend fun allTitles(): List<OfflineTitleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTitle(entity: OfflineTitleEntity)

    @Query("DELETE FROM offline_titles WHERE titleId = :titleId")
    suspend fun deleteTitleMeta(titleId: String)

    @Query("DELETE FROM offline_titles")
    suspend fun clearTitles()
}

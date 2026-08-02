package ru.tomilo.lib.mobile.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_chapters")
data class OfflineChapterEntity(
    @PrimaryKey val chapterId: String,
    val titleId: String,
    val titleName: String,
    val titleSlug: String,
    val titleCover: String?,
    val chapterNumber: String,
    val chapterName: String?,
    val pageCount: Int,
    val localDir: String,
    val downloadedAt: Long,
    val bytesTotal: Long = 0L,
)

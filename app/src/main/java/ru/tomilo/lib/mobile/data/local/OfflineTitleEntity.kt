package ru.tomilo.lib.mobile.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Снимок тайтла для офлайн: метаданные + список всех глав (id/номер),
 * чтобы видеть что скачано/прочитано и подтягивать новые главы при синке.
 */
@Entity(tableName = "offline_titles")
data class OfflineTitleEntity(
    @PrimaryKey val titleId: String,
    val name: String,
    val slug: String,
    val coverImage: String?,
    val type: String?,
    val status: String?,
    val description: String?,
    val totalChapters: Int?,
    val averageRating: Double?,
    val releaseYear: Int?,
    /** JSON: list of OfflineChapterMeta */
    val chaptersJson: String,
    val lastSyncedAt: Long,
    val createdAt: Long = System.currentTimeMillis(),
)

/** Лёгкая мета главы в chaptersJson */
@kotlinx.serialization.Serializable
data class OfflineChapterMeta(
    val chapterId: String,
    val chapterNumber: String,
    val name: String? = null,
    val pagesCount: Int? = null,
    val releaseDate: String? = null,
)

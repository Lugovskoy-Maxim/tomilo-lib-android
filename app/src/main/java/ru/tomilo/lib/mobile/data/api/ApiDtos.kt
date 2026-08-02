package ru.tomilo.lib.mobile.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ApiResponse<T>(
    val success: Boolean = false,
    val data: T? = null,
    val message: String? = null,
    val errors: List<String>? = null,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class AuthPayload(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    val user: UserDto,
)

@Serializable
data class UserDto(
    @SerialName("_id") val underscoreId: String? = null,
    val id: String? = null,
    val email: String? = null,
    val username: String? = null,
    val avatar: String? = null,
    val role: String? = null,
    val level: Int? = null,
    val experience: Int? = null,
    val subscriptionExpiresAt: String? = null,
    val commentsCount: Int? = null,
    val likesReceivedCount: Int? = null,
    val chaptersRead: Int? = null,
    val readingTimeMinutes: Int? = null,
    val currentStreak: Int? = null,
    val titlesReadCount: Int? = null,
    val completedTitlesCount: Int? = null,
    val balance: Int? = null,
) {
    fun stableId(): String = id ?: underscoreId.orEmpty()
}

@Serializable
data class CatalogTitleDto(
    val id: String? = null,
    @SerialName("_id") val underscoreId: String? = null,
    val title: String? = null,
    val name: String? = null,
    val slug: String? = null,
    val cover: String? = null,
    val coverImage: String? = null,
    val rating: Double? = null,
    val averageRating: Double? = null,
    val type: String? = null,
    val releaseYear: Int? = null,
    val description: String? = null,
    val totalChapters: Int? = null,
    val chapter: String? = null,
    val chapterNumber: JsonElement? = null,
    val status: String? = null,
    val genres: List<String>? = null,
    val isAdult: Boolean? = null,
) {
    fun stableId(): String = id ?: underscoreId.orEmpty()
    fun displayTitle(): String = title ?: name.orEmpty()
    fun coverPath(): String? = cover ?: coverImage
    fun displayRating(): Double? = rating ?: averageRating
}

@Serializable
data class TitleDetailDto(
    @SerialName("_id") val underscoreId: String? = null,
    val id: String? = null,
    val name: String? = null,
    val slug: String? = null,
    val description: String? = null,
    val coverImage: String? = null,
    val status: String? = null,
    val type: String? = null,
    val author: String? = null,
    val artist: String? = null,
    val releaseYear: Int? = null,
    val totalChapters: Int? = null,
    val averageRating: Double? = null,
    val genres: List<String>? = null,
    val tags: List<String>? = null,
    val altNames: List<String>? = null,
    val ageLimit: Int? = null,
) {
    fun stableId(): String = id ?: underscoreId.orEmpty()
}

@Serializable
data class ChaptersPageDto(
    val chapters: List<ChapterDto> = emptyList(),
    val pagination: PaginationDto? = null,
)

@Serializable
data class PaginationDto(
    val page: Int = 1,
    val limit: Int = 20,
    val total: Int = 0,
    val pages: Int = 0,
    val hasMore: Boolean = false,
)

@Serializable
data class ChapterDto(
    @SerialName("_id") val underscoreId: String? = null,
    val id: String? = null,
    val chapterNumber: JsonElement? = null,
    val name: String? = null,
    val pages: List<String>? = null,
    val pagesCount: Int? = null,
    val views: JsonElement? = null,
    val isPublished: Boolean? = null,
    val isPaid: Boolean? = null,
    val unlockPrice: Int? = null,
    val freeAt: String? = null,
    val releaseDate: String? = null,
) {
    fun stableId(): String = id ?: underscoreId.orEmpty()

    fun numberLabel(): String {
        val n = chapterNumberAsDouble()
        return if (n != null) {
            if (n % 1.0 == 0.0) n.toInt().toString() else n.toString()
        } else {
            name.orEmpty().ifBlank { "?" }
        }
    }

    fun chapterNumberAsDouble(): Double? {
        val el = chapterNumber ?: return null
        return try {
            el.toString().trim('"').toDoubleOrNull()
        } catch (_: Exception) {
            null
        }
    }
}

@Serializable
data class SearchHitDto(
    val kind: String? = null,
    val id: String? = null,
    val title: String? = null,
    val name: String? = null,
    val slug: String? = null,
    val cover: String? = null,
    val image: String? = null,
    val type: String? = null,
    val rating: Double? = null,
    val totalChapters: Int? = null,
    val releaseYear: Int? = null,
) {
    fun displayTitle(): String = title ?: name.orEmpty()
}

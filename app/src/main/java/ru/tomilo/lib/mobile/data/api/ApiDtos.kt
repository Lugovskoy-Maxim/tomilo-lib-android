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
data class YandexTokenRequest(
    @SerialName("access_token") val accessToken: String,
)

@Serializable
data class VkIdLoginRequest(
    val code: String,
    @SerialName("code_verifier") val codeVerifier: String,
    @SerialName("device_id") val deviceId: String,
    val state: String,
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
    fun isStaff(): Boolean {
        val r = role?.lowercase().orEmpty()
        return r == "admin" || r == "moderator"
    }
    fun isAdmin(): Boolean = role?.equals("admin", ignoreCase = true) == true
}

@Serializable
data class RateTitleRequest(val rating: Int)

@Serializable
data class HistoryEntryDto(
    @SerialName("_id") val underscoreId: String? = null,
    val id: String? = null,
    val titleId: JsonElement? = null,
    val chapterId: JsonElement? = null,
    val titleName: String? = null,
    val titleSlug: String? = null,
    val coverImage: String? = null,
    val cover: String? = null,
    val chapterNumber: JsonElement? = null,
    val chapterName: String? = null,
    val readAt: String? = null,
    val updatedAt: String? = null,
) {
    fun titleKey(): String = extractId(titleId)
    fun chapterKey(): String = extractId(chapterId)
    fun displayTitle(): String = titleName?.takeIf { it.isNotBlank() } ?: "Тайтл"
    fun coverPath(): String? = coverImage ?: cover
    fun chapterLabel(): String {
        val n = chapterNumber?.toString()?.trim('"')
        return when {
            !chapterName.isNullOrBlank() -> chapterName
            !n.isNullOrBlank() -> "Глава $n"
            else -> "Глава"
        }
    }

    private fun extractId(el: JsonElement?): String {
        if (el == null) return ""
        return when (el) {
            is kotlinx.serialization.json.JsonPrimitive -> el.content
            is kotlinx.serialization.json.JsonObject ->
                el["_id"]?.toString()?.trim('"')
                    ?: el["id"]?.toString()?.trim('"')
                    ?: ""
            else -> ""
        }
    }
}

@Serializable
data class AdminDashboardDto(
    val totalUsers: Int? = null,
    val totalTitles: Int? = null,
    val totalChapters: Int? = null,
    val totalComments: Int? = null,
    val totalViews: Long? = null,
    val activeUsers: Int? = null,
    val newUsersToday: Int? = null,
    val newUsersWeek: Int? = null,
    val premiumUsers: Int? = null,
    val users: Int? = null,
    val titles: Int? = null,
    val chapters: Int? = null,
    val comments: Int? = null,
)

@Serializable
data class AdminUsersPageDto(
    val users: List<AdminUserDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 30,
)

@Serializable
data class AdminUserDto(
    @SerialName("_id") val underscoreId: String? = null,
    val id: String? = null,
    val username: String? = null,
    val email: String? = null,
    val role: String? = null,
    val level: Int? = null,
    val avatar: String? = null,
    val isBanned: Boolean? = null,
    val banned: Boolean? = null,
    val createdAt: String? = null,
    val subscriptionExpiresAt: String? = null,
) {
    fun stableId(): String = id ?: underscoreId.orEmpty()
    fun banned(): Boolean = isBanned == true || banned == true
}

@Serializable
data class AdminBanRequest(
    val reason: String? = "Нарушение правил",
    val permanent: Boolean = true,
)

@Serializable
data class AdminRoleRequest(val role: String)

@Serializable
data class AdminCommentsPageDto(
    val comments: List<AdminCommentDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
)

@Serializable
data class AdminCommentDto(
    @SerialName("_id") val underscoreId: String? = null,
    val id: String? = null,
    val content: String? = null,
    val isHidden: Boolean? = null,
    val hiddenBySystem: Boolean? = null,
    val createdAt: String? = null,
    val user: AdminUserDto? = null,
    val author: AdminUserDto? = null,
) {
    fun stableId(): String = id ?: underscoreId.orEmpty()
    fun authorName(): String = (user ?: author)?.username ?: "user"
    fun hidden(): Boolean = isHidden == true || hiddenBySystem == true
}

@Serializable
data class AdminCommentVisibilityRequest(val isHidden: Boolean)

@Serializable
data class AdminTitlesPageDto(
    val titles: List<AdminTitleDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
)

@Serializable
data class AdminTitleDto(
    @SerialName("_id") val underscoreId: String? = null,
    val id: String? = null,
    val name: String? = null,
    val title: String? = null,
    val slug: String? = null,
    val coverImage: String? = null,
    val cover: String? = null,
    val isPublished: Boolean? = null,
    val totalChapters: Int? = null,
    val type: String? = null,
) {
    fun stableId(): String = id ?: underscoreId.orEmpty()
    fun displayName(): String = name ?: title ?: slug ?: "Тайтл"
    fun coverPath(): String? = coverImage ?: cover
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

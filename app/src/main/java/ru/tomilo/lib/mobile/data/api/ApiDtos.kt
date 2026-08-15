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
data class ShopDecorationDto(
    @SerialName("_id") val underscoreId: String? = null,
    val id: String? = null,
    val name: String = "Украшение",
    val description: String? = null,
    val imageUrl: String? = null,
    val type: String? = null,
    val price: Int = 0,
    val rarity: String = "common",
    val isAvailable: Boolean? = true,
    val isEquipped: Boolean? = false,
    val stock: Int? = null,
    val quantity: Int? = null,
    val isSoldOut: Boolean? = null,
    val originalPrice: Int? = null,
    val ownersCount: Int? = null,
    val purchaseCount: Int? = null,
    val authorUsername: String? = null,
) {
    fun stableId(): String = id ?: underscoreId.orEmpty()
    fun remaining(): Int? = stock ?: quantity
    fun soldOut(): Boolean = isSoldOut == true || remaining()?.let { it <= 0 } == true
}

@Serializable
data class RateTitleRequest(val rating: Int)

@Serializable
data class DailyQuestDto(
    val id: String,
    val type: String? = null,
    val name: String,
    val description: String? = null,
    val target: Int = 0,
    val progress: Int = 0,
    val rewardExp: Int = 0,
    val rewardCoins: Int = 0,
    val completed: Boolean = false,
    val claimedAt: String? = null,
)

@Serializable
data class DailyQuestsDto(
    val date: String? = null,
    val serverNow: String? = null,
    val resetAt: String? = null,
    val quests: List<DailyQuestDto> = emptyList(),
)

@Serializable
data class QuestClaimRequest(val questId: String)

@Serializable
data class QuestClaimResultDto(
    val success: Boolean = false,
    val claimedCount: Int = 0,
    val expGained: Int = 0,
    val coinsGained: Int = 0,
    val balance: Int? = null,
    val message: String? = null,
)

@Serializable
data class DailyBonusResultDto(
    val success: Boolean = false,
    val message: String? = null,
    val currentStreak: Int = 0,
    val experienceGained: Int = 0,
    val coinsGained: Int = 0,
)

@Serializable
data class WheelSegmentDto(
    val rewardType: String = "",
    val label: String = "",
    val weight: Double = 0.0,
    val param: JsonElement? = null,
    val icon: String? = null,
    val rarity: String? = null,
    val rewardMeta: WheelRewardMetaDto? = null,
)

@Serializable
data class WheelRewardMetaDto(
    val kind: String = "",
    val valueText: String? = null,
)

@Serializable
data class WheelDto(
    val segments: List<WheelSegmentDto> = emptyList(),
    val spinCostCoins: Int = 0,
    val instantSpinCostCoins: Int? = null,
    val canSpin: Boolean = false,
    val canInstantSpin: Boolean = false,
    val lastWheelSpinAt: String? = null,
    val nextSpinAt: String? = null,
    val balance: Int = 0,
    val spinCooldownSeconds: Int? = null,
)

@Serializable
data class WheelSpinRequest(val skipCooldown: Boolean? = null)

@Serializable
data class WheelItemGainDto(
    val itemId: String = "",
    val count: Int = 0,
    val name: String? = null,
    val icon: String? = null,
)

@Serializable
data class WheelRewardSummaryDto(
    val type: String = "",
    val label: String = "",
    val amount: Int? = null,
    val icon: String? = null,
)

@Serializable
data class WheelSpinResultDto(
    val rewardType: String = "",
    val label: String = "",
    val expGained: Int? = null,
    val coinsGained: Int? = null,
    val itemsGained: List<WheelItemGainDto> = emptyList(),
    val twistOfFate: Boolean = false,
    val compensationCoins: Int? = null,
    val balance: Int? = null,
    val selectedSegmentIndex: Int? = null,
    val nextSpinAt: String? = null,
    val rewardSummary: List<WheelRewardSummaryDto> = emptyList(),
)

@Serializable
data class WheelRecentWinDto(
    val username: String = "",
    val avatar: String? = null,
    val rewardType: String = "",
    val label: String = "",
    val wonAt: String = "",
    val displayTier: String = "feed",
    val rarity: String? = null,
    val icon: String? = null,
)

@Serializable
data class WheelRecentWinsDto(
    val highlight: WheelRecentWinDto? = null,
    val recent: List<WheelRecentWinDto> = emptyList(),
)

/** GET users/profile/progress/:titleId */
@Serializable
data class ReadingProgressDto(
    val titleId: String? = null,
    val lastChapterId: String? = null,
    val lastChapterNumber: Double? = null,
    val chaptersRead: Int = 0,
    val totalChapters: Int = 0,
    val progressPercent: Int = 0,
    val readAt: String? = null,
) {
    fun progressLine(): String {
        val read = chaptersRead.coerceAtLeast(0)
        val total = totalChapters.coerceAtLeast(0)
        return when {
            total > 0 -> "Прочитано $read / $total гл." +
                if (progressPercent > 0) " · $progressPercent%" else ""
            read > 0 -> "Прочитано $read гл."
            else -> "Не начато"
        }
    }
}

@Serializable
data class HistoryLastChapterDto(
    val chapterId: JsonElement? = null,
    val chapterNumber: JsonElement? = null,
    val chapterTitle: String? = null,
    val readAt: String? = null,
) {
    fun chapterKey(): String = jsonElementId(chapterId)
    fun numberLabel(): String {
        val n = chapterNumber?.toString()?.trim('"')
        return n?.takeIf { it.isNotBlank() } ?: "?"
    }
}

/**
 * Элемент лёгкой истории: titleId (string | populated), lastChapter, chaptersCount.
 */
@Serializable
data class HistoryEntryDto(
    val titleId: JsonElement? = null,
    val readAt: String? = null,
    val lastChapter: HistoryLastChapterDto? = null,
    val chaptersCount: Int? = null,
    /** full format may include chapters[] */
    val chapters: List<HistoryLastChapterDto>? = null,
    // flat aliases if ever present
    val titleName: String? = null,
    val titleSlug: String? = null,
    val coverImage: String? = null,
    val cover: String? = null,
    val chapterId: JsonElement? = null,
    val chapterNumber: JsonElement? = null,
    val chapterName: String? = null,
) {
    fun titleKey(): String = jsonElementId(titleId)

    fun titleMeta(): Pair<String, String?> {
        val el = titleId
        if (el is kotlinx.serialization.json.JsonObject) {
            val name = el["name"]?.toString()?.trim('"')
                ?: el["title"]?.toString()?.trim('"')
            val slug = el["slug"]?.toString()?.trim('"')
            return (name?.takeIf { it.isNotBlank() } ?: titleName ?: "Тайтл") to slug
        }
        return (titleName ?: "Тайтл") to titleSlug
    }

    fun displayTitle(): String = titleMeta().first

    fun slug(): String? = titleMeta().second

    fun coverPath(): String? {
        val el = titleId
        if (el is kotlinx.serialization.json.JsonObject) {
            val c = el["coverImage"]?.toString()?.trim('"')
                ?: el["cover"]?.toString()?.trim('"')
            if (!c.isNullOrBlank() && c != "null") return c
        }
        return coverImage ?: cover
    }

    fun type(): String? {
        val el = titleId
        if (el is kotlinx.serialization.json.JsonObject) {
            return el["type"]?.toString()?.trim('"')?.takeIf { it.isNotBlank() && it != "null" }
        }
        return null
    }

    fun chapterKey(): String {
        lastChapter?.chapterKey()?.takeIf { it.isNotBlank() }?.let { return it }
        jsonElementId(chapterId).takeIf { it.isNotBlank() }?.let { return it }
        return chapters?.firstOrNull()?.chapterKey().orEmpty()
    }

    fun chapterLabel(): String {
        lastChapter?.let {
            val title = it.chapterTitle
            return if (!title.isNullOrBlank()) "Глава ${it.numberLabel()} · $title"
            else "Глава ${it.numberLabel()}"
        }
        val n = chapterNumber?.toString()?.trim('"')
        return when {
            !chapterName.isNullOrBlank() -> chapterName
            !n.isNullOrBlank() -> "Глава $n"
            else -> chaptersCount?.let { "Прочитано: $it гл." } ?: "История"
        }
    }

    fun readAtLabel(): String? =
        (lastChapter?.readAt ?: readAt)?.take(16)?.replace('T', ' ')
}

@Serializable
data class ReadIdsDto(
    val chapterIds: List<String> = emptyList(),
    val chapterNumbers: List<Double> = emptyList(),
)

private fun jsonElementId(el: JsonElement?): String {
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
    val views: Long? = null,
    val weekViews: Long? = null,
) {
    fun stableId(): String = id ?: underscoreId.orEmpty()
    fun displayTitle(): String = title ?: name.orEmpty()
    fun coverPath(): String? = cover ?: coverImage
    fun displayRating(): Double? = rating ?: averageRating
}

@Serializable
data class CatalogPageDto(
    val titles: List<CatalogTitleDto> = emptyList(),
    val pagination: PaginationDto? = null,
)

@Serializable
data class CatalogFilterOptionsDto(
    val genres: List<String> = emptyList(),
    val types: List<String> = emptyList(),
    val status: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val releaseYears: List<Int> = emptyList(),
    val ageLimits: List<Int> = emptyList(),
    val chaptersMin: Int? = null,
    val chaptersMax: Int? = null,
    val sortByOptions: List<String> = emptyList(),
)

data class CatalogQuery(
    val page: Int = 1,
    val limit: Int = 24,
    val search: String? = null,
    val genres: String? = null,
    val types: String? = null,
    val status: String? = null,
    val sortBy: String = "updatedAt",
    val sortOrder: String = "desc",
    val releaseYears: String? = null,
    val ageLimits: String? = null,
    val includeAdult: Boolean = false,
)

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
    val totalRatings: Int? = null,
    val views: Long? = null,
    val dayViews: Long? = null,
    val weekViews: Long? = null,
    val monthViews: Long? = null,
    val genres: List<String>? = null,
    val tags: List<String>? = null,
    val altNames: List<String>? = null,
    val ageLimit: Int? = null,
    val isAdult: Boolean? = null,
    val isPublished: Boolean? = null,
    val chaptersRemovedByCopyrightHolder: Boolean? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
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
    /** Разблокировка за монеты активности (с сервера). */
    val isUnlockedByActivityCoins: Boolean? = null,
    val status: String? = null,
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

    fun isWithdrawn(): Boolean {
        val s = status?.lowercase().orEmpty()
        return s == "hidden" || s == "deleted"
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

@Serializable
data class CreateRobokassaPaymentRequest(val planId: String)

@Serializable
data class RobokassaPaymentFormDto(
    val paymentId: String = "",
    val invId: String = "",
    val planId: String = "",
    val chargedAmount: Double = 0.0,
    val isTestPayment: Boolean = false,
    val gatewayTestMode: Boolean = false,
    val paymentUrl: String = "",
    val fields: Map<String, String> = emptyMap(),
)

@Serializable
data class RobokassaPaymentStatusDto(
    val invId: String = "",
    val planId: String = "",
    val amount: Double = 0.0,
    val durationDays: Int = 0,
    val status: String = "pending",
    val paidAt: String? = null,
    val subscriptionExpiresAt: String? = null,
)

@Serializable
data class PremiumPaymentReceiptDto(
    val sellerName: String? = null,
    val sellerInn: String? = null,
    val number: String? = null,
    val itemName: String? = null,
    val email: String? = null,
)

@Serializable
data class PremiumPaymentHistoryItemDto(
    val id: String = "",
    val type: String = "robokassa",
    val status: String = "pending",
    val amount: Double = 0.0,
    val currency: String = "RUB",
    val description: String = "",
    val durationDays: Int = 0,
    val invId: String? = null,
    val createdAt: String? = null,
    val paidAt: String? = null,
    val subscriptionExpiresAt: String? = null,
    val isTestPayment: Boolean = false,
    val gatewayTestMode: Boolean = false,
    val receipt: PremiumPaymentReceiptDto? = null,
)

@Serializable
data class CoinPremiumPurchaseRequest(val purchaseId: String)

@Serializable
data class CoinPremiumPurchaseResultDto(
    val balance: Int = 0,
    val subscriptionExpiresAt: String? = null,
    val durationDays: Int = 30,
    val priceCoins: Int = 30_000,
)

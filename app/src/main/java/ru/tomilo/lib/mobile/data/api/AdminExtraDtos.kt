package ru.tomilo.lib.mobile.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class AdminReportsPageDto(
    val reports: List<AdminReportDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val totalPages: Int = 1,
    val limit: Int = 30,
)

@Serializable
data class AdminReportDto(
    @SerialName("_id") val underscoreId: String? = null,
    val id: String? = null,
    val reportType: String? = null,
    val content: String? = null,
    val resolutionMessage: String? = null,
    val response: String? = null,
    val entityId: String? = null,
    val entityType: String? = null,
    val url: String? = null,
    val titleId: String? = null,
    val isResolved: Boolean? = null,
    val createdAt: String? = null,
    val userId: JsonElement? = null,
) {
    fun stableId(): String = id ?: underscoreId.orEmpty()
    fun resolved(): Boolean = isResolved == true
    fun typeLabel(): String = when (reportType) {
        "error" -> "Ошибка"
        "typo" -> "Опечатка"
        "complaint" -> "Жалоба"
        "missing_pages" -> "Нет страниц"
        "broken_images" -> "Битые картинки"
        "wrong_order" -> "Неверный порядок"
        "duplicate" -> "Дубликат"
        "comment_report" -> "Комментарий"
        else -> reportType ?: "Другое"
    }
    fun authorName(): String {
        val el = userId
        if (el is JsonObject) {
            return el["username"]?.toString()?.trim('"')?.takeIf { it.isNotBlank() && it != "null" }
                ?: "user"
        }
        return "user"
    }
}

@Serializable
data class AdminReportStatusRequest(
    val isResolved: Boolean,
    val resolutionMessage: String? = null,
)

@Serializable
data class AdminUserUpdateRequest(
    val subscriptionExpiresAt: String? = null,
    val username: String? = null,
    val email: String? = null,
    val level: Int? = null,
)

@Serializable
data class AdminBalanceRequest(
    val amount: Int,
    val description: String = "Админка Android",
)

@Serializable
data class AdminTitleUpdateRequest(
    val name: String? = null,
    val description: String? = null,
    val status: String? = null,
    val type: String? = null,
    val isPublished: Boolean? = null,
    val isAdult: Boolean? = null,
    val ageLimit: Int? = null,
    val author: String? = null,
)

@Serializable
data class AdminSiteSettingsDto(
    @SerialName("_id") val underscoreId: String? = null,
    val maintenanceMode: Boolean? = null,
    val maintenanceMessage: String? = null,
    val registrationEnabled: Boolean? = null,
    val commentsEnabled: Boolean? = null,
    val ratingsEnabled: Boolean? = null,
    val adultContentEnabled: Boolean? = null,
    val adultContentRequiresAuth: Boolean? = null,
    val guestHomeAdultContentEnabled: Boolean? = null,
    val siteName: String? = null,
    val siteDescription: String? = null,
    val contactEmail: String? = null,
    val siteVersionLabel: String? = null,
    val defaultUserRole: String? = null,
)

@Serializable
data class AdminSiteSettingsUpdate(
    val maintenanceMode: Boolean? = null,
    val maintenanceMessage: String? = null,
    val registrationEnabled: Boolean? = null,
    val commentsEnabled: Boolean? = null,
    val ratingsEnabled: Boolean? = null,
    val adultContentEnabled: Boolean? = null,
    val adultContentRequiresAuth: Boolean? = null,
    val guestHomeAdultContentEnabled: Boolean? = null,
    val siteName: String? = null,
    val siteDescription: String? = null,
    val contactEmail: String? = null,
    val siteVersionLabel: String? = null,
)

@Serializable
data class AutoParseJobDto(
    @SerialName("_id") val underscoreId: String? = null,
    val id: String? = null,
    val titleId: JsonElement? = null,
    val url: String? = null,
    val sources: List<String>? = null,
    val frequency: String? = null,
    val scheduleHour: Int? = null,
    val scheduleMinute: Int? = null,
    val enabled: Boolean? = null,
) {
    fun stableId(): String = id ?: underscoreId.orEmpty()
    fun titleName(): String {
        val el = titleId
        if (el is JsonObject) {
            return el["name"]?.toString()?.trim('"')
                ?: el["title"]?.toString()?.trim('"')
                ?: "Тайтл"
        }
        return "Тайтл"
    }
    fun titleKey(): String {
        val el = titleId
        if (el is JsonObject) {
            return el["_id"]?.toString()?.trim('"')
                ?: el["id"]?.toString()?.trim('"')
                ?: ""
        }
        return el?.toString()?.trim('"').orEmpty()
    }
}

@Serializable
data class AutoParseCreateRequest(
    val titleId: String,
    val sources: List<String>? = null,
    val frequency: String? = "daily",
    val enabled: Boolean? = true,
)

@Serializable
data class AutoParseUpdateRequest(
    val enabled: Boolean? = null,
    val sources: List<String>? = null,
    val frequency: String? = null,
)

@Serializable
data class SearchSourcesRequest(
    val titleId: String,
    val sites: List<String>? = null,
)

@Serializable
data class ParseTitleRequest(
    val url: String,
    val customTitle: String? = null,
)

@Serializable
data class SourceCandidateDto(
    val site: String? = null,
    val title: String? = null,
    val url: String? = null,
    val score: Double? = null,
    val alreadyInSources: Boolean? = null,
)

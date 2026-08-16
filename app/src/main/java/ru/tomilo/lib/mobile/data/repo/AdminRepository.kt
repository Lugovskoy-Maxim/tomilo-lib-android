package ru.tomilo.lib.mobile.data.repo

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import ru.tomilo.lib.mobile.data.api.AdminBalanceRequest
import ru.tomilo.lib.mobile.data.api.AdminBanRequest
import ru.tomilo.lib.mobile.data.api.AdminCommentDto
import ru.tomilo.lib.mobile.data.api.AdminCommentVisibilityRequest
import ru.tomilo.lib.mobile.data.api.AdminDashboardDto
import ru.tomilo.lib.mobile.data.api.AdminReportDto
import ru.tomilo.lib.mobile.data.api.AdminReportStatusRequest
import ru.tomilo.lib.mobile.data.api.AdminRoleRequest
import ru.tomilo.lib.mobile.data.api.AdminSiteSettingsDto
import ru.tomilo.lib.mobile.data.api.AdminSiteSettingsUpdate
import ru.tomilo.lib.mobile.data.api.AdminTitleDto
import ru.tomilo.lib.mobile.data.api.AdminTitleUpdateRequest
import ru.tomilo.lib.mobile.data.api.AdminUserDto
import ru.tomilo.lib.mobile.data.api.AutoParseCreateRequest
import ru.tomilo.lib.mobile.data.api.AutoParseJobDto
import ru.tomilo.lib.mobile.data.api.AutoParseUpdateRequest
import ru.tomilo.lib.mobile.data.api.NetworkModule
import ru.tomilo.lib.mobile.data.api.ParseTitleRequest
import ru.tomilo.lib.mobile.data.api.SearchSourcesRequest
import ru.tomilo.lib.mobile.data.api.SourceCandidateDto
import ru.tomilo.lib.mobile.data.api.TomiloApi
import java.time.Instant
import java.time.temporal.ChronoUnit

class AdminRepository(private val api: TomiloApi) {
    private val json = NetworkModule.json

    suspend fun dashboard(): Result<AdminDashboardDto> = runCatching {
        val res = api.adminDashboard()
        if (!res.success) error(res.message ?: "Нет доступа к дашборду")
        res.data ?: AdminDashboardDto()
    }

    suspend fun users(page: Int = 1, search: String? = null): Result<List<AdminUserDto>> =
        runCatching {
            val res = api.adminUsers(page = page, search = search?.ifBlank { null })
            if (!res.success) error(res.message ?: "Ошибка пользователей")
            res.data?.users.orEmpty()
        }

    suspend fun banUser(id: String, reason: String = "Нарушение правил"): Result<Unit> =
        runCatching {
            val res = api.adminBanUser(id, AdminBanRequest(reason = reason))
            if (!res.success) error(res.message ?: "Не удалось забанить")
        }

    suspend fun unbanUser(id: String): Result<Unit> = runCatching {
        val res = api.adminUnbanUser(id)
        if (!res.success) error(res.message ?: "Не удалось разбанить")
    }

    suspend fun setRole(id: String, role: String): Result<Unit> = runCatching {
        val res = api.adminSetRole(id, AdminRoleRequest(role))
        if (!res.success) error(res.message ?: "Не удалось сменить роль")
    }

    suspend fun comments(page: Int = 1): Result<List<AdminCommentDto>> = runCatching {
        val res = api.adminComments(page = page)
        if (!res.success) error(res.message ?: "Ошибка комментариев")
        res.data?.comments.orEmpty()
    }

    suspend fun hideComment(id: String, hidden: Boolean): Result<Unit> = runCatching {
        val res = api.adminCommentVisibility(id, AdminCommentVisibilityRequest(hidden))
        if (!res.success) error(res.message ?: "Ошибка видимости")
    }

    suspend fun deleteComment(id: String): Result<Unit> = runCatching {
        val res = api.adminDeleteComment(id)
        if (!res.success) error(res.message ?: "Не удалось удалить")
    }

    suspend fun titles(page: Int = 1, search: String? = null): Result<List<AdminTitleDto>> =
        runCatching {
            val res = api.adminTitles(page = page, search = search?.ifBlank { null })
            if (!res.success) error(res.message ?: "Ошибка тайтлов")
            res.data?.titles.orEmpty()
        }

    suspend fun clearCache(): Result<String> = runCatching {
        val res = api.adminClearCache()
        if (!res.success) error(res.message ?: "Ошибка очистки кеша")
        res.message ?: "Кеш очищен"
    }

    suspend fun activity(): Result<List<String>> = runCatching {
        val res = api.adminActivity(limit = 40)
        if (!res.success) error(res.message ?: "Ошибка активности")
        parseActivity(res.data)
    }

    suspend fun reports(unresolvedOnly: Boolean = true): Result<List<AdminReportDto>> = runCatching {
        val res = api.adminReports(
            isResolved = if (unresolvedOnly) "false" else null,
        )
        if (!res.success) error(res.message ?: "Не удалось загрузить жалобы")
        res.data?.reports.orEmpty()
    }

    suspend fun resolveReport(id: String, message: String?): Result<Unit> = runCatching {
        val res = api.adminUpdateReportStatus(
            id,
            AdminReportStatusRequest(isResolved = true, resolutionMessage = message?.ifBlank { null }),
        )
        if (!res.success) error(res.message ?: "Не удалось закрыть жалобу")
    }

    suspend fun deleteReport(id: String): Result<Unit> = runCatching {
        val res = api.adminDeleteReport(id)
        if (!res.success) error(res.message ?: "Не удалось удалить жалобу")
    }

    suspend fun grantPremiumDays(userId: String, days: Int, currentIso: String?): Result<Unit> =
        runCatching {
            val now = Instant.now()
            val base = currentIso?.let { runCatching { Instant.parse(it) }.getOrNull() }
                ?.takeIf { it.isAfter(now) }
                ?: now
            val next = base.plus(days.toLong(), ChronoUnit.DAYS).toString()
            val res = api.adminUpdateUser(
                userId,
                buildJsonObject { put("subscriptionExpiresAt", JsonPrimitive(next)) },
            )
            if (!res.success) error(res.message ?: "Не удалось выдать Premium")
        }

    suspend fun setPremiumUntil(userId: String, isoOrNull: String?): Result<Unit> = runCatching {
        val res = api.adminUpdateUser(
            userId,
            buildJsonObject {
                if (isoOrNull.isNullOrBlank()) put("subscriptionExpiresAt", JsonNull)
                else put("subscriptionExpiresAt", JsonPrimitive(isoOrNull))
            },
        )
        if (!res.success) error(res.message ?: "Не удалось обновить Premium")
    }

    suspend fun changeBalance(userId: String, amount: Int, note: String): Result<Unit> = runCatching {
        val res = api.adminUpdateUserBalance(userId, AdminBalanceRequest(amount = amount, description = note))
        if (!res.success) error(res.message ?: "Не удалось изменить баланс")
    }

    suspend fun siteSettings(): Result<AdminSiteSettingsDto> = runCatching {
        val res = api.adminSettings()
        if (!res.success) error(res.message ?: "Нет настроек сайта")
        res.data ?: error("Пустые настройки")
    }

    suspend fun updateSiteSettings(body: AdminSiteSettingsUpdate): Result<AdminSiteSettingsDto> =
        runCatching {
            val res = api.adminUpdateSettings(body)
            if (!res.success) error(res.message ?: "Не удалось сохранить настройки")
            res.data ?: error("Пустой ответ")
        }

    suspend fun updateTitle(id: String, body: AdminTitleUpdateRequest): Result<Unit> = runCatching {
        val res = api.adminUpdateTitle(id, body)
        if (!res.success) error(res.message ?: "Не удалось сохранить тайтл")
    }

    suspend fun deleteTitle(id: String): Result<Unit> = runCatching {
        val res = api.adminDeleteTitle(id)
        if (!res.success) error(res.message ?: "Не удалось удалить тайтл")
    }

    suspend fun autoJobs(): Result<List<AutoParseJobDto>> = runCatching {
        parseAutoJobs(api.autoParsingJobsRaw())
    }

    suspend fun createAutoJob(titleId: String, sourceUrl: String?): Result<Unit> = runCatching {
        val sources = sourceUrl?.trim()?.takeIf { it.isNotBlank() }?.let { listOf(it) }
        api.createAutoParsingJob(AutoParseCreateRequest(titleId = titleId, sources = sources))
    }

    suspend fun setAutoJobEnabled(id: String, enabled: Boolean): Result<Unit> = runCatching {
        api.updateAutoParsingJob(id, AutoParseUpdateRequest(enabled = enabled))
    }

    suspend fun deleteAutoJob(id: String): Result<Unit> = runCatching {
        api.deleteAutoParsingJob(id)
    }

    suspend fun runAutoJob(id: String): Result<String> = runCatching {
        val raw = api.checkAutoParsingJob(id)
        raw.jsonMessage()
    }

    suspend fun searchSources(titleId: String): Result<List<SourceCandidateDto>> = runCatching {
        parseCandidates(api.searchMangaSources(SearchSourcesRequest(titleId)))
    }

    suspend fun importByUrl(url: String): Result<String> = runCatching {
        api.parseMangaTitle(ParseTitleRequest(url = url)).jsonMessage()
    }

    private fun JsonElement.jsonMessage(): String {
        if (this is JsonObject) {
            return this["message"]?.toString()?.trim('"')
                ?: this["data"]?.let { d ->
                    if (d is JsonObject) d["message"]?.toString()?.trim('"') else null
                }
                ?: toString()
        }
        return toString()
    }

    private fun parseAutoJobs(raw: JsonElement): List<AutoParseJobDto> {
        val arr = when (raw) {
            is JsonArray -> raw
            is JsonObject -> (raw["data"] ?: raw["jobs"])?.let {
                runCatching { it.jsonArray }.getOrNull()
            }
            else -> null
        } ?: return emptyList()
        return arr.mapNotNull {
            runCatching { json.decodeFromJsonElement<AutoParseJobDto>(it) }.getOrNull()
        }
    }

    private fun parseCandidates(raw: JsonElement): List<SourceCandidateDto> {
        val obj = raw as? JsonObject ?: return emptyList()
        val data = obj["data"] as? JsonObject ?: obj
        val arr = data["candidates"]?.let { runCatching { it.jsonArray }.getOrNull() }
            ?: return emptyList()
        return arr.mapNotNull {
            runCatching { json.decodeFromJsonElement<SourceCandidateDto>(it) }.getOrNull()
        }
    }

    private fun parseActivity(data: JsonElement?): List<String> {
        if (data == null) return emptyList()
        val arr: JsonArray = when (data) {
            is JsonArray -> data
            is JsonObject -> (data["items"] ?: data["activity"] ?: data["data"])
                ?.let { runCatching { it.jsonArray }.getOrNull() }
                ?: return emptyList()
            else -> return emptyList()
        }
        return arr.mapNotNull { el ->
            if (el !is JsonObject) return@mapNotNull el.toString()
            val type = el["type"]?.toString()?.trim('"')
            val msg = el["message"]?.toString()?.trim('"')
                ?: el["description"]?.toString()?.trim('"')
                ?: el["action"]?.toString()?.trim('"')
            listOfNotNull(type, msg).joinToString(": ").ifBlank { el.toString() }
        }
    }
}

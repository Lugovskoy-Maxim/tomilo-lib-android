package ru.tomilo.lib.mobile.data.repo

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import ru.tomilo.lib.mobile.data.api.AdminBanRequest
import ru.tomilo.lib.mobile.data.api.AdminCommentDto
import ru.tomilo.lib.mobile.data.api.AdminCommentVisibilityRequest
import ru.tomilo.lib.mobile.data.api.AdminDashboardDto
import ru.tomilo.lib.mobile.data.api.AdminRoleRequest
import ru.tomilo.lib.mobile.data.api.AdminTitleDto
import ru.tomilo.lib.mobile.data.api.AdminUserDto
import ru.tomilo.lib.mobile.data.api.NetworkModule
import ru.tomilo.lib.mobile.data.api.TomiloApi

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

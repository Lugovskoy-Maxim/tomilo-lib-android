package ru.tomilo.lib.mobile.data.repo

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import ru.tomilo.lib.mobile.data.api.BookmarkEntryDto
import ru.tomilo.lib.mobile.data.api.BookmarkStatusDto
import ru.tomilo.lib.mobile.data.api.CommentDto
import ru.tomilo.lib.mobile.data.api.ConversationPreviewDto
import ru.tomilo.lib.mobile.data.api.CreateCommentRequest
import ru.tomilo.lib.mobile.data.api.CreateConversationRequest
import ru.tomilo.lib.mobile.data.api.DirectMessageDto
import ru.tomilo.lib.mobile.data.api.LeaderboardUserDto
import ru.tomilo.lib.mobile.data.api.NetworkModule
import ru.tomilo.lib.mobile.data.api.NotificationDto
import ru.tomilo.lib.mobile.data.api.PublicUserDto
import ru.tomilo.lib.mobile.data.api.SendMessageRequest
import ru.tomilo.lib.mobile.data.api.TomiloApi
import ru.tomilo.lib.mobile.data.api.UpdateBookmarkRequest

class SocialRepository(private val api: TomiloApi) {
    private val json = NetworkModule.json

    // ── Bookmarks ───────────────────────────────────────────────
    suspend fun bookmarks(category: String? = null): Result<List<BookmarkEntryDto>> = runCatching {
        val res = api.bookmarks(category = category, grouped = false)
        if (!res.success) error(res.message ?: "Не удалось загрузить закладки")
        parseBookmarks(res.data)
    }

    suspend fun bookmarkStatus(titleId: String): Result<BookmarkStatusDto> = runCatching {
        val res = api.bookmarkStatus(titleId)
        if (!res.success) error(res.message ?: "Ошибка статуса")
        res.data ?: BookmarkStatusDto()
    }

    suspend fun addBookmark(titleId: String, category: String = "reading"): Result<Unit> =
        runCatching {
            val res = api.addBookmark(titleId, category)
            if (!res.success) error(res.message ?: res.errors?.firstOrNull() ?: "Не удалось добавить")
        }

    suspend fun removeBookmark(titleId: String): Result<Unit> = runCatching {
        val res = api.removeBookmark(titleId)
        if (!res.success) error(res.message ?: "Не удалось удалить")
    }

    suspend fun updateBookmarkCategory(titleId: String, category: String): Result<Unit> =
        runCatching {
            val res = api.updateBookmark(
                titleId,
                ru.tomilo.lib.mobile.data.api.UpdateBookmarkRequest(category),
            )
            if (!res.success) error(res.message ?: "Не удалось обновить")
        }

    private fun parseBookmarks(data: JsonElement?): List<BookmarkEntryDto> {
        if (data == null) return emptyList()
        return when (data) {
            is JsonArray -> data.mapNotNull {
                runCatching { json.decodeFromJsonElement<BookmarkEntryDto>(it) }.getOrNull()
            }
            is JsonObject -> {
                // grouped: { reading: [...], planned: [...] }
                data.values.flatMap { value ->
                    if (value is JsonArray) {
                        value.mapNotNull {
                            runCatching { json.decodeFromJsonElement<BookmarkEntryDto>(it) }.getOrNull()
                        }
                    } else emptyList()
                }
            }
            else -> emptyList()
        }
    }

    // ── Comments ────────────────────────────────────────────────
    suspend fun comments(
        entityType: String,
        entityId: String,
        page: Int = 1,
    ): Result<List<CommentDto>> = runCatching {
        val res = api.comments(
            entityType = entityType,
            entityId = entityId,
            page = page,
            limit = 40,
            includeReplies = true,
        )
        if (!res.success) error(res.message ?: "Ошибка комментариев")
        res.data?.comments.orEmpty()
    }

    suspend fun postComment(
        entityType: String,
        entityId: String,
        content: String,
        parentId: String? = null,
    ): Result<CommentDto> = runCatching {
        val res = api.createComment(
            CreateCommentRequest(
                entityType = entityType,
                entityId = entityId,
                content = content.trim(),
                parentId = parentId,
            ),
        )
        if (!res.success) error(res.message ?: res.errors?.firstOrNull() ?: "Не удалось отправить")
        res.data ?: error("Пустой ответ")
    }

    // ── Chats ───────────────────────────────────────────────────
    suspend fun conversations(): Result<List<ConversationPreviewDto>> = runCatching {
        val res = api.conversations()
        if (!res.success) {
            error(res.message ?: res.errors?.firstOrNull() ?: "Ошибка чатов")
        }
        parseConversationList(res.data)
    }

    suspend fun supportConversation(): Result<ConversationPreviewDto> = runCatching {
        val res = api.supportConversation()
        if (!res.success) error(res.message ?: "Не удалось открыть поддержку")
        parseConversation(res.data) ?: error("Пустой диалог")
    }

    suspend fun openConversationWith(userId: String): Result<ConversationPreviewDto> = runCatching {
        val res = api.createConversation(CreateConversationRequest(userId))
        if (!res.success) error(res.message ?: "Не удалось создать чат")
        parseConversation(res.data) ?: error("Пустой диалог")
    }

    suspend fun messages(conversationId: String): Result<List<DirectMessageDto>> = runCatching {
        val res = api.messages(conversationId)
        if (!res.success) error(res.message ?: "Ошибка сообщений")
        parseMessages(res.data)
    }

    suspend fun sendMessage(conversationId: String, body: String): Result<DirectMessageDto> =
        runCatching {
            val res = api.sendMessage(conversationId, SendMessageRequest(body.trim()))
            if (!res.success) error(res.message ?: "Не удалось отправить")
            res.data ?: error("Пустое сообщение")
        }

    suspend fun markConversationRead(conversationId: String) {
        runCatching { api.markConversationRead(conversationId) }
    }

    suspend fun chatsUnread(): Int = runCatching {
        val res = api.conversationsUnread()
        val data = res.data
        when (data) {
            is JsonObject -> data["count"]?.toString()?.trim('"')?.toIntOrNull() ?: 0
            else -> 0
        }
    }.getOrDefault(0)

    private fun parseConversationList(data: JsonElement?): List<ConversationPreviewDto> {
        if (data == null) return emptyList()
        val arr = when (data) {
            is JsonArray -> data
            is JsonObject -> {
                val nested = data["conversations"] ?: data["items"] ?: data["data"] ?: data["results"]
                when (nested) {
                    is JsonArray -> nested
                    is JsonObject -> (nested["conversations"] ?: nested["items"]) as? JsonArray
                    else -> null
                }
            }
            else -> null
        } ?: return emptyList()
        return arr.mapNotNull { el -> parseConversation(el) }
            .filter { it.stableId().isNotBlank() }
    }

    private fun parseConversation(data: JsonElement?): ConversationPreviewDto? {
        if (data == null) return null
        // прямой decode
        runCatching { json.decodeFromJsonElement<ConversationPreviewDto>(data) }
            .getOrNull()
            ?.takeIf { it.stableId().isNotBlank() }
            ?.let { return it }
        // ручной разбор (participant может быть вложеннее / с лишними полями)
        val obj = data as? JsonObject ?: return null
        fun str(key: String): String? =
            obj[key]?.let {
                when (it) {
                    is kotlinx.serialization.json.JsonPrimitive -> it.content
                    else -> null
                }
            }?.takeIf { it.isNotBlank() && it != "null" }
        val id = str("_id") ?: str("id") ?: return null
        val participantEl = obj["participant"]
        val participant = when (participantEl) {
            is JsonObject -> runCatching {
                json.decodeFromJsonElement<ru.tomilo.lib.mobile.data.api.ConversationUserDto>(participantEl)
            }.getOrNull() ?: ru.tomilo.lib.mobile.data.api.ConversationUserDto(
                underscoreId = (participantEl["_id"] as? kotlinx.serialization.json.JsonPrimitive)?.content,
                id = (participantEl["id"] as? kotlinx.serialization.json.JsonPrimitive)?.content,
                username = (participantEl["username"] as? kotlinx.serialization.json.JsonPrimitive)?.content,
                avatar = (participantEl["avatar"] as? kotlinx.serialization.json.JsonPrimitive)?.content,
            )
            else -> null
        }
        val unread = when (val u = obj["unreadCount"]) {
            is kotlinx.serialization.json.JsonPrimitive -> u.content.toIntOrNull() ?: 0
            else -> 0
        }
        return ConversationPreviewDto(
            underscoreId = id,
            id = id,
            type = str("type"),
            participant = participant,
            lastMessageAt = str("lastMessageAt"),
            lastMessagePreview = str("lastMessagePreview"),
            lastMessageSenderId = str("lastMessageSenderId"),
            unreadCount = unread,
        )
    }

    private fun parseMessages(data: JsonElement?): List<DirectMessageDto> {
        if (data == null) return emptyList()
        val arr = when (data) {
            is JsonArray -> data
            is JsonObject -> (data["messages"] ?: data["items"]) as? JsonArray
            else -> null
        } ?: return emptyList()
        return arr.mapNotNull {
            runCatching { json.decodeFromJsonElement<DirectMessageDto>(it) }.getOrNull()
        }
    }

    // ── Leaders ─────────────────────────────────────────────────
    suspend fun leaderboard(
        category: String = "level",
        period: String = "all",
        limit: Int = 50,
    ): Result<List<LeaderboardUserDto>> = runCatching {
        val res = api.leaderboard(category = category, period = period, limit = limit)
        if (!res.success) error(res.message ?: "Ошибка лидерборда")
        res.data?.users.orEmpty()
    }

    // ── Public profile ──────────────────────────────────────────
    suspend fun publicUser(userId: String): Result<PublicUserDto> = runCatching {
        val res = api.publicUser(userId)
        if (!res.success) error(res.message ?: "Профиль не найден")
        res.data ?: error("Профиль не найден")
    }

    // ── Notifications ───────────────────────────────────────────
    suspend fun notifications(page: Int = 1): Result<List<NotificationDto>> = runCatching {
        val res = api.notifications(page = page, limit = 40)
        if (!res.success) error(res.message ?: "Ошибка уведомлений")
        parseNotifications(res.data)
    }

    suspend fun notificationsUnread(): Int = runCatching {
        api.notificationsUnread().data?.count ?: 0
    }.getOrDefault(0)

    suspend fun markNotificationRead(id: String) {
        runCatching { api.markNotificationRead(id) }
    }

    suspend fun markAllNotificationsRead() {
        runCatching { api.markAllNotificationsRead() }
    }

    private fun parseNotifications(data: JsonElement?): List<NotificationDto> {
        if (data == null) return emptyList()
        val listEl: JsonElement? = when (data) {
            is JsonArray -> data
            is JsonObject -> {
                data["notifications"]
                    ?: data["items"]
                    ?: data["results"]
                    ?: data["docs"]
                    ?: data["list"]
            }
            else -> null
        }
        val arr = listEl?.let {
            runCatching { it.jsonArray }.getOrNull()
        } ?: return emptyList()
        return arr.mapNotNull {
            runCatching { json.decodeFromJsonElement<NotificationDto>(it) }.getOrNull()
        }
    }
}

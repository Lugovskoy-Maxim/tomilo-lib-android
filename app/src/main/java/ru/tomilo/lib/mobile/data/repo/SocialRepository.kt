package ru.tomilo.lib.mobile.data.repo

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import ru.tomilo.lib.mobile.data.api.BookmarkEntryDto
import ru.tomilo.lib.mobile.data.api.BookmarkGroupDto
import ru.tomilo.lib.mobile.data.api.BookmarkStatusDto
import ru.tomilo.lib.mobile.data.api.CommentDto
import ru.tomilo.lib.mobile.data.api.CommentReactionRequest
import ru.tomilo.lib.mobile.data.api.ConversationPreviewDto
import ru.tomilo.lib.mobile.data.api.CreateCommentRequest
import ru.tomilo.lib.mobile.data.api.CreateConversationRequest
import ru.tomilo.lib.mobile.data.api.CreateReportRequest
import ru.tomilo.lib.mobile.data.api.DeviceTokenRequest
import ru.tomilo.lib.mobile.data.api.DeviceTokenUnregisterRequest
import ru.tomilo.lib.mobile.data.api.DirectMessageDto
import ru.tomilo.lib.mobile.data.api.LeaderboardUserDto
import ru.tomilo.lib.mobile.data.api.NetworkModule
import ru.tomilo.lib.mobile.data.api.NotificationDto
import ru.tomilo.lib.mobile.data.api.PublicUserDto
import ru.tomilo.lib.mobile.data.api.SendMessageRequest
import ru.tomilo.lib.mobile.data.api.ShopDecorationDto
import ru.tomilo.lib.mobile.data.api.TomiloApi
import ru.tomilo.lib.mobile.data.api.UpdateBookmarkRequest
import ru.tomilo.lib.mobile.data.api.CreateBookmarkGroupRequest
import ru.tomilo.lib.mobile.data.api.UpdateCommentRequest

class SocialRepository(private val api: TomiloApi) {
    private val json = NetworkModule.json

    // ── Shop ───────────────────────────────────────────────────
    suspend fun shopDecorations(type: String): Result<List<ShopDecorationDto>> = runCatchingCancellable {
        val res = api.shopDecorations(type)
        if (!res.success) error(res.message ?: "Не удалось загрузить магазин")
        res.data.orEmpty().filter { it.stableId().isNotBlank() && it.isAvailable != false }
    }

    suspend fun ownedDecorations(): Result<List<ShopDecorationDto>> = runCatchingCancellable {
        val res = api.ownedDecorations()
        if (!res.success) error(res.message ?: "Не удалось загрузить инвентарь")
        res.data.orEmpty()
    }

    suspend fun purchaseDecoration(type: String, id: String): Result<Unit> = runCatchingCancellable {
        val res = api.purchaseDecoration(type, id)
        if (!res.success) error(res.message ?: res.errors?.firstOrNull() ?: "Покупка не выполнена")
    }

    suspend fun equipDecoration(type: String, id: String): Result<Unit> = runCatchingCancellable {
        val res = api.equipDecoration(type, id)
        if (!res.success) error(res.message ?: "Не удалось надеть украшение")
    }

    suspend fun unequipDecoration(type: String): Result<Unit> = runCatchingCancellable {
        val res = api.unequipDecoration(type)
        if (!res.success) error(res.message ?: "Не удалось снять украшение")
    }

    // ── Bookmarks ───────────────────────────────────────────────
    suspend fun bookmarks(category: String? = null): Result<List<BookmarkEntryDto>> = runCatchingCancellable {
        val res = api.bookmarks(category = category, grouped = false)
        if (!res.success) error(res.message ?: "Не удалось загрузить закладки")
        parseBookmarks(res.data)
    }

    suspend fun bookmarkGroups(): Result<List<BookmarkGroupDto>> = runCatchingCancellable {
        val res = api.bookmarkGroups()
        if (!res.success) error(res.message ?: "Не удалось загрузить группы")
        res.data.orEmpty()
    }

    suspend fun createBookmarkGroup(name: String): Result<BookmarkGroupDto> = runCatchingCancellable {
        val res = api.createBookmarkGroup(CreateBookmarkGroupRequest(name.trim()))
        if (!res.success) error(res.message ?: "Не удалось создать группу")
        res.data ?: error("Сервер не вернул группу")
    }

    suspend fun renameBookmarkGroup(id: String, name: String): Result<BookmarkGroupDto> = runCatchingCancellable {
        val res = api.renameBookmarkGroup(id.removePrefix("group:"), CreateBookmarkGroupRequest(name.trim()))
        if (!res.success) error(res.message ?: "Не удалось переименовать группу")
        res.data ?: error("Сервер не вернул группу")
    }

    suspend fun deleteBookmarkGroup(id: String): Result<Unit> = runCatchingCancellable {
        val res = api.deleteBookmarkGroup(id.removePrefix("group:"))
        if (!res.success) error(res.message ?: "Не удалось удалить группу")
    }

    suspend fun bookmarkStatus(titleId: String): Result<BookmarkStatusDto> = runCatchingCancellable {
        val res = api.bookmarkStatus(titleId)
        if (!res.success) error(res.message ?: "Ошибка статуса")
        res.data ?: BookmarkStatusDto()
    }

    suspend fun addBookmark(titleId: String, category: String = "reading"): Result<Unit> =
        runCatchingCancellable {
            val res = api.addBookmark(titleId, category)
            if (!res.success) error(res.message ?: res.errors?.firstOrNull() ?: "Не удалось добавить")
        }

    suspend fun removeBookmark(titleId: String): Result<Unit> = runCatchingCancellable {
        val res = api.removeBookmark(titleId)
        if (!res.success) error(res.message ?: "Не удалось удалить")
    }

    suspend fun updateBookmarkCategory(titleId: String, category: String): Result<Unit> =
        runCatchingCancellable {
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
        sortOrder: String = "newest",
    ): Result<ru.tomilo.lib.mobile.data.api.CommentsPageDto> = runCatchingCancellable {
        val res = api.comments(
            entityType = entityType,
            entityId = entityId,
            page = page,
            limit = 40,
            includeReplies = true,
            sortOrder = sortOrder,
        )
        if (!res.success) error(res.message ?: "Ошибка комментариев")
        val first = res.data ?: ru.tomilo.lib.mobile.data.api.CommentsPageDto()
        if (first.totalPages <= 1) return@runCatchingCancellable first
        val all = first.comments.toMutableList()
        for (nextPage in 2..first.totalPages.coerceAtMost(10)) {
            val next = api.comments(
                entityType = entityType,
                entityId = entityId,
                page = nextPage,
                limit = 40,
                includeReplies = true,
                sortOrder = sortOrder,
            )
            if (!next.success) break
            all += next.data?.comments.orEmpty()
        }
        first.copy(comments = all.distinctBy { it.stableId() })
    }

    suspend fun postComment(
        entityType: String,
        entityId: String,
        content: String,
        parentId: String? = null,
        isSpoiler: Boolean = false,
    ): Result<CommentDto> = runCatchingCancellable {
        val res = api.createComment(
            CreateCommentRequest(
                entityType = entityType,
                entityId = entityId,
                content = content.trim(),
                parentId = parentId,
                isSpoiler = isSpoiler.takeIf { it },
            ),
        )
        if (!res.success) error(res.message ?: res.errors?.firstOrNull() ?: "Не удалось отправить")
        res.data ?: error("Пустой ответ")
    }

    suspend fun updateComment(
        commentId: String,
        content: String,
        isSpoiler: Boolean = false,
    ): Result<CommentDto> = runCatchingCancellable {
        if (commentId.isBlank()) error("Комментарий не найден")
        val res = api.updateComment(
            commentId,
            UpdateCommentRequest(content = content.trim(), isSpoiler = isSpoiler),
        )
        if (!res.success) error(res.message ?: res.errors?.firstOrNull() ?: "Не удалось сохранить")
        res.data ?: error("Пустой ответ")
    }

    suspend fun deleteComment(commentId: String): Result<Unit> = runCatchingCancellable {
        if (commentId.isBlank()) error("Комментарий не найден")
        val res = api.deleteComment(commentId)
        if (!res.success) error(res.message ?: res.errors?.firstOrNull() ?: "Не удалось удалить")
    }

    suspend fun reportComment(
        commentId: String,
        content: String,
        titleId: String? = null,
    ): Result<Unit> = runCatchingCancellable {
        if (commentId.isBlank()) error("Комментарий не найден")
        val trimmed = content.trim()
        if (trimmed.length < 10) error("Опишите причину не короче 10 символов")
        val res = api.createReport(
            CreateReportRequest(
                reportType = "comment_report",
                content = trimmed,
                entityType = "comment",
                entityId = commentId,
                titleId = titleId?.takeIf { it.isNotBlank() },
            ),
        )
        if (!res.success) {
            error(res.message ?: res.errors?.firstOrNull() ?: "Не удалось отправить жалобу")
        }
    }

    suspend fun likeComment(commentId: String): Result<Unit> = runCatchingCancellable {
        if (commentId.isBlank()) error("Комментарий не найден")
        val res = api.likeComment(commentId)
        if (!res.success) error(res.message ?: "Не удалось поставить лайк")
    }

    suspend fun commentReactionEmojis(): Result<List<String>> = runCatchingCancellable {
        val res = api.commentReactionEmojis()
        if (!res.success) error(res.message ?: "Не удалось загрузить реакции")
        res.data?.emojis.orEmpty().filter { it.isNotBlank() }.distinct()
    }

    suspend fun toggleCommentReaction(commentId: String, emoji: String): Result<Unit> = runCatchingCancellable {
        if (commentId.isBlank()) error("Комментарий не найден")
        if (emoji.isBlank()) error("Реакция не выбрана")
        val res = api.toggleCommentReaction(commentId, CommentReactionRequest(emoji))
        if (!res.success) {
            error(res.message ?: res.errors?.firstOrNull() ?: "Не удалось поставить реакцию")
        }
    }

    // ── Chats ───────────────────────────────────────────────────
    private fun apiError(res: ru.tomilo.lib.mobile.data.api.ApiResponse<*>, fallback: String): String {
        val msg = res.message?.takeIf { it.isNotBlank() }
            ?: res.errors?.firstOrNull { it.isNotBlank() }
            ?: fallback
        // Понятные тексты с сервера
        return when {
            msg.contains("friends", ignoreCase = true) ||
                msg.contains("only message friends", ignoreCase = true) ->
                "Писать можно только друзьям. Добавьте пользователя в друзья на сайте."
            msg.contains("Not allowed", ignoreCase = true) ->
                "Нет доступа к этому диалогу"
            msg.contains("Invalid token", ignoreCase = true) ||
                msg.contains("Unauthorized", ignoreCase = true) ->
                "Сессия устарела — войдите снова"
            msg.contains("not found", ignoreCase = true) ->
                "Диалог не найден"
            else -> msg
        }
    }

    suspend fun conversations(): Result<List<ConversationPreviewDto>> = runCatchingCancellable {
        val res = api.conversations()
        if (!res.success) error(apiError(res, "Ошибка чатов"))
        parseConversationList(res.data)
    }

    suspend fun supportConversation(): Result<ConversationPreviewDto> = runCatchingCancellable {
        val res = api.supportConversation()
        if (!res.success) error(apiError(res, "Не удалось открыть поддержку"))
        parseConversation(res.data) ?: error("Пустой диалог поддержки")
    }

    /** Admin: inbox of all support tickets from users. */
    suspend fun supportInbox(): Result<List<ConversationPreviewDto>> = runCatchingCancellable {
        val res = api.supportInbox()
        if (!res.success) error(apiError(res, "Не удалось загрузить поддержку"))
        parseConversationList(res.data)
    }

    suspend fun openConversationWith(userId: String): Result<ConversationPreviewDto> = runCatchingCancellable {
        if (userId.isBlank()) error("Не указан пользователь")
        val res = api.createConversation(CreateConversationRequest(userId))
        if (!res.success) error(apiError(res, "Не удалось создать чат"))
        parseConversation(res.data) ?: error("Пустой диалог")
    }

    suspend fun messages(conversationId: String): Result<List<DirectMessageDto>> = runCatchingCancellable {
        val id = conversationId.trim()
        if (id.isBlank()) error("Пустой id диалога")
        val res = api.messages(id)
        if (!res.success) error(apiError(res, "Ошибка сообщений"))
        parseMessages(res.data)
    }

    suspend fun sendMessage(conversationId: String, body: String): Result<DirectMessageDto> =
        runCatchingCancellable {
            val id = conversationId.trim()
            if (id.isBlank()) error("Пустой id диалога")
            val text = body.trim()
            if (text.isEmpty()) error("Пустое сообщение")
            val res = api.sendMessage(id, SendMessageRequest(body = text))
            if (!res.success) error(apiError(res, "Не удалось отправить"))
            parseMessage(res.data) ?: error("Сервер не вернул сообщение")
        }

    suspend fun markConversationRead(conversationId: String) {
        if (conversationId.isBlank()) return
        runCatchingCancellable { api.markConversationRead(conversationId) }
    }

    suspend fun chatsUnread(): Int = runCatchingCancellable {
        val res = api.conversationsUnread()
        val data = res.data
        when (data) {
            is JsonObject -> data["count"]?.toString()?.trim('"')?.toIntOrNull() ?: 0
            is kotlinx.serialization.json.JsonPrimitive -> data.content.toIntOrNull() ?: 0
            else -> 0
        }
    }.getOrDefault(0)

    suspend fun friends(): Result<List<ru.tomilo.lib.mobile.data.api.FriendEntryDto>> = runCatchingCancellable {
        val res = api.friends()
        if (!res.success) error(apiError(res, "Не удалось загрузить друзей"))
        res.data.orEmpty()
    }

    suspend fun friendRequests(): Result<ru.tomilo.lib.mobile.data.api.FriendRequestsDto> = runCatchingCancellable {
        val res = api.friendRequests()
        if (!res.success) error(apiError(res, "Не удалось загрузить заявки"))
        res.data ?: ru.tomilo.lib.mobile.data.api.FriendRequestsDto()
    }

    suspend fun searchFriends(query: String): Result<List<ru.tomilo.lib.mobile.data.api.FriendSearchResultDto>> = runCatchingCancellable {
        val q = query.trim()
        if (q.length < 2) return@runCatchingCancellable emptyList()
        val res = api.searchFriends(q)
        if (!res.success) error(apiError(res, "Не удалось найти пользователей"))
        res.data.orEmpty()
    }

    suspend fun friendStatus(userId: String): Result<String> = runCatchingCancellable {
        val res = api.friendStatus(userId)
        if (!res.success) error(apiError(res, "Не удалось проверить статус дружбы"))
        res.data?.status ?: "none"
    }

    suspend fun sendFriendRequest(userId: String): Result<Unit> = runCatchingCancellable {
        val res = api.sendFriendRequest(ru.tomilo.lib.mobile.data.api.SendFriendRequestDto(userId))
        if (!res.success) error(apiError(res, "Не удалось отправить заявку"))
    }

    suspend fun acceptFriendRequest(requestId: String): Result<Unit> = runCatchingCancellable {
        val res = api.acceptFriendRequest(requestId)
        if (!res.success) error(apiError(res, "Не удалось принять заявку"))
    }

    suspend fun rejectFriendRequest(requestId: String): Result<Unit> = runCatchingCancellable {
        val res = api.rejectFriendRequest(requestId)
        if (!res.success) error(apiError(res, "Не удалось отклонить заявку"))
    }

    suspend fun removeFriend(userId: String): Result<Unit> = runCatchingCancellable {
        val res = api.removeFriend(userId)
        if (!res.success) error(apiError(res, "Не удалось удалить друга"))
    }

    private fun jsonStr(el: JsonElement?): String? = when (el) {
        is kotlinx.serialization.json.JsonPrimitive ->
            el.content.takeIf { it.isNotBlank() && it != "null" }
        is JsonObject -> {
            (el["\$oid"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                ?: (el["\$date"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                ?: (el["_id"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                ?: (el["id"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                ?: jsonStr(el["\$date"])
        }
        else -> null
    }

    private fun parseConversationList(data: JsonElement?): List<ConversationPreviewDto> {
        if (data == null) return emptyList()
        val arr: JsonArray? = when (data) {
            is JsonArray -> data
            is JsonObject -> {
                val nested = data["conversations"]
                    ?: data["items"]
                    ?: data["data"]
                    ?: data["results"]
                    ?: data["docs"]
                when (nested) {
                    is JsonArray -> nested
                    is JsonObject -> {
                        val inner = nested["conversations"]
                            ?: nested["items"]
                            ?: nested["results"]
                            ?: nested["docs"]
                            ?: nested["data"]
                        inner as? JsonArray
                    }
                    else -> null
                }
            }
            else -> null
        }
        if (arr == null) {
            // иногда data — один объект
            parseConversation(data)?.let { return listOf(it) }
            return emptyList()
        }
        return arr.mapNotNull { parseConversation(it) }
            .filter { it.stableId().isNotBlank() }
    }

    private fun parseConversation(data: JsonElement?): ConversationPreviewDto? {
        if (data == null) return null
        val envelope = data as? JsonObject ?: return null
        // Некоторые версии API оборачивают элемент inbox в { conversation, user }.
        val obj = (envelope["conversation"] as? JsonObject) ?: envelope
        val id = jsonStr(obj["_id"])
            ?: jsonStr(obj["id"])
            ?: jsonStr(obj["conversationId"])
            ?: return null
        val participantEl = obj["participant"]
            ?: envelope["participant"]
            ?: envelope["user"]
            ?: envelope["customer"]
        val participant = when (participantEl) {
            is JsonObject -> {
                val isSupportFlag =
                    (participantEl["isSupport"] as? kotlinx.serialization.json.JsonPrimitive)
                        ?.content?.toBooleanStrictOrNull() == true ||
                        jsonStr(participantEl["username"])?.equals("Поддержка", true) == true ||
                        jsonStr(participantEl["_id"]) == "support" ||
                        jsonStr(participantEl["id"]) == "support"
                ru.tomilo.lib.mobile.data.api.ConversationUserDto(
                    underscoreId = jsonStr(participantEl["_id"]),
                    id = jsonStr(participantEl["id"]),
                    username = jsonStr(participantEl["username"]),
                    avatar = jsonStr(participantEl["avatar"]),
                    level = (participantEl["level"] as? kotlinx.serialization.json.JsonPrimitive)
                        ?.content?.toIntOrNull(),
                    isSupport = isSupportFlag,
                )
            }
            else -> null
        }
        val unread = when (val u = obj["unreadCount"] ?: envelope["unreadCount"]) {
            is kotlinx.serialization.json.JsonPrimitive -> u.content.toIntOrNull() ?: 0
            else -> 0
        }
        val type = jsonStr(obj["type"] ?: envelope["type"])
            ?: if (participant?.isSupport == true || participant?.stableId() == "support") "support"
            else "direct"
        return ConversationPreviewDto(
            underscoreId = id,
            id = id,
            type = type,
            participant = participant,
            lastMessageAt = jsonStr(obj["lastMessageAt"] ?: envelope["lastMessageAt"]),
            lastMessagePreview = jsonStr(obj["lastMessagePreview"] ?: envelope["lastMessagePreview"]),
            lastMessageSenderId = jsonStr(obj["lastMessageSenderId"] ?: envelope["lastMessageSenderId"]),
            unreadCount = unread,
        )
    }

    private fun parseMessages(data: JsonElement?): List<DirectMessageDto> {
        if (data == null) return emptyList()
        val arr = when (data) {
            is JsonArray -> data
            is JsonObject -> {
                val nested = data["messages"] ?: data["items"] ?: data["data"] ?: data["docs"]
                when (nested) {
                    is JsonArray -> nested
                    is JsonObject -> {
                        val inner = nested["messages"] ?: nested["items"]
                        inner as? JsonArray
                    }
                    else -> null
                }
            }
            else -> null
        }
        if (arr == null) return emptyList()
        return arr.mapNotNull { el -> parseMessage(el) }
            .filter { it.stableId().isNotBlank() }
    }

    private fun parseMessage(data: JsonElement?): DirectMessageDto? {
        if (data == null) return null
        val obj = data as? JsonObject ?: return null
        val id = jsonStr(obj["_id"]) ?: jsonStr(obj["id"]) ?: return null
        val body = jsonStr(obj["body"])
            ?: jsonStr(obj["text"])
            ?: jsonStr(obj["content"])
            ?: jsonStr(obj["message"])
        return DirectMessageDto(
            underscoreId = id,
            id = id,
            conversationId = jsonStr(obj["conversationId"]),
            senderId = obj["senderId"],
            body = body,
            deletedAt = jsonStr(obj["deletedAt"]),
            createdAt = obj["createdAt"],
        )
    }

    // ── Leaders ─────────────────────────────────────────────────
    suspend fun leaderboard(
        category: String = "level",
        period: String = "all",
        limit: Int = 50,
    ): Result<List<LeaderboardUserDto>> = runCatchingCancellable {
        val res = api.leaderboard(category = category, period = period, limit = limit)
        if (!res.success) error(res.message ?: "Ошибка лидерборда")
        res.data?.users.orEmpty()
    }

    // ── Public profile ──────────────────────────────────────────
    suspend fun publicUser(userId: String): Result<PublicUserDto> = runCatchingCancellable {
        val res = api.publicUser(userId)
        if (!res.success) error(res.message ?: "Профиль не найден")
        res.data ?: error("Профиль не найден")
    }

    // ── Notifications ───────────────────────────────────────────
    suspend fun notifications(page: Int = 1): Result<List<NotificationDto>> = runCatchingCancellable {
        val res = api.notifications(page = page, limit = 40)
        if (!res.success) error(res.message ?: "Ошибка уведомлений")
        parseNotifications(res.data)
    }

    suspend fun notificationsUnread(): Result<Int> = runCatchingCancellable {
        val response = api.notificationsUnread()
        if (!response.success) error(response.message ?: "Не удалось загрузить счётчик уведомлений")
        response.data?.count ?: error("Сервер не вернул счётчик уведомлений")
    }

    suspend fun markNotificationRead(id: String) {
        runCatchingCancellable { api.markNotificationRead(id) }
    }

    suspend fun markAllNotificationsRead(): Result<Unit> = runCatchingCancellable {
        val response = api.markAllNotificationsRead()
        if (!response.success) error(response.message ?: "Не удалось отметить уведомления прочитанными")
    }

    suspend fun deleteNotification(id: String): Result<Unit> = runCatchingCancellable {
        if (id.isBlank()) error("Уведомление не найдено")
        val res = api.deleteNotification(id)
        if (!res.success) error(res.message ?: "Не удалось удалить уведомление")
    }

    // ── Push (device token: FCM/RuStore Push) ───────────────────────
    suspend fun registerDeviceToken(
        token: String,
        appVersion: String? = null,
        provider: String = "fcm",
    ): Result<Unit> = runCatchingCancellable {
        val res = api.registerDeviceToken(
            DeviceTokenRequest(token = token, appVersion = appVersion, provider = provider),
        )
        if (!res.success) error(res.message ?: "Не удалось зарегистрировать push-токен")
    }

    suspend fun unregisterDeviceToken(token: String): Result<Unit> = runCatchingCancellable {
        val res = api.unregisterDeviceToken(DeviceTokenUnregisterRequest(token = token))
        if (!res.success) error(res.message ?: "Не удалось удалить push-токен")
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

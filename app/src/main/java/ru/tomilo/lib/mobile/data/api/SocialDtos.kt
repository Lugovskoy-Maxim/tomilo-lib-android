package ru.tomilo.lib.mobile.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class PublicUserDto(
    @SerialName("_id") val underscoreId: String? = null,
    val id: String? = null,
    val username: String? = null,
    val avatar: String? = null,
    val role: String? = null,
    val level: Int? = null,
    val experience: Int? = null,
    val bio: String? = null,
    val subscriptionExpiresAt: String? = null,
    val commentsCount: Int? = null,
    val likesReceivedCount: Int? = null,
    val chaptersRead: Int? = null,
    val readingTimeMinutes: Int? = null,
    val currentStreak: Int? = null,
    val longestStreak: Int? = null,
    val titlesReadCount: Int? = null,
    val completedTitlesCount: Int? = null,
    val ratingsCount: Int? = null,
    val createdAt: String? = null,
    val showStats: Boolean? = null,
) {
    fun stableId(): String = id ?: underscoreId.orEmpty()
}

@Serializable
data class LeaderboardResponseDto(
    val users: List<LeaderboardUserDto> = emptyList(),
    val total: Int = 0,
    val category: String? = null,
    val period: String? = null,
)

@Serializable
data class LeaderboardUserDto(
    @SerialName("_id") val underscoreId: String? = null,
    val id: String? = null,
    val username: String? = null,
    val avatar: String? = null,
    val level: Int? = null,
    val experience: Int? = null,
    val readingTimeMinutes: Int? = null,
    val chaptersRead: Int? = null,
    val ratingsCount: Int? = null,
    val commentsCount: Int? = null,
    val likesReceivedCount: Int? = null,
    val currentStreak: Int? = null,
    val longestStreak: Int? = null,
    val subscriptionExpiresAt: String? = null,
    val balance: Int? = null,
) {
    fun stableId(): String = id ?: underscoreId.orEmpty()
}

@Serializable
data class BookmarkEntryDto(
    val titleId: JsonElement? = null,
    val category: String? = null,
    val addedAt: String? = null,
    /** Иногда клиент/legacy кладёт тайтл отдельно; обычно populate идёт в titleId. */
    val title: BookmarkTitleDto? = null,
) {
    fun resolvedTitle(): BookmarkTitleDto? {
        title?.let { return it }
        val el = titleId
        if (el is JsonObject) {
            return runCatching {
                NetworkModule.json.decodeFromJsonElement<BookmarkTitleDto>(el)
            }.getOrNull()
        }
        return null
    }

    fun resolvedTitleId(): String {
        resolvedTitle()?._id?.let { if (it.isNotBlank()) return it }
        resolvedTitle()?.id?.let { if (it.isNotBlank()) return it }
        val el = titleId ?: return ""
        return when (el) {
            is JsonPrimitive -> el.contentOrNull.orEmpty()
            is JsonObject -> {
                el["_id"]?.jsonPrimitive?.contentOrNull
                    ?: el["id"]?.jsonPrimitive?.contentOrNull
                    ?: ""
            }
            else -> ""
        }
    }

    fun displayName(): String {
        val t = resolvedTitle()
        return t?.name?.takeIf { it.isNotBlank() }
            ?: t?.title?.takeIf { it.isNotBlank() }
            ?: t?.slug?.takeIf { it.isNotBlank() }
            ?: "Тайтл"
    }

    fun coverPath(): String? = resolvedTitle()?.coverImage ?: resolvedTitle()?.cover
}

@Serializable
data class BookmarkTitleDto(
    @SerialName("_id") val _id: String? = null,
    val id: String? = null,
    val name: String? = null,
    /** legacy / неверный select на бэке */
    val title: String? = null,
    val slug: String? = null,
    val coverImage: String? = null,
    val cover: String? = null,
    val type: String? = null,
    val status: String? = null,
    val totalChapters: Int? = null,
    val chaptersCount: Int? = null,
    val averageRating: Double? = null,
)

@Serializable
data class BookmarkStatusDto(
    val isBookmarked: Boolean = false,
    /** legacy alias */
    val bookmarked: Boolean = false,
    val category: String? = null,
) {
    fun active(): Boolean = isBookmarked || bookmarked
}

@Serializable
data class UpdateBookmarkRequest(val category: String)

@Serializable
data class CreateCommentRequest(
    val entityType: String,
    val entityId: String,
    val content: String,
    val parentId: String? = null,
    val isSpoiler: Boolean? = null,
)

@Serializable
data class CommentsPageDto(
    val comments: List<CommentDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val totalPages: Int = 0,
    val limit: Int = 20,
)

@Serializable
data class CommentDto(
    @SerialName("_id") val underscoreId: String? = null,
    val id: String? = null,
    val content: String? = null,
    val entityType: String? = null,
    val entityId: String? = null,
    val parentId: String? = null,
    val isSpoiler: Boolean? = null,
    val likesCount: Int? = null,
    val dislikesCount: Int? = null,
    val repliesCount: Int? = null,
    val createdAt: String? = null,
    val user: CommentUserDto? = null,
    val author: CommentUserDto? = null,
    val replies: List<CommentDto>? = null,
    val hiddenBySystem: Boolean? = null,
) {
    fun stableId(): String = id ?: underscoreId.orEmpty()
    fun authorName(): String = (user ?: author)?.username ?: "Аноним"
    fun authorId(): String = (user ?: author)?.stableId().orEmpty()
    fun authorAvatar(): String? = (user ?: author)?.avatar
}

@Serializable
data class CommentUserDto(
    @SerialName("_id") val underscoreId: String? = null,
    val id: String? = null,
    val username: String? = null,
    val avatar: String? = null,
    val level: Int? = null,
) {
    fun stableId(): String = id ?: underscoreId.orEmpty()
}

@Serializable
data class ConversationPreviewDto(
    @SerialName("_id") val underscoreId: String? = null,
    val id: String? = null,
    val type: String? = null,
    val participant: ConversationUserDto? = null,
    val lastMessageAt: String? = null,
    val lastMessagePreview: String? = null,
    val lastMessageSenderId: String? = null,
    val unreadCount: Int = 0,
) {
    fun stableId(): String = id ?: underscoreId.orEmpty()
}

@Serializable
data class ConversationUserDto(
    @SerialName("_id") val underscoreId: String? = null,
    val id: String? = null,
    val username: String? = null,
    val avatar: String? = null,
    val level: Int? = null,
    val isSupport: Boolean? = null,
) {
    fun stableId(): String = id ?: underscoreId.orEmpty()
}

@Serializable
data class CreateConversationRequest(val userId: String)

@Serializable
data class SendMessageRequest(
    val body: String,
    val replyToId: String? = null,
)

@Serializable
data class MessagesPageDto(
    val messages: List<DirectMessageDto> = emptyList(),
    val hasMore: Boolean = false,
)

@Serializable
data class DirectMessageDto(
    @SerialName("_id") val underscoreId: String? = null,
    val id: String? = null,
    val conversationId: String? = null,
    val senderId: String? = null,
    val body: String? = null,
    val deletedAt: String? = null,
    val createdAt: String? = null,
) {
    fun stableId(): String = id ?: underscoreId.orEmpty()
}

@Serializable
data class UnreadCountDto(val count: Int = 0)

@Serializable
data class NotificationsPageDto(
    val notifications: List<NotificationDto> = emptyList(),
    val pagination: NotificationPaginationDto? = null,
    val total: Int? = null,
    val page: Int? = null,
    val limit: Int? = null,
)

@Serializable
data class NotificationPaginationDto(
    val total: Int? = null,
    val page: Int? = null,
    val limit: Int? = null,
    val pages: Int? = null,
    val totalPages: Int? = null,
)

@Serializable
data class NotificationDto(
    @SerialName("_id") val underscoreId: String? = null,
    val id: String? = null,
    val type: String? = null,
    val title: String? = null,
    val message: String? = null,
    val isRead: Boolean? = null,
    val createdAt: String? = null,
    val linkUrl: String? = null,
    val metadata: JsonElement? = null,
) {
    fun stableId(): String = id ?: underscoreId.orEmpty()
    fun read(): Boolean = isRead == true
}

/** Flexible bookmarks list: either array or grouped map. */
@Serializable
data class BookmarksFlexible(
    val items: List<BookmarkEntryDto>? = null,
)

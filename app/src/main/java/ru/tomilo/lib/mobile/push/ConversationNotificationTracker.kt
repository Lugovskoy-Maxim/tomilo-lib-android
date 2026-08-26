package ru.tomilo.lib.mobile.push

internal data class ConversationSnapshot(
    val conversationId: String,
    val participantName: String,
    val preview: String,
    val signature: String,
    val unreadCount: Int,
)

internal fun findConversationUpdates(
    previous: Map<String, String>,
    current: List<ConversationSnapshot>,
): List<ConversationSnapshot> = current.filter { item ->
    item.conversationId.isNotBlank() &&
        item.unreadCount > 0 &&
        previous[item.conversationId] != item.signature
}

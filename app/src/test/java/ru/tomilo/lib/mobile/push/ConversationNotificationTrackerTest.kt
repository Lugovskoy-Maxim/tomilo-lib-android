package ru.tomilo.lib.mobile.push

import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationNotificationTrackerTest {
    @Test
    fun `reports a new unread message`() {
        val current = listOf(
            ConversationSnapshot("a", "Анна", "Привет", "2026-08-26T10:00|1|Привет", 1),
            ConversationSnapshot("b", "Борис", "Прочитано", "2026-08-26T09:00|0|Прочитано", 0),
        )

        assertEquals(listOf("a"), findConversationUpdates(emptyMap(), current).map { it.conversationId })
    }

    @Test
    fun `does not repeat the same message`() {
        val item = ConversationSnapshot("a", "Анна", "Привет", "same", 2)

        assertEquals(emptyList<ConversationSnapshot>(), findConversationUpdates(mapOf("a" to "same"), listOf(item)))
    }

    @Test
    fun `reports another message in an existing conversation`() {
        val item = ConversationSnapshot("a", "Анна", "Ещё сообщение", "new", 2)

        assertEquals(listOf("a"), findConversationUpdates(mapOf("a" to "old"), listOf(item)).map { it.conversationId })
    }
}

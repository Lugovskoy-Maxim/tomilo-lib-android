package ru.tomilo.lib.mobile.push

import org.junit.Assert.assertEquals
import org.junit.Test

class BookmarkChapterTrackerTest {
    @Test
    fun `reports only increased chapter counts`() {
        val updates = findBookmarkChapterUpdates(
            previous = mapOf("a" to 10, "b" to 4, "c" to 8),
            current = listOf(
                BookmarkChapterSnapshot("a", "A", 11),
                BookmarkChapterSnapshot("b", "B", 4),
                BookmarkChapterSnapshot("c", "C", 7),
                BookmarkChapterSnapshot("new", "New", 3),
            ),
        )

        assertEquals(listOf("a"), updates.map { it.titleId })
    }

    @Test
    fun `suppresses titles already delivered by server notification`() {
        val updates = findBookmarkChapterUpdates(
            previous = mapOf("a" to 10, "b" to 3),
            current = listOf(
                BookmarkChapterSnapshot("a", "A", 11),
                BookmarkChapterSnapshot("b", "B", 5),
            ),
            alreadyDeliveredTitleIds = setOf("a"),
        )

        assertEquals(listOf("b"), updates.map { it.titleId })
    }
}

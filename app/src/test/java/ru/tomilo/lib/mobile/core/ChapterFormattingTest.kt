package ru.tomilo.lib.mobile.core

import org.junit.Assert.assertEquals
import org.junit.Test

class ChapterFormattingTest {
    @Test
    fun formatChapterTitleDropsRepeatedChapterPrefix() {
        assertEquals("Глава 1", formatChapterTitle("1", "Глава 1"))
        assertEquals("Глава 1", formatChapterTitle("1", "1"))
        assertEquals("Глава 1 · Пролог", formatChapterTitle("1", "Глава 1: Пролог"))
    }

    @Test
    fun badgeIgnoresTitleDuplicatedInChapterField() {
        assertEquals(
            "Гл. 12",
            chapterBadgeText(
                chapter = "Solo Leveling",
                chapterNumber = "12",
                titleName = "Solo Leveling",
                totalChapters = 200,
            ),
        )
        assertEquals(
            "48 гл.",
            chapterBadgeText(
                chapter = "Solo Leveling",
                chapterNumber = null,
                titleName = "Solo Leveling",
                totalChapters = 48,
            ),
        )
        assertEquals(
            "Гл. 5",
            chapterBadgeText(
                chapter = "Глава 5",
                chapterNumber = null,
                titleName = "Башня бога",
            ),
        )
    }

    @Test
    fun updateLineDoesNotRepeatTitleName() {
        assertEquals(
            "Новая глава 12",
            chapterUpdateSubtitle(
                chapter = "Solo Leveling",
                chapterNumber = "12.0",
                titleName = "Solo Leveling",
            ),
        )
        assertEquals(
            "Недавно обновлено",
            chapterUpdateSubtitle(
                chapter = "Solo Leveling",
                chapterNumber = null,
                titleName = "Solo Leveling",
            ),
        )
        assertEquals(
            "Новая глава 3 · Пролог",
            chapterUpdateSubtitle(
                chapter = "Глава 3: Пролог",
                chapterNumber = null,
                titleName = "Башня бога",
            ),
        )
    }
}

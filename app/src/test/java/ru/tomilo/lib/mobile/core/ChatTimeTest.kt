package ru.tomilo.lib.mobile.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class ChatTimeTest {
    private val zone = ZoneOffset.UTC
    private val now = LocalDateTime.of(2026, 8, 30, 18, 0).toInstant(ZoneOffset.UTC)

    @Test
    fun todayShowsClock() {
        assertEquals("15:04", ChatTime.label("2026-08-30T15:04:12.000Z", now, zone))
    }

    @Test
    fun yesterdayShowsPrefix() {
        assertEquals("вчера 21:45", ChatTime.label("2026-08-29T21:45:00Z", now, zone))
    }

    @Test
    fun olderSameYearShowsDayAndMonth() {
        val label = ChatTime.label("2026-03-12T09:01:00Z", now, zone)
        assertTrue(label!!.contains("09:01"))
        assertTrue(label.contains("мар") || label.contains("12"))
    }

    @Test
    fun previousYearShowsNumericDate() {
        assertEquals("12.08.2025, 09:01", ChatTime.label("2025-08-12T09:01:00Z", now, zone))
    }

    @Test
    fun parsesSpaceSeparatedUtc() {
        assertEquals("15:04", ChatTime.label("2026-08-30 15:04:12", now, zone))
    }
}

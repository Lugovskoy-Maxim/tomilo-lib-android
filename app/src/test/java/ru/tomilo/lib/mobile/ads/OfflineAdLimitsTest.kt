package ru.tomilo.lib.mobile.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class OfflineAdLimitsTest {
    @Test
    fun creditsNeverExceedBankCap() {
        assertEquals(5, OfflineAdLimits.creditsAfterGrant(4, 1))
        assertEquals(5, OfflineAdLimits.creditsAfterGrant(5, 3))
        assertEquals(5, OfflineAdLimits.clampCredits(99))
        assertEquals(0, OfflineAdLimits.clampCredits(-2))
    }

    @Test
    fun dailyRewardedResetsOnNewDay() {
        assertEquals(8, OfflineAdLimits.rewardedRemaining(8, "2026-09-09", "2026-09-10"))
        assertTrue(OfflineAdLimits.canGrantRewarded(8, "2026-09-09", "2026-09-10"))
        assertEquals(0, OfflineAdLimits.rewardedRemaining(8, "2026-09-10", "2026-09-10"))
        assertFalse(OfflineAdLimits.canGrantRewarded(8, "2026-09-10", "2026-09-10"))
        assertEquals(3, OfflineAdLimits.rewardedRemaining(5, "2026-09-10", "2026-09-10"))
    }

    @Test
    fun largeGrantHitsBankCap() {
        assertEquals(5, OfflineAdLimits.creditsAfterGrant(0, 40))
        assertEquals(1, OfflineAdLimits.creditsAfterGrant(0, 1))
    }

    @Test
    fun readPassIsTimeBoxed() {
        val now = 1_000_000L
        val until = OfflineAdLimits.readPassUntil(now)
        assertEquals(now + OfflineAdLimits.READ_PASS_MS, until)
        assertTrue(OfflineAdLimits.isReadPassActive(until, now + 1))
        assertFalse(OfflineAdLimits.isReadPassActive(until, until))
        assertFalse(OfflineAdLimits.isReadPassActive(until, until + 1))
    }

    @Test
    fun todayKeyUsesCalendarDateInZone() {
        val utcNoon = Instant.parse("2026-05-06T12:00:00Z").toEpochMilli()
        assertEquals("2026-05-06", OfflineAdLimits.todayKey(utcNoon, ZoneOffset.UTC))
        assertEquals("2026-05-07", OfflineAdLimits.todayKey(utcNoon, ZoneOffset.ofHours(14)))
    }

    @Test
    fun offlineReadNeverGatedWithoutNetwork() {
        assertFalse(OfflineAdLimits.requiresAdForOfflineRead(isPremium = false, online = false, hasReadPass = false))
        assertTrue(OfflineAdLimits.requiresAdForOfflineRead(isPremium = false, online = true, hasReadPass = false))
        assertFalse(OfflineAdLimits.requiresAdForOfflineRead(isPremium = false, online = true, hasReadPass = true))
        assertFalse(OfflineAdLimits.requiresAdForOfflineRead(isPremium = true, online = true, hasReadPass = false))
    }

    @Test
    fun interChapterCountdownIsFiveToOne() {
        assertEquals(listOf(5, 4, 3, 2, 1), ChapterTransitionAds.countdownTicks().toList())
    }
}

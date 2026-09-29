package ru.tomilo.lib.mobile.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChapterAdPolicyTest {
    @Test
    fun `attempt when interstitial enabled and cooldown elapsed`() {
        assertTrue(
            ChapterAdPolicy.shouldAttempt(
                premium = false,
                cooldownElapsed = true,
                interstitialEnabled = true,
            ),
        )
    }

    @Test
    fun `skip for premium cooldown or disabled unit`() {
        assertFalse(
            ChapterAdPolicy.shouldAttempt(
                premium = true,
                cooldownElapsed = true,
                interstitialEnabled = true,
            ),
        )
        assertFalse(
            ChapterAdPolicy.shouldAttempt(
                premium = false,
                cooldownElapsed = false,
                interstitialEnabled = true,
            ),
        )
        assertFalse(
            ChapterAdPolicy.shouldAttempt(
                premium = false,
                cooldownElapsed = true,
                interstitialEnabled = false,
            ),
        )
    }

    @Test
    fun `countdown waits five seconds`() {
        assertEquals(listOf(5, 4, 3, 2, 1), ChapterAdPolicy.countdownTicks().toList())
    }
}

package ru.tomilo.lib.mobile.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PageRetryPolicyTest {
    @Test
    fun automaticRetriesStopAtTheEndOfEachAttemptCycle() {
        assertTrue(PageRetryPolicy.shouldRetry(attempt = 0, maxAttempts = 3))
        assertTrue(PageRetryPolicy.shouldRetry(attempt = 1, maxAttempts = 3))
        assertFalse(PageRetryPolicy.shouldRetry(attempt = 2, maxAttempts = 3))
    }

    @Test
    fun aManualRetryStartsAnotherFullCycleAfterCacheBustNonceIncrements() {
        assertTrue(PageRetryPolicy.shouldRetry(attempt = 3, maxAttempts = 3))
        assertTrue(PageRetryPolicy.shouldRetry(attempt = 4, maxAttempts = 3))
        assertFalse(PageRetryPolicy.shouldRetry(attempt = 5, maxAttempts = 3))
    }

    @Test
    fun invalidAttemptCountsNeverScheduleAnAutomaticRetry() {
        assertFalse(PageRetryPolicy.shouldRetry(attempt = 0, maxAttempts = 0))
        assertFalse(PageRetryPolicy.shouldRetry(attempt = 0, maxAttempts = 1))
    }
}

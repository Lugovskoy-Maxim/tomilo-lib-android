package ru.tomilo.lib.mobile.core

/** Limits automatic retries per user-triggered page load, even when cache-bust attempts keep increasing. */
object PageRetryPolicy {
    fun shouldRetry(attempt: Int, maxAttempts: Int): Boolean {
        if (maxAttempts <= 1) return false
        return attempt.coerceAtLeast(0) % maxAttempts < maxAttempts - 1
    }
}

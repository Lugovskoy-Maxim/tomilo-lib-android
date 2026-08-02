package ru.tomilo.lib.mobile.core

import java.time.Instant

object Premium {
    fun isActive(subscriptionExpiresAt: String?): Boolean {
        if (subscriptionExpiresAt.isNullOrBlank()) return false
        return try {
            Instant.parse(subscriptionExpiresAt).isAfter(Instant.now())
        } catch (_: Exception) {
            false
        }
    }
}

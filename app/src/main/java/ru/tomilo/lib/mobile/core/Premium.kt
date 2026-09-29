package ru.tomilo.lib.mobile.core

import java.time.Instant

object Premium {
    /**
     * Мобильная реклама выключается только этой проверкой.
     * Оплаченная пауза рекламы на сайте (`siteAdPauseExpiresAt`, цены `adPause*`)
     * и флаги модерации контента на приложение не распространяются.
     */
    fun isActive(subscriptionExpiresAt: String?): Boolean {
        if (subscriptionExpiresAt.isNullOrBlank()) return false
        return try {
            Instant.parse(subscriptionExpiresAt).isAfter(Instant.now())
        } catch (_: Exception) {
            false
        }
    }
}

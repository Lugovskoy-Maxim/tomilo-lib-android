package ru.tomilo.lib.mobile.core

import java.time.Instant

/**
 * Доступ к платным главам — как на сайте (chapter-access.ts).
 * isPaid + freeAt ещё не наступил → только Premium или разблокировка за монеты.
 */
object ChapterAccess {
    fun isPremiumOnly(
        isPaid: Boolean?,
        freeAt: String?,
        unlockedByActivityCoins: Boolean? = false,
    ): Boolean {
        if (isPaid != true) return false
        if (unlockedByActivityCoins == true) return false
        if (freeAt.isNullOrBlank()) return true
        return try {
            Instant.parse(freeAt).isAfter(Instant.now())
        } catch (_: Exception) {
            // не ISO — пробуем как millis или оставляем «ещё платная»
            freeAt.toLongOrNull()?.let { ms ->
                return ms > System.currentTimeMillis()
            }
            true
        }
    }

    fun userCanRead(
        isPaid: Boolean?,
        freeAt: String?,
        unlockedByActivityCoins: Boolean? = false,
        subscriptionExpiresAt: String?,
    ): Boolean {
        if (!isPremiumOnly(isPaid, freeAt, unlockedByActivityCoins)) return true
        return Premium.isActive(subscriptionExpiresAt)
    }

    fun freeOpensAtLabel(freeAt: String?): String? {
        if (freeAt.isNullOrBlank()) return null
        return try {
            val inst = Instant.parse(freeAt)
            if (!inst.isAfter(Instant.now())) return null
            // простой ISO → локальная подсказка
            freeAt.replace('T', ' ').take(16)
        } catch (_: Exception) {
            freeAt.take(16)
        }
    }

    fun lockHint(
        isPaid: Boolean?,
        freeAt: String?,
        unlockPrice: Int? = null,
        isPremiumUser: Boolean,
    ): String? {
        if (!isPremiumOnly(isPaid, freeAt, false)) return null
        if (isPremiumUser) return null
        val free = freeOpensAtLabel(freeAt)
        val price = unlockPrice?.takeIf { it > 0 }
        return buildString {
            append("Платная глава. Доступ с Premium")
            if (price != null) append(" или за $price монет (на сайте)")
            if (free != null) append(". Бесплатно с $free")
            append(".")
        }
    }
}

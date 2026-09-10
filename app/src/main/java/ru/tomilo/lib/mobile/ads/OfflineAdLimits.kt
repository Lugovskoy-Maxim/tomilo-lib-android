package ru.tomilo.lib.mobile.ads

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Лимиты офлайна без Premium: 1 rewarded = 1 кредит скачивания + пропуск на чтение.
 * Кредиты не копятся без потолка; рекламу нельзя крутить весь день.
 */
object OfflineAdLimits {
    const val MAX_STORED_CREDITS = 5
    const val MAX_REWARDED_PER_DAY = 8
    const val READ_PASS_MS = 45L * 60L * 1000L
    const val READ_PASS_MINUTES = 45

    val DAILY_CAP_MESSAGE =
        "Сегодня лимит рекламы исчерпан ($MAX_REWARDED_PER_DAY/день). Завтра или Premium."

    private val dayFmt = DateTimeFormatter.ISO_LOCAL_DATE

    fun todayKey(
        now: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): String = Instant.ofEpochMilli(now).atZone(zone).toLocalDate().format(dayFmt)

    fun clampCredits(current: Int): Int = current.coerceIn(0, MAX_STORED_CREDITS)

    fun creditsAfterGrant(current: Int, amount: Int): Int =
        clampCredits(current + amount.coerceAtLeast(0))

    fun normalizedEarnedToday(earnedToday: Int, storedDay: String, today: String): Int =
        if (storedDay == today) earnedToday.coerceAtLeast(0) else 0

    fun rewardedRemaining(earnedToday: Int, storedDay: String, today: String): Int =
        (MAX_REWARDED_PER_DAY - normalizedEarnedToday(earnedToday, storedDay, today))
            .coerceAtLeast(0)

    fun canGrantRewarded(earnedToday: Int, storedDay: String, today: String): Boolean =
        rewardedRemaining(earnedToday, storedDay, today) > 0

    fun isReadPassActive(until: Long, now: Long = System.currentTimeMillis()): Boolean =
        until > now

    fun readPassUntil(now: Long = System.currentTimeMillis()): Long = now + READ_PASS_MS

    /** Реклама на чтение скачанного — только когда сеть есть. Без сети главы не блокируем. */
    fun requiresAdForOfflineRead(
        isPremium: Boolean,
        online: Boolean,
        hasReadPass: Boolean,
    ): Boolean = !isPremium && online && !hasReadPass
}

data class OfflineAdStatus(
    val credits: Int = 0,
    val dailyRemaining: Int = OfflineAdLimits.MAX_REWARDED_PER_DAY,
    val readUntil: Long = 0L,
) {
    fun hasReadPass(now: Long = System.currentTimeMillis()): Boolean =
        OfflineAdLimits.isReadPassActive(readUntil, now)
}

data class RewardGrant(
    val ok: Boolean,
    val creditsAdded: Int = 0,
    val creditsTotal: Int = 0,
    val dailyRemaining: Int = 0,
    val readUntil: Long = 0L,
    val reason: String? = null,
)

package ru.tomilo.lib.mobile.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ru.tomilo.lib.mobile.ads.OfflineAdLimits
import ru.tomilo.lib.mobile.ads.OfflineAdStatus
import ru.tomilo.lib.mobile.ads.RewardGrant

private val Context.adRewardDataStore by preferencesDataStore("tomilo_ad_rewards")

/**
 * Кредиты и пропуск офлайн-чтения за rewarded РСЯ.
 * 1 просмотр = 1 кредит скачивания (банк до [OfflineAdLimits.MAX_STORED_CREDITS])
 * и пропуск чтения на [OfflineAdLimits.READ_PASS_MINUTES] мин.
 * Не больше [OfflineAdLimits.MAX_REWARDED_PER_DAY] просмотров в календарный день.
 */
class AdRewardStore(private val context: Context) {
    private val offlineCreditsKey = intPreferencesKey("offline_credits")
    private val rewardedDayKey = stringPreferencesKey("rewarded_day")
    private val rewardedTodayKey = intPreferencesKey("rewarded_today")
    private val readPassUntilKey = longPreferencesKey("offline_read_until")

    val statusFlow: Flow<OfflineAdStatus> = context.adRewardDataStore.data.map { p ->
        val today = OfflineAdLimits.todayKey()
        OfflineAdStatus(
            credits = OfflineAdLimits.clampCredits(p[offlineCreditsKey] ?: 0),
            dailyRemaining = OfflineAdLimits.rewardedRemaining(
                earnedToday = p[rewardedTodayKey] ?: 0,
                storedDay = p[rewardedDayKey].orEmpty(),
                today = today,
            ),
            readUntil = p[readPassUntilKey] ?: 0L,
        )
    }

    val offlineCreditsFlow: Flow<Int> = statusFlow.map { it.credits }

    suspend fun status(): OfflineAdStatus = statusFlow.first()

    suspend fun offlineCredits(): Int = status().credits

    suspend fun canGrantRewarded(): Boolean = status().dailyRemaining > 0

    suspend fun hasOfflineReadAccess(now: Long = System.currentTimeMillis()): Boolean =
        status().hasReadPass(now)

    /**
     * Один завершённый rewarded-ролик. Сумма из РСЯ игнорируется: всегда 1 кредит,
     * чтобы кабинет не раздувал выдачу.
     */
    suspend fun grantRewarded(): RewardGrant {
        var result = RewardGrant(ok = false, reason = OfflineAdLimits.DAILY_CAP_MESSAGE)
        context.adRewardDataStore.edit { prefs ->
            val today = OfflineAdLimits.todayKey()
            val earned = OfflineAdLimits.normalizedEarnedToday(
                earnedToday = prefs[rewardedTodayKey] ?: 0,
                storedDay = prefs[rewardedDayKey].orEmpty(),
                today = today,
            )
            if (earned >= OfflineAdLimits.MAX_REWARDED_PER_DAY) {
                result = RewardGrant(
                    ok = false,
                    creditsTotal = OfflineAdLimits.clampCredits(prefs[offlineCreditsKey] ?: 0),
                    dailyRemaining = 0,
                    readUntil = prefs[readPassUntilKey] ?: 0L,
                    reason = OfflineAdLimits.DAILY_CAP_MESSAGE,
                )
                return@edit
            }
            val cur = OfflineAdLimits.clampCredits(prefs[offlineCreditsKey] ?: 0)
            val next = OfflineAdLimits.creditsAfterGrant(cur, 1)
            val until = OfflineAdLimits.readPassUntil()
            prefs[offlineCreditsKey] = next
            prefs[rewardedDayKey] = today
            prefs[rewardedTodayKey] = earned + 1
            prefs[readPassUntilKey] = until
            result = RewardGrant(
                ok = true,
                creditsAdded = next - cur,
                creditsTotal = next,
                dailyRemaining = OfflineAdLimits.MAX_REWARDED_PER_DAY - earned - 1,
                readUntil = until,
            )
        }
        return result
    }

    suspend fun addOfflineCredits(amount: Int) {
        if (amount <= 0) return
        context.adRewardDataStore.edit { prefs ->
            val cur = OfflineAdLimits.clampCredits(prefs[offlineCreditsKey] ?: 0)
            prefs[offlineCreditsKey] = OfflineAdLimits.creditsAfterGrant(cur, amount)
        }
    }

    /** @return true если кредит списан. */
    suspend fun tryConsumeOfflineCredit(): Boolean {
        var ok = false
        context.adRewardDataStore.edit { prefs ->
            val cur = OfflineAdLimits.clampCredits(prefs[offlineCreditsKey] ?: 0)
            if (cur > 0) {
                prefs[offlineCreditsKey] = cur - 1
                ok = true
            } else if (prefs[offlineCreditsKey] != null) {
                prefs[offlineCreditsKey] = 0
            }
        }
        return ok
    }

    suspend fun refundOfflineCredit() {
        addOfflineCredits(1)
    }
}

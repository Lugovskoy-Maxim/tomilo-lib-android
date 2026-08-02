package ru.tomilo.lib.mobile.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.adRewardDataStore by preferencesDataStore("tomilo_ad_rewards")

/**
 * Кредиты за просмотр rewarded-рекламы.
 * 1 Reward из РСЯ = 1 кредит на скачивание 1 главы офлайн без Premium.
 */
class AdRewardStore(private val context: Context) {
    private val offlineCreditsKey = intPreferencesKey("offline_credits")

    val offlineCreditsFlow: Flow<Int> = context.adRewardDataStore.data.map { p ->
        (p[offlineCreditsKey] ?: 0).coerceAtLeast(0)
    }

    suspend fun offlineCredits(): Int = offlineCreditsFlow.first()

    suspend fun addOfflineCredits(amount: Int) {
        if (amount <= 0) return
        context.adRewardDataStore.edit { prefs ->
            val cur = prefs[offlineCreditsKey] ?: 0
            prefs[offlineCreditsKey] = (cur + amount).coerceAtMost(99)
        }
    }

    /** @return true если кредит списан. */
    suspend fun tryConsumeOfflineCredit(): Boolean {
        var ok = false
        context.adRewardDataStore.edit { prefs ->
            val cur = prefs[offlineCreditsKey] ?: 0
            if (cur > 0) {
                prefs[offlineCreditsKey] = cur - 1
                ok = true
            }
        }
        return ok
    }

    suspend fun refundOfflineCredit() {
        addOfflineCredits(1)
    }
}

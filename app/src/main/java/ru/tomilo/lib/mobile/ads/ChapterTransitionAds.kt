package ru.tomilo.lib.mobile.ads

import android.app.Activity
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.tomilo.lib.mobile.core.Premium
import ru.tomilo.lib.mobile.data.api.UserDto
import ru.tomilo.lib.mobile.data.local.AdFrequencyStore

/**
 * Реклама при переходе между главами: не чаще 1 раза в 10 минут.
 * Premium — без рекламы. Rewarded показывается отдельно, только по явному
 * действию пользователя в сценариях офлайн-доступа.
 */
class ChapterTransitionAds(
    private val frequencyStore: AdFrequencyStore,
    private val interstitialAdManager: InterstitialAdManager,
    private val scope: CoroutineScope,
) {
    /**
     * [proceed] — открыть целевую главу (всегда вызывается).
     */
    fun maybeShowThen(
        activity: Activity?,
        user: UserDto?,
        proceed: () -> Unit,
    ) {
        if (activity == null || activity.isFinishing) {
            proceed()
            return
        }
        if (Premium.isActive(user?.subscriptionExpiresAt)) {
            proceed()
            return
        }

        scope.launch {
            if (!shouldPrompt(user, alreadyCheckedPremium = true)) {
                withContext(Dispatchers.Main) { proceed() }
                return@launch
            }
            withContext(Dispatchers.Main) {
                when {
                    interstitialAdManager.isReady -> {
                        Log.i(TAG, "Show ready interstitial between chapters")
                        interstitialAdManager.show(activity) { shown ->
                            if (shown) scope.launch { frequencyStore.markInterChapterShown() }
                            proceed()
                        }
                    }
                    else -> {
                        // Объявление не готово — не блокируем чтение и подготавливаем следующее.
                        interstitialAdManager.preload()
                        proceed()
                    }
                }
            }
        }
    }

    /** Показываем только готовый interstitial, не задерживая переход к главе. */
    suspend fun shouldPrompt(user: UserDto?, alreadyCheckedPremium: Boolean = false): Boolean {
        if (!alreadyCheckedPremium && Premium.isActive(user?.subscriptionExpiresAt)) return false
        if (!frequencyStore.canShowInterChapter()) return false
        return interstitialAdManager.isReady
    }

    companion object {
        private const val TAG = "ChapterTransitionAds"
    }
}

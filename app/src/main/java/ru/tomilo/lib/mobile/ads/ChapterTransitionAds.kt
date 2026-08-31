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
import ru.tomilo.lib.mobile.data.local.AdRewardStore

/**
 * Реклама при переходе между главами: не чаще 1 раза в 10 минут.
 * Premium — без рекламы. Приоритет: interstitial → fallback rewarded (кредит офлайн).
 */
class ChapterTransitionAds(
    private val frequencyStore: AdFrequencyStore,
    private val interstitialAdManager: InterstitialAdManager,
    private val rewardedAdManager: RewardedAdManager,
    private val adRewardStore: AdRewardStore,
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
            val canShow = frequencyStore.canShowInterChapter()
            if (!canShow) {
                withContext(Dispatchers.Main) { proceed() }
                return@launch
            }

            withContext(Dispatchers.Main) {
                when {
                    interstitialAdManager.enabled -> {
                        Log.i(TAG, "Wait briefly for interstitial between chapters")
                        interstitialAdManager.showWhenReady(activity) { shown ->
                            if (shown) scope.launch { frequencyStore.markInterChapterShown() }
                            proceed()
                        }
                    }
                    // Fallback: rewarded-блок, если interstitial unit ещё не создан
                    rewardedAdManager.isReady -> {
                        Log.i(TAG, "Show rewarded fallback between chapters")
                        var finished = false
                        fun done() {
                            if (finished) return
                            finished = true
                            scope.launch { frequencyStore.markInterChapterShown() }
                            proceed()
                        }
                        rewardedAdManager.show(
                            activity = activity,
                            onRewarded = { amount, _ ->
                                scope.launch {
                                    adRewardStore.addOfflineCredits(amount.coerceAtLeast(1))
                                }
                            },
                            onFailed = { done() },
                            onDismissed = { done() },
                        )
                    }
                    else -> {
                        // реклама не готова — не блокируем чтение, кулдаун не ставим
                        interstitialAdManager.preload()
                        rewardedAdManager.preload()
                        proceed()
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "ChapterTransitionAds"
    }
}

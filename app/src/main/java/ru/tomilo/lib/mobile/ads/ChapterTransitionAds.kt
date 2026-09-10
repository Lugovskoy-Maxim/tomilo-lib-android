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
 * Premium — без рекламы. Приоритет: interstitial → fallback rewarded
 * (1 кредит + пропуск офлайн-чтения, с дневным лимитом).
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
            if (!shouldPrompt(user, alreadyCheckedPremium = true)) {
                withContext(Dispatchers.Main) { proceed() }
                return@launch
            }
            val canReward = adRewardStore.canGrantRewarded()

            withContext(Dispatchers.Main) {
                when {
                    interstitialAdManager.enabled -> {
                        Log.i(TAG, "Wait briefly for interstitial between chapters")
                        interstitialAdManager.showWhenReady(activity) { shown ->
                            if (shown) scope.launch { frequencyStore.markInterChapterShown() }
                            proceed()
                        }
                    }
                    // Fallback: rewarded, если interstitial unit ещё не создан
                    rewardedAdManager.isReady && canReward -> {
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
                            onRewarded = { _, _ ->
                                scope.launch { adRewardStore.grantRewarded() }
                            },
                            onFailed = { done() },
                            onDismissed = { done() },
                        )
                    }
                    else -> {
                        // реклама не готова / дневной лимит — не блокируем чтение
                        interstitialAdManager.preload()
                        rewardedAdManager.preload()
                        proceed()
                    }
                }
            }
        }
    }

    /**
     * Будет ли попытка показать рекламу. Таймер 5–1 только в этом случае.
     */
    suspend fun shouldPrompt(user: UserDto?, alreadyCheckedPremium: Boolean = false): Boolean {
        if (!alreadyCheckedPremium && Premium.isActive(user?.subscriptionExpiresAt)) return false
        if (!frequencyStore.canShowInterChapter()) return false
        if (interstitialAdManager.enabled) return true
        return rewardedAdManager.isReady && adRewardStore.canGrantRewarded()
    }

    companion object {
        private const val TAG = "ChapterTransitionAds"
        const val COUNTDOWN_SECONDS = 5

        fun countdownTicks(): IntProgression = COUNTDOWN_SECONDS downTo 1
    }
}

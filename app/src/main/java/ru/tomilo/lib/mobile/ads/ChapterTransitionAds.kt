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
 * Premium — без рекламы. Если блок включён, ждём загрузку interstitial
 * (отсчёт в читалке + [InterstitialAdManager.showWhenReady]), а не
 * пропускаем показ, когда объявление ещё не готово.
 * Rewarded показывается отдельно, только по явному действию в офлайне.
 */
class ChapterTransitionAds(
    private val frequencyStore: AdFrequencyStore,
    private val interstitialAdManager: InterstitialAdManager,
    private val scope: CoroutineScope,
) {
    /** Начать загрузку заранее, чтобы отсчёт 5 с не пропал впустую. */
    fun prepare() {
        interstitialAdManager.preload()
    }

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
            if (!shouldPrompt(user)) {
                withContext(Dispatchers.Main) { proceed() }
                return@launch
            }
            withContext(Dispatchers.Main) {
                if (interstitialAdManager.enabled) {
                    Log.i(TAG, "Wait for interstitial between chapters")
                    interstitialAdManager.showWhenReady(activity) { shown ->
                        if (shown) scope.launch { frequencyStore.markInterChapterShown() }
                        proceed()
                    }
                } else {
                    interstitialAdManager.preload()
                    proceed()
                }
            }
        }
    }

    /**
     * Будет ли попытка показать рекламу. Таймер 5–1 только в этом случае.
     * Готовность креатива здесь не требуется: её дожидается [maybeShowThen].
     */
    suspend fun shouldPrompt(user: UserDto?): Boolean {
        return ChapterAdPolicy.shouldAttempt(
            premium = Premium.isActive(user?.subscriptionExpiresAt),
            cooldownElapsed = frequencyStore.canShowInterChapter(),
            interstitialEnabled = interstitialAdManager.enabled,
        )
    }

    companion object {
        private const val TAG = "ChapterTransitionAds"
        const val COUNTDOWN_SECONDS = ChapterAdPolicy.COUNTDOWN_SECONDS

        fun countdownTicks(): IntProgression = ChapterAdPolicy.countdownTicks()
    }
}

package ru.tomilo.lib.mobile.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.common.YandexAds
import com.yandex.mobile.ads.rewarded.Reward
import com.yandex.mobile.ads.rewarded.RewardedAd
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoader
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Rewarded РСЯ: оба блока кабинета грузятся заранее и независимо.
 * R-M-19689456-1 и R-M-19689456-3. Показ берёт уже готовый креатив,
 * второй остаётся в кеше на следующий явный просмотр (скачивание или офлайн).
 * Между главами rewarded не показывается.
 * Награда из кабинета: валюта Reward, сумма 1 → 1 офлайн-кредит главы.
 */
class RewardedAdManager(
    appContext: Context,
    adUnitIds: List<String> = AdUnits.rewardedUnits,
) {
    private val appContext = appContext.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val slots = adUnitIds
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .map { Slot(it) }
    private val sdkReady = AtomicBoolean(false)
    private val adsAllowed = AtomicBoolean(false)
    private val personalized = AtomicBoolean(true)

    @Volatile
    var isReady: Boolean = false
        private set

    /**
     * Передаёт персонализацию в рекламный SDK: true — персонализированные
     * объявления, false — только контекстные. Вызывать до [initialize];
     * при живом SDK обновляет согласие для следующих запросов.
     */
    fun setPersonalized(personalizedAds: Boolean) {
        personalized.set(personalizedAds)
        if (sdkReady.get()) {
            mainHandler.post { YandexAds.setUserConsent(personalizedAds) }
        }
    }

    fun initialize(onReady: (() -> Unit)? = null) {
        if (!adsAllowed.get() || slots.isEmpty()) {
            onReady?.invoke()
            return
        }
        if (sdkReady.get()) {
            onReady?.invoke()
            return
        }
        mainHandler.post { YandexAds.setUserConsent(personalized.get()) }
        YandexAdsSdk.initialize(appContext) {
            if (!adsAllowed.get()) {
                onReady?.invoke()
                return@initialize
            }
            sdkReady.set(true)
            preload()
            onReady?.invoke()
            Log.i(TAG, "Yandex Mobile Ads SDK ready, units=${slots.joinToString { it.unitId }}")
        }
    }

    /** Грузит каждый блок, у которого ещё нет креатива. Повторные вызовы не сбрасывают готовый кеш. */
    fun preload() {
        if (!adsAllowed.get() || slots.isEmpty() || !sdkReady.get()) return
        slots.forEach { slot ->
            slot.retryRunnable?.let(mainHandler::removeCallbacks)
            slot.retryRunnable = null
        }
        slots.forEach(::loadSlot)
    }

    /** Premium completely disables requests, cached ads and future shows. */
    fun setAdsAllowed(allowed: Boolean) {
        val changed = adsAllowed.getAndSet(allowed) != allowed
        if (!allowed) {
            destroy()
        } else if (changed || !sdkReady.get()) {
            if (sdkReady.get()) preload() else initialize()
        }
    }

    /**
     * Показать rewarded. Если кеш пуст, ждёт загрузку, а не сразу отвечает ошибкой.
     * [onRewarded] — после полного просмотра (amount/type из РСЯ).
     * Вызывать с UI-потока; [activity] не finishing.
     */
    fun show(
        activity: Activity,
        onRewarded: (amount: Int, type: String) -> Unit,
        onFailed: (message: String) -> Unit,
        onDismissed: () -> Unit = {},
    ) {
        mainHandler.post {
            if (!adsAllowed.get() || slots.isEmpty()) {
                onFailed("Реклама недоступна")
                return@post
            }
            if (activity.isFinishing || activity.isDestroyed) {
                onFailed("Экран недоступен")
                return@post
            }
            if (!sdkReady.get()) {
                initialize {
                    awaitShow(
                        activity = activity,
                        deadlineMs = System.currentTimeMillis() + SHOW_WAIT_MS,
                        kick = true,
                        onRewarded = onRewarded,
                        onFailed = onFailed,
                        onDismissed = onDismissed,
                    )
                }
                return@post
            }
            awaitShow(
                activity = activity,
                deadlineMs = System.currentTimeMillis() + SHOW_WAIT_MS,
                kick = true,
                onRewarded = onRewarded,
                onFailed = onFailed,
                onDismissed = onDismissed,
            )
        }
    }

    fun destroy() {
        slots.forEach { slot ->
            slot.retryRunnable?.let(mainHandler::removeCallbacks)
            slot.retryRunnable = null
        }
        isReady = false
        mainHandler.post {
            slots.forEach { slot ->
                slot.loadedAd?.setAdEventListener(null)
                slot.loadedAd = null
                slot.loader?.cancelLoading()
                slot.loader = null
                slot.loading.set(false)
            }
            isReady = false
        }
    }

    private fun awaitShow(
        activity: Activity,
        deadlineMs: Long,
        kick: Boolean,
        onRewarded: (amount: Int, type: String) -> Unit,
        onFailed: (message: String) -> Unit,
        onDismissed: () -> Unit,
    ) {
        if (!adsAllowed.get() || slots.isEmpty()) {
            onFailed("Реклама недоступна")
            return
        }
        if (activity.isFinishing || activity.isDestroyed) {
            onFailed("Экран недоступен")
            return
        }
        val slot = slots.firstOrNull { it.loadedAd != null }
        if (slot != null) {
            present(activity, slot, onRewarded, onFailed, onDismissed)
            return
        }
        if (kick) preload()
        if (System.currentTimeMillis() >= deadlineMs) {
            onFailed("Не удалось загрузить рекламу. Проверьте интернет и попробуйте ещё раз.")
            return
        }
        mainHandler.postDelayed(
            {
                awaitShow(
                    activity = activity,
                    deadlineMs = deadlineMs,
                    kick = false,
                    onRewarded = onRewarded,
                    onFailed = onFailed,
                    onDismissed = onDismissed,
                )
            },
            AD_POLL_INTERVAL_MS,
        )
    }

    private fun present(
        activity: Activity,
        slot: Slot,
        onRewarded: (amount: Int, type: String) -> Unit,
        onFailed: (message: String) -> Unit,
        onDismissed: () -> Unit,
    ) {
        val ad = slot.loadedAd
        if (ad == null || !adsAllowed.get()) {
            onFailed("Реклама недоступна")
            return
        }
        slot.loadedAd = null
        refreshReady()
        var rewarded = false
        ad.setAdEventListener(
            object : RewardedAdEventListener {
                override fun onAdShown() {
                    Log.i(TAG, "Rewarded shown unit=${slot.unitId}")
                }

                override fun onAdFailedToShow(adError: AdError) {
                    ad.setAdEventListener(null)
                    onFailed(adError.description ?: "Не удалось показать рекламу")
                    loadSlot(slot)
                }

                override fun onAdDismissed() {
                    ad.setAdEventListener(null)
                    if (!rewarded) {
                        // закрыл досрочно — без награды
                    }
                    onDismissed()
                    loadSlot(slot)
                }

                override fun onAdClicked() = Unit

                override fun onAdImpression(impressionData: ImpressionData?) = Unit

                override fun onRewarded(reward: Reward) {
                    rewarded = true
                    val amount = reward.amount.coerceAtLeast(1)
                    val type = reward.type.ifBlank { "Reward" }
                    onRewarded(amount, type)
                }
            },
        )
        ad.show(activity)
    }

    private fun loadSlot(slot: Slot) {
        if (!adsAllowed.get() || !sdkReady.get()) return
        if (slot.loadedAd != null || slot.loading.get()) return
        mainHandler.post {
            if (!adsAllowed.get() || slot.loadedAd != null || slot.loading.get()) return@post
            if (slot.loader == null) slot.loader = RewardedAdLoader(appContext)
            slot.loading.set(true)
            val request = AdRequest.Builder(slot.unitId).build()
            slot.loader?.loadAd(
                request,
                object : RewardedAdLoadListener {
                    override fun onAdLoaded(ad: RewardedAd) {
                        slot.loading.set(false)
                        if (!adsAllowed.get()) {
                            ad.setAdEventListener(null)
                            return
                        }
                        slot.loadedAd = ad
                        slot.retryAttempt = 0
                        slot.retryRunnable?.let(mainHandler::removeCallbacks)
                        slot.retryRunnable = null
                        refreshReady()
                        Log.i(TAG, "Rewarded loaded unit=${slot.unitId}")
                    }

                    override fun onAdFailedToLoad(error: AdRequestError) {
                        slot.loading.set(false)
                        slot.loadedAd = null
                        refreshReady()
                        Log.w(TAG, "Rewarded failed unit=${slot.unitId}: ${error.code} ${error.description}")
                        scheduleRetry(slot)
                    }
                },
            )
        }
    }

    private fun scheduleRetry(slot: Slot) {
        if (!adsAllowed.get() || slot.retryRunnable != null) return
        val delay = (RETRY_BASE_MS * (1L shl slot.retryAttempt.coerceAtMost(4))).coerceAtMost(RETRY_MAX_MS)
        slot.retryAttempt = (slot.retryAttempt + 1).coerceAtMost(5)
        slot.retryRunnable = Runnable {
            slot.retryRunnable = null
            loadSlot(slot)
        }.also { mainHandler.postDelayed(it, delay) }
    }

    private fun refreshReady() {
        isReady = slots.any { it.loadedAd != null }
    }

    private class Slot(val unitId: String) {
        var loader: RewardedAdLoader? = null
        var loadedAd: RewardedAd? = null
        val loading = AtomicBoolean(false)
        var retryAttempt = 0
        var retryRunnable: Runnable? = null
    }

    companion object {
        private const val TAG = "TomiloRewarded"
        private const val SHOW_WAIT_MS = 8_000L
        private const val AD_POLL_INTERVAL_MS = 200L
        private const val RETRY_BASE_MS = 5_000L
        private const val RETRY_MAX_MS = 60_000L
    }
}

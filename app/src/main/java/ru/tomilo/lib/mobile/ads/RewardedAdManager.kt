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
import com.yandex.mobile.ads.common.InitializationListener
import com.yandex.mobile.ads.common.YandexAds
import com.yandex.mobile.ads.rewarded.Reward
import com.yandex.mobile.ads.rewarded.RewardedAd
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoader
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Rewarded РСЯ (R-M-19689456-1).
 * Награда из кабинета: валюта Reward, сумма 1 → 1 офлайн-кредит главы.
 */
class RewardedAdManager(
    appContext: Context,
    private val adUnitId: String = AdUnits.rewarded,
) {
    private val appContext = appContext.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var loader: RewardedAdLoader? = null
    private var loadedAd: RewardedAd? = null
    private val sdkReady = AtomicBoolean(false)
    private val loading = AtomicBoolean(false)

    @Volatile
    var isReady: Boolean = false
        private set

    fun initialize(onReady: (() -> Unit)? = null) {
        mainHandler.post {
            YandexAds.initialize(
                appContext,
                InitializationListener {
                    sdkReady.set(true)
                    ensureLoader()
                    preload()
                    onReady?.invoke()
                    Log.i(TAG, "Yandex Mobile Ads SDK ready, unit=$adUnitId")
                },
            )
        }
    }

    private fun ensureLoader() {
        if (loader == null) {
            loader = RewardedAdLoader(appContext)
        }
    }

    fun preload() {
        if (!sdkReady.get()) return
        if (loadedAd != null || loading.get()) return
        mainHandler.post {
            if (loadedAd != null || loading.get()) return@post
            ensureLoader()
            loading.set(true)
            isReady = false
            val request = AdRequest.Builder(adUnitId).build()
            loader?.loadAd(
                request,
                object : RewardedAdLoadListener {
                    override fun onAdLoaded(ad: RewardedAd) {
                        loadedAd = ad
                        loading.set(false)
                        isReady = true
                        Log.i(TAG, "Rewarded loaded")
                    }

                    override fun onAdFailedToLoad(error: AdRequestError) {
                        loading.set(false)
                        isReady = false
                        loadedAd = null
                        Log.w(TAG, "Rewarded failed: ${error.code} ${error.description}")
                    }
                },
            )
        }
    }

    /**
     * Показать rewarded. [onRewarded] — после полного просмотра (amount/type из РСЯ).
     * Вызывать с UI-потока; [activity] не finishing.
     */
    fun show(
        activity: Activity,
        onRewarded: (amount: Int, type: String) -> Unit,
        onFailed: (message: String) -> Unit,
        onDismissed: () -> Unit = {},
    ) {
        mainHandler.post {
            if (activity.isFinishing) {
                onFailed("Экран недоступен")
                return@post
            }
            val ad = loadedAd
            if (ad == null) {
                preload()
                onFailed("Реклама ещё загружается — попробуйте через пару секунд")
                return@post
            }
            loadedAd = null
            isReady = false

            var rewarded = false
            ad.setAdEventListener(
                object : RewardedAdEventListener {
                    override fun onAdShown() = Unit

                    override fun onAdFailedToShow(adError: AdError) {
                        ad.setAdEventListener(null)
                        onFailed(adError.description ?: "Не удалось показать рекламу")
                        preload()
                    }

                    override fun onAdDismissed() {
                        ad.setAdEventListener(null)
                        if (!rewarded) {
                            // закрыл досрочно — без награды
                        }
                        onDismissed()
                        preload()
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
    }

    fun destroy() {
        mainHandler.post {
            loadedAd?.setAdEventListener(null)
            loadedAd = null
            loader?.cancelLoading()
            loader = null
            isReady = false
            loading.set(false)
        }
    }

    companion object {
        private const val TAG = "TomiloRewarded"
    }
}

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
import com.yandex.mobile.ads.interstitial.InterstitialAd
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Межстраничная (interstitial) РСЯ — показ между главами.
 * Unit: [AdUnits.interstitial] (если пусто в release — менеджер неактивен).
 */
class InterstitialAdManager(
    appContext: Context,
    private val adUnitId: String = AdUnits.interstitial,
) {
    private val appContext = appContext.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var loader: InterstitialAdLoader? = null
    private var loadedAd: InterstitialAd? = null
    private var retryAttempt = 0
    private var retryRunnable: Runnable? = null
    private val sdkReady = AtomicBoolean(false)
    private val loading = AtomicBoolean(false)

    val enabled: Boolean get() = adUnitId.isNotBlank()

    @Volatile
    var isReady: Boolean = false
        private set

    fun initialize(onReady: (() -> Unit)? = null) {
        if (!enabled) {
            Log.i(TAG, "Interstitial disabled (empty ad unit)")
            onReady?.invoke()
            return
        }
        mainHandler.post {
            YandexAds.initialize(
                appContext,
                InitializationListener {
                    sdkReady.set(true)
                    ensureLoader()
                    preload()
                    onReady?.invoke()
                    Log.i(TAG, "Interstitial ready, unit=$adUnitId")
                },
            )
        }
    }

    private fun ensureLoader() {
        if (loader == null) loader = InterstitialAdLoader(appContext)
    }

    fun preload() {
        if (!enabled || !sdkReady.get()) return
        if (loadedAd != null || loading.get()) return
        mainHandler.post {
            if (loadedAd != null || loading.get()) return@post
            ensureLoader()
            loading.set(true)
            isReady = false
            val request = AdRequest.Builder(adUnitId).build()
            loader?.loadAd(
                request,
                object : InterstitialAdLoadListener {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        loadedAd = ad
                        loading.set(false)
                        isReady = true
                        retryAttempt = 0
                        retryRunnable?.let(mainHandler::removeCallbacks)
                        retryRunnable = null
                        Log.i(TAG, "Interstitial loaded")
                    }

                    override fun onAdFailedToLoad(error: AdRequestError) {
                        loading.set(false)
                        isReady = false
                        loadedAd = null
                        Log.w(TAG, "Interstitial fail: ${error.code} ${error.description}")
                        scheduleRetry()
                    }
                },
            )
        }
    }

    /**
     * Первый переход часто случается раньше, чем SDK успевает получить
     * interstitial. Ждём короткое время вместо молчаливого пропуска рекламы.
     * [onFinished] получает true, только если был готовый рекламный объект.
     */
    fun showWhenReady(
        activity: Activity,
        maxWaitMs: Long = INITIAL_AD_WAIT_MS,
        onFinished: (shown: Boolean) -> Unit,
    ) {
        mainHandler.post {
            if (!enabled || activity.isFinishing) {
                onFinished(false)
                return@post
            }
            val deadline = System.currentTimeMillis() + maxWaitMs
            fun waitForAd() {
                if (activity.isFinishing) {
                    onFinished(false)
                    return
                }
                if (loadedAd != null) {
                    show(activity) { onFinished(true) }
                    return
                }
                preload()
                if (System.currentTimeMillis() >= deadline) {
                    onFinished(false)
                } else {
                    mainHandler.postDelayed({ waitForAd() }, AD_POLL_INTERVAL_MS)
                }
            }
            waitForAd()
        }
    }

    private fun scheduleRetry() {
        if (!enabled || retryRunnable != null) return
        val delay = (RETRY_BASE_MS * (1L shl retryAttempt.coerceAtMost(4))).coerceAtMost(RETRY_MAX_MS)
        retryAttempt = (retryAttempt + 1).coerceAtMost(5)
        retryRunnable = Runnable {
            retryRunnable = null
            preload()
        }.also { mainHandler.postDelayed(it, delay) }
    }

    /**
     * Показать interstitial. После dismiss/fail всегда вызывается [onFinished]
     * (переход к следующей главе не блокируется).
     */
    fun show(
        activity: Activity,
        onFinished: () -> Unit,
    ) {
        mainHandler.post {
            if (!enabled || activity.isFinishing) {
                onFinished()
                return@post
            }
            val ad = loadedAd
            if (ad == null) {
                preload()
                onFinished()
                return@post
            }
            loadedAd = null
            isReady = false

            var finished = false
            fun done() {
                if (finished) return
                finished = true
                onFinished()
            }

            ad.setAdEventListener(
                object : InterstitialAdEventListener {
                    override fun onAdShown() = Unit

                    override fun onAdFailedToShow(adError: AdError) {
                        ad.setAdEventListener(null)
                        Log.w(TAG, "show fail: ${adError.description}")
                        done()
                        preload()
                    }

                    override fun onAdDismissed() {
                        ad.setAdEventListener(null)
                        done()
                        preload()
                    }

                    override fun onAdClicked() = Unit

                    override fun onAdImpression(impressionData: ImpressionData?) = Unit
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
            retryRunnable?.let(mainHandler::removeCallbacks)
            retryRunnable = null
            isReady = false
            loading.set(false)
        }
    }

    companion object {
        private const val TAG = "TomiloInterstitial"
        private const val INITIAL_AD_WAIT_MS = 2_000L
        private const val AD_POLL_INTERVAL_MS = 200L
        private const val RETRY_BASE_MS = 5_000L
        private const val RETRY_MAX_MS = 60_000L
    }
}

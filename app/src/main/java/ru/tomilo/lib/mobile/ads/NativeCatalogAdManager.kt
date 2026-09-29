package ru.tomilo.lib.mobile.ads

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.nativeads.NativeAd
import com.yandex.mobile.ads.nativeads.NativeAdLoadListener
import com.yandex.mobile.ads.nativeads.NativeAdLoader
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Нативный блок каталога R-M-19689456-4.
 * Запрос уходит при разрешённой рекламе, до того как пользователь долистает до карточки.
 * Повтор при ошибке. Premium сбрасывает кеш и запрещает новые запросы.
 */
class NativeCatalogAdManager(
    appContext: Context,
    private val adUnitId: String = AdUnits.nativeCatalog,
) {
    private val appContext = appContext.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val sdkReady = AtomicBoolean(false)
    private val loading = AtomicBoolean(false)
    private val adsAllowed = AtomicBoolean(false)
    private var loader: NativeAdLoader? = null
    private var retryAttempt = 0
    private var retryRunnable: Runnable? = null
    private val _nativeAd = MutableStateFlow<NativeAd?>(null)
    val nativeAd: StateFlow<NativeAd?> = _nativeAd.asStateFlow()

    fun initialize(onReady: (() -> Unit)? = null) {
        if (!adsAllowed.get() || adUnitId.isBlank()) {
            onReady?.invoke()
            return
        }
        if (sdkReady.get()) {
            onReady?.invoke()
            return
        }
        YandexAdsSdk.initialize(appContext) {
            if (!adsAllowed.get()) {
                onReady?.invoke()
                return@initialize
            }
            sdkReady.set(true)
            preload()
            onReady?.invoke()
            Log.i(TAG, "Native catalog ready, unit=$adUnitId")
        }
    }

    fun preload() {
        if (!adsAllowed.get() || adUnitId.isBlank() || !sdkReady.get()) return
        retryRunnable?.let(mainHandler::removeCallbacks)
        retryRunnable = null
        if (_nativeAd.value != null || loading.get()) return
        mainHandler.post {
            if (!adsAllowed.get() || _nativeAd.value != null || loading.get()) return@post
            if (loader == null) loader = NativeAdLoader(appContext)
            loading.set(true)
            loader?.loadAd(
                AdRequest.Builder(adUnitId).build(),
                object : NativeAdLoadListener {
                    override fun onAdLoaded(ad: NativeAd) {
                        loading.set(false)
                        if (!adsAllowed.get()) {
                            ad.setNativeAdEventListener(null)
                            return
                        }
                        _nativeAd.value?.setNativeAdEventListener(null)
                        _nativeAd.value = ad
                        retryAttempt = 0
                        retryRunnable?.let(mainHandler::removeCallbacks)
                        retryRunnable = null
                        Log.i(TAG, "Native catalog loaded unit=$adUnitId")
                    }

                    override fun onAdFailedToLoad(error: AdRequestError) {
                        loading.set(false)
                        Log.w(TAG, "Native catalog failed unit=$adUnitId: ${error.code} ${error.description}")
                        scheduleRetry()
                    }
                },
            )
        }
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

    fun destroy() {
        retryRunnable?.let(mainHandler::removeCallbacks)
        retryRunnable = null
        mainHandler.post {
            loader?.cancelLoading()
            loader = null
            _nativeAd.value?.setNativeAdEventListener(null)
            _nativeAd.value = null
            loading.set(false)
        }
    }

    private fun scheduleRetry() {
        if (!adsAllowed.get() || adUnitId.isBlank() || retryRunnable != null) return
        val delay = (RETRY_BASE_MS * (1L shl retryAttempt.coerceAtMost(4))).coerceAtMost(RETRY_MAX_MS)
        retryAttempt = (retryAttempt + 1).coerceAtMost(5)
        retryRunnable = Runnable {
            retryRunnable = null
            preload()
        }.also { mainHandler.postDelayed(it, delay) }
    }

    companion object {
        private const val TAG = "TomiloNativeCatalog"
        private const val RETRY_BASE_MS = 5_000L
        private const val RETRY_MAX_MS = 60_000L
    }
}

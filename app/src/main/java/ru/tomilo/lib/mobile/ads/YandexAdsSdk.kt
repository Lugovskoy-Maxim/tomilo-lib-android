package ru.tomilo.lib.mobile.ads

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.yandex.mobile.ads.common.YandexAds
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Единая точка инициализации Yandex Mobile Ads SDK.
 *
 * SDK рассчитан на однократный `YandexAds.initialize`: при повторных вызовах
 * InitializationListener может не быть доставлен, из-за чего менеджер рекламы
 * оставался бы вечно неготовым («реклама не запускается вообще»). Здесь
 * single-flight: первый вызов стартует SDK, все остальные ждут того же колбэка.
 * Колбэки вызываются на главном потоке.
 */
object YandexAdsSdk {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val ready = AtomicBoolean(false)
    private val started = AtomicBoolean(false)

    @Volatile
    private var pending: List<() -> Unit> = emptyList()

    val isReady: Boolean get() = ready.get()

    fun initialize(context: Context, onReady: () -> Unit) {
        if (ready.get()) {
            onReady()
            return
        }
        synchronized(this) {
            pending = pending + onReady
        }
        if (started.compareAndSet(false, true)) {
            mainHandler.post {
                YandexAds.initialize(context.applicationContext) {
                    ready.set(true)
                    val callbacks = synchronized(this) {
                        val callbacks = pending
                        pending = emptyList()
                        callbacks
                    }
                    callbacks.forEach { it.invoke() }
                }
            }
        }
    }
}

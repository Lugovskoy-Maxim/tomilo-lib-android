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
    private val personalized = AtomicBoolean(true)

    @Volatile
    private var pending: List<() -> Unit> = emptyList()

    val isReady: Boolean get() = ready.get()

    fun setPersonalized(personalizedAds: Boolean) {
        val shouldApply = synchronized(this) {
            personalized.set(personalizedAds)
            ready.get()
        }
        if (shouldApply) {
            mainHandler.post {
                synchronized(this) {
                    if (ready.get()) YandexAds.setUserConsent(personalized.get())
                }
            }
        }
    }

    fun initialize(context: Context, onReady: () -> Unit) {
        val alreadyReady = synchronized(this) {
            if (ready.get()) true
            else {
                pending = pending + onReady
                false
            }
        }
        if (alreadyReady) {
            mainHandler.post(onReady)
            return
        }
        if (started.compareAndSet(false, true)) {
            mainHandler.post {
                YandexAds.setUserConsent(personalized.get())
                YandexAds.initialize(context.applicationContext) {
                    mainHandler.post {
                        val callbacks = synchronized(this) {
                            YandexAds.setUserConsent(personalized.get())
                            ready.set(true)
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
}

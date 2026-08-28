package ru.tomilo.lib.mobile

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import ru.rustore.sdk.pushclient.RuStorePushClient
import ru.rustore.sdk.pushclient.common.logger.DefaultLogger
import ru.tomilo.lib.mobile.data.api.NetworkModule
import ru.tomilo.lib.mobile.push.NotificationHelper
import ru.tomilo.lib.mobile.push.NotificationsPollWorker
import ru.tomilo.lib.mobile.push.PushTokenSync
import ru.tomilo.lib.mobile.core.networkAvailabilityFlow
import ru.tomilo.lib.mobile.data.update.AppUpdateCheckWorker
import ru.tomilo.lib.mobile.ui.components.RewardNotifications

class TomiloApp : Application(), ImageLoaderFactory {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Синхронно подтянуть токен до UI — иначе чаты/закладки уходят без Authorization
        runBlocking {
            TokenBridge.setCached(container.authStore.token())
        }
        // РСЯ: init + preload rewarded + interstitial (между главами)
        container.rewardedAdManager.initialize()
        container.interstitialAdManager.initialize()
        NotificationHelper.ensureChannel(this)
        if (BuildConfig.RUSTORE_PUSH_PROJECT_ID.isNotBlank()) {
            runCatching {
                RuStorePushClient.init(
                    application = this,
                    projectId = BuildConfig.RUSTORE_PUSH_PROJECT_ID,
                    logger = DefaultLogger(),
                )
            }
        }
        NotificationsPollWorker.schedule(this)
        AppUpdateCheckWorker.schedule(this)
        appScope.launch {
            container.authStore.tokenFlow.collectLatest { token ->
                TokenBridge.setCached(token)
                // Вход может завершиться, когда сеть уже подключена и новый
                // network callback не придёт. Запускаем подписку сразу по токену.
                if (!token.isNullOrBlank()) {
                    NotificationsPollWorker.schedule(this@TomiloApp)
                    PushTokenSync.syncIfNeeded(this@TomiloApp)
                }
            }
        }
        appScope.launch {
            applicationContext.networkAvailabilityFlow().collectLatest { online ->
                if (online && container.authRepository.isLoggedIn()) {
                    NotificationsPollWorker.enqueueNow(this@TomiloApp)
                    container.readingPrefs.pendingHistory().forEach { (titleId, chapterId) ->
                        container.historyRepository.markRead(titleId, chapterId)
                            .onSuccess { reward ->
                                container.readingPrefs.markHistorySynced(titleId, chapterId)
                                RewardNotifications.show(
                                    experience = reward.experienceGained,
                                    coins = reward.coinsGained,
                                    source = reward.reason ?: "Офлайн-глава синхронизирована",
                                )
                            }
                    }
                }
                if (online) AppUpdateCheckWorker.enqueueNow(this@TomiloApp)
            }
        }
        // Фоновый рефреш офлайн-каталогов (новые главы)
        appScope.launch {
            runCatching { container.offlineRepository.refreshStaleTitles() }
        }
    }

    override fun newImageLoader(): ImageLoader {
        val mediaClient: OkHttpClient = NetworkModule.createMediaClient(this)
        return ImageLoader.Builder(this)
            .okHttpClient(mediaClient)
            .components {
                // ImageDecoder сохраняет анимацию WebP/GIF на Android 9+.
                // На Android 8 используется совместимый GIF-декодер.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil_image_cache"))
                    .maxSizeBytes(250L * 1024L * 1024L)
                    .build()
            }
            .respectCacheHeaders(false)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .crossfade(true)
            .build()
    }
}

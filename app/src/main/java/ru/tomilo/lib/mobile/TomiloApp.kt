package ru.tomilo.lib.mobile

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
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
import ru.tomilo.lib.mobile.data.api.NetworkModule
import ru.tomilo.lib.mobile.push.NotificationHelper
import ru.tomilo.lib.mobile.push.NotificationsPollWorker

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
        // РСЯ: init + preload rewarded
        container.rewardedAdManager.initialize()
        NotificationHelper.ensureChannel(this)
        NotificationsPollWorker.schedule(this)
        appScope.launch {
            container.authStore.tokenFlow.collectLatest { token ->
                TokenBridge.setCached(token)
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

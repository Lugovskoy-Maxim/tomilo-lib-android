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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import ru.rustore.sdk.pushclient.RuStorePushClient
import ru.rustore.sdk.pushclient.common.logger.DefaultLogger
import ru.tomilo.lib.mobile.data.api.NetworkModule
import ru.tomilo.lib.mobile.push.NotificationHelper
import ru.tomilo.lib.mobile.push.NotificationsPollWorker
import ru.tomilo.lib.mobile.push.PushTokenSync
import ru.tomilo.lib.mobile.core.isNetworkAvailable
import ru.tomilo.lib.mobile.core.networkAvailabilityFlow
import ru.tomilo.lib.mobile.core.Premium
import ru.tomilo.lib.mobile.data.local.AdsConsent
import ru.tomilo.lib.mobile.data.update.AppUpdateCheckWorker
import ru.tomilo.lib.mobile.ui.components.RewardNotifications

class TomiloApp : Application(), ImageLoaderFactory {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val historySyncMutex = Mutex()

    private val _sessionReady = MutableStateFlow(false)
    /** Токены из DataStore уже в TokenBridge — можно ходить в API с Authorization. */
    val sessionReady: StateFlow<Boolean> = _sessionReady.asStateFlow()

    /**
     * Очередь сохраняется в DataStore, поэтому неудачные элементы останутся до
     * следующего подключения/запуска. Mutex не даёт событиям сети и входа
     * отправить одну и ту же главу параллельно.
     */
    private suspend fun syncPendingHistory() = historySyncMutex.withLock {
        if (!applicationContext.isNetworkAvailable() || !container.authRepository.isLoggedIn()) return@withLock

        var profileChanged = false
        container.readingPrefs.pendingHistory().forEach { (titleId, chapterId) ->
            container.historyRepository.markRead(titleId, chapterId)
                .onSuccess { reward ->
                    container.readingPrefs.markHistorySynced(titleId, chapterId)
                    profileChanged = profileChanged ||
                        reward.experienceGained != 0 || reward.coinsGained != 0
                    RewardNotifications.show(
                        experience = reward.experienceGained,
                        coins = reward.coinsGained,
                        source = reward.reason ?: "Офлайн-глава синхронизирована",
                    )
                }
        }
        if (profileChanged) container.authRepository.refreshProfile()
    }

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Не блокируем main thread: hydrate сессии и фоновые сервисы уходят в IO.
        // MainActivity ждёт sessionReady перед первым Navigation/API.
        appScope.launch {
            runCatching {
                container.authStore.encryptLegacySessionSecrets()
                TokenBridge.setCachedSession(
                    token = container.authStore.token(),
                    refreshToken = container.authStore.refreshToken(),
                )
                container.authStore.user()
            }.onSuccess { initialUser ->
                applyInitialAdsPolicy(initialUser?.subscriptionExpiresAt)
            }.onFailure {
                applyInitialAdsPolicy(null)
            }
            _sessionReady.value = true
            startPostSessionWork()
        }
        NotificationHelper.ensureChannel(this)
    }

    private fun applyInitialAdsPolicy(subscriptionExpiresAt: String?) {
        // У Premium реклама отключена полностью: SDK не запрашивает и не кеширует объявления.
        // Выбор согласия убран из приложения: без Premium реклама показывается
        // всегда, поэтому на старте принудительно ставим GRANTED (старое значение
        // DENIED/UNKNOWN/CONTEXTUAL перезаписывается).
        if (container.adsConsentStore.consent.value != AdsConsent.GRANTED) {
            container.adsConsentStore.set(AdsConsent.GRANTED)
        }
        val initialConsent = container.adsConsentStore.consent.value
        val adsAllowedInitially = initialConsent.allowsAds && !Premium.isActive(subscriptionExpiresAt)
        container.rewardedAdManager.setPersonalized(initialConsent.personalized)
        container.interstitialAdManager.setPersonalized(initialConsent.personalized)
        container.rewardedAdManager.setAdsAllowed(adsAllowedInitially)
        container.interstitialAdManager.setAdsAllowed(adsAllowedInitially)
        container.nativeCatalogAdManager.setAdsAllowed(adsAllowedInitially)
    }

    private fun startPostSessionWork() {
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
            container.authStore.userFlow
                .map { !Premium.isActive(it?.subscriptionExpiresAt) }
                .distinctUntilChanged()
                .collect { nonPremium ->
                    applyAdsConsent(container.adsConsentStore.consent.value, nonPremium)
                }
        }
        appScope.launch {
            container.adsConsentStore.consent.collect { consent ->
                val nonPremium = !Premium.isActive(container.authStore.user()?.subscriptionExpiresAt)
                applyAdsConsent(consent, nonPremium)
            }
        }
        appScope.launch {
            container.authStore.tokenFlow.distinctUntilChanged().collectLatest { token ->
                TokenBridge.setCached(token)
                // Вход может завершиться, когда сеть уже подключена и новый
                // network callback не придёт. Запускаем подписку сразу по токену.
                if (!token.isNullOrBlank()) {
                    NotificationsPollWorker.schedule(this@TomiloApp)
                    PushTokenSync.syncIfNeeded(this@TomiloApp)
                    // Вход может завершиться уже при активной сети, без
                    // нового connectivity callback.
                    syncPendingHistory()
                }
            }
        }
        appScope.launch {
            // collectLatest мог отменить HTTP-запрос при быстрой смене
            // network capabilities. Начатую синхронизацию доводим до конца.
            applicationContext.networkAvailabilityFlow().collect { online ->
                if (online && container.authRepository.isLoggedIn()) {
                    NotificationsPollWorker.enqueueNow(this@TomiloApp)
                    syncPendingHistory()
                }
                if (online) AppUpdateCheckWorker.enqueueNow(this@TomiloApp)
            }
        }
        // Фоновый рефреш офлайн-каталогов (новые главы)
        appScope.launch {
            runCatching { container.offlineRepository.refreshStaleTitles() }
        }
    }

    /**
     * Единая точка применения согласия: персонализация передаётся в SDK,
     * показ разрешён только при CONTEXTUAL/GRANTED и без активного Premium.
     */
    private fun applyAdsConsent(consent: AdsConsent, nonPremium: Boolean) {
        container.rewardedAdManager.setPersonalized(consent.personalized)
        container.interstitialAdManager.setPersonalized(consent.personalized)
        // Пауза рекламы на сайте (siteAdPause*) и настройки показа РСЯ сайта
        // мобильные блоки не выключают. Единственное исключение — активный Premium.
        val allowed = consent.allowsAds && nonPremium
        container.rewardedAdManager.setAdsAllowed(allowed)
        container.interstitialAdManager.setAdsAllowed(allowed)
        container.nativeCatalogAdManager.setAdsAllowed(allowed)
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
                    .maxSizePercent(0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil_image_cache"))
                    // OkHttp media cache уже 200 MiB — не раздуваем второй слой.
                    .maxSizeBytes(150L * 1024L * 1024L)
                    .build()
            }
            .respectCacheHeaders(true)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .crossfade(true)
            .build()
    }
}

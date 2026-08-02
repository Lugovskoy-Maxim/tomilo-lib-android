package ru.tomilo.lib.mobile

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import ru.tomilo.lib.mobile.ads.ChapterTransitionAds
import ru.tomilo.lib.mobile.ads.InterstitialAdManager
import ru.tomilo.lib.mobile.ads.RewardedAdManager
import ru.tomilo.lib.mobile.data.api.NetworkModule
import ru.tomilo.lib.mobile.data.download.DownloadManager
import ru.tomilo.lib.mobile.data.local.AdFrequencyStore
import ru.tomilo.lib.mobile.data.local.AdRewardStore
import ru.tomilo.lib.mobile.data.local.AuthStore
import ru.tomilo.lib.mobile.data.local.ContentPrefs
import ru.tomilo.lib.mobile.data.local.OfflineDatabase
import ru.tomilo.lib.mobile.data.local.ReadingPrefs
import ru.tomilo.lib.mobile.data.repo.AdminRepository
import ru.tomilo.lib.mobile.data.repo.AuthRepository
import ru.tomilo.lib.mobile.data.repo.CatalogRepository
import ru.tomilo.lib.mobile.data.repo.HistoryRepository
import ru.tomilo.lib.mobile.data.repo.OfflineRepository
import ru.tomilo.lib.mobile.data.repo.SocialRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val authStore = AuthStore(appContext)
    val readingPrefs = ReadingPrefs(appContext)
    val contentPrefs = ContentPrefs(appContext)
    val adRewardStore = AdRewardStore(appContext)
    val adFrequencyStore = AdFrequencyStore(appContext)
    val rewardedAdManager = RewardedAdManager(appContext)
    val interstitialAdManager = InterstitialAdManager(appContext)
    val chapterTransitionAds = ChapterTransitionAds(
        frequencyStore = adFrequencyStore,
        interstitialAdManager = interstitialAdManager,
        rewardedAdManager = rewardedAdManager,
        adRewardStore = adRewardStore,
        scope = appScope,
    )
    private val tokenHolder = TokenHolder()

    val tomiloApi = NetworkModule.createApi(appContext) {
        // tokenHolder может ещё не подтянуться с DataStore — fallback на store
        tokenHolder.token ?: TokenBridge.peekToken()
    }

    val authRepository = AuthRepository(tomiloApi, authStore)
    val catalogRepository = CatalogRepository(tomiloApi)
    val socialRepository = SocialRepository(tomiloApi)
    val historyRepository = HistoryRepository(tomiloApi)
    val adminRepository = AdminRepository(tomiloApi)
    private val offlineDb = OfflineDatabase.create(appContext)
    val offlineRepository = OfflineRepository(
        context = appContext,
        api = tomiloApi,
        dao = offlineDb.offlineDao(),
        authRepository = authRepository,
        adRewardStore = adRewardStore,
    )
    val downloadManager = DownloadManager(offlineRepository, appScope)

    init {
        TokenBridge.holder = tokenHolder
        TokenBridge.authStore = authStore
    }
}

class TokenHolder {
    @Volatile
    var token: String? = null
}

object TokenBridge {
    lateinit var holder: TokenHolder
    lateinit var authStore: AuthStore

    /** Синхронный peek: holder, затем (если уже инициализирован) — null (async only). */
    @Volatile
    private var cachedToken: String? = null

    fun setCached(token: String?) {
        cachedToken = token
        if (::holder.isInitialized) holder.token = token
    }

    fun peekToken(): String? {
        if (::holder.isInitialized) {
            holder.token?.let { return it }
        }
        return cachedToken
    }
}

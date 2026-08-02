package ru.tomilo.lib.mobile

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import ru.tomilo.lib.mobile.data.api.NetworkModule
import ru.tomilo.lib.mobile.data.download.DownloadManager
import ru.tomilo.lib.mobile.data.local.AuthStore
import ru.tomilo.lib.mobile.data.local.OfflineDatabase
import ru.tomilo.lib.mobile.data.local.ReadingPrefs
import ru.tomilo.lib.mobile.data.repo.AuthRepository
import ru.tomilo.lib.mobile.data.repo.CatalogRepository
import ru.tomilo.lib.mobile.data.repo.OfflineRepository
import ru.tomilo.lib.mobile.data.repo.SocialRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val authStore = AuthStore(appContext)
    val readingPrefs = ReadingPrefs(appContext)
    private val tokenHolder = TokenHolder()

    val tomiloApi = NetworkModule.createApi(appContext) { tokenHolder.token }

    val authRepository = AuthRepository(tomiloApi, authStore)
    val catalogRepository = CatalogRepository(tomiloApi)
    val socialRepository = SocialRepository(tomiloApi)
    private val offlineDb = OfflineDatabase.create(appContext)
    val offlineRepository = OfflineRepository(
        context = appContext,
        api = tomiloApi,
        dao = offlineDb.offlineDao(),
        authRepository = authRepository,
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
}

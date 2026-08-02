package ru.tomilo.lib.mobile

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.push.NotificationHelper
import ru.tomilo.lib.mobile.push.NotificationsPollWorker

class TomiloApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.ensureChannel(this)
        NotificationsPollWorker.schedule(this)
        appScope.launch {
            container.authStore.tokenFlow.collectLatest { token ->
                TokenBridge.holder.token = token
            }
        }
    }
}

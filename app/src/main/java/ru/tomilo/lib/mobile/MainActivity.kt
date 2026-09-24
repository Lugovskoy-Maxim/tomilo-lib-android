package ru.tomilo.lib.mobile

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import ru.tomilo.lib.mobile.push.NotificationHelper
import ru.tomilo.lib.mobile.push.NotificationOpen
import ru.tomilo.lib.mobile.push.NotificationsPollWorker
import ru.tomilo.lib.mobile.ui.navigation.TomiloNavHost
import ru.tomilo.lib.mobile.ui.theme.TomiloTheme
import ru.tomilo.lib.mobile.data.update.AppUpdateCheckWorker
import ru.tomilo.lib.mobile.rustore.RuStoreEngagement

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as TomiloApp
        handleNotificationIntent(intent)
        setContent {
            val themePrefs = app.container.themePrefs
            val accentHex by themePrefs.accentHexFlow.collectAsState(initial = null)
            val user by app.container.authStore.userFlow.collectAsState(initial = null)
            val isPremium = ru.tomilo.lib.mobile.core.Premium.isActive(user?.subscriptionExpiresAt)
            val activeAccent = remember(accentHex, isPremium) {
                if (isPremium && !accentHex.isNullOrBlank()) {
                    try {
                        Color(android.graphics.Color.parseColor(accentHex))
                    } catch (_: Exception) {
                        null
                    }
                } else null
            }
            TomiloTheme(accentColor = activeAccent) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TomiloNavHost(container = app.container)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        (application as? TomiloApp)?.container?.downloadManager?.resumePersisted()
        RuStoreEngagement.attach(this)
        NotificationsPollWorker.enqueueNow(this)
        AppUpdateCheckWorker.enqueueNow(this)
    }

    override fun onStop() {
        RuStoreEngagement.detach(this)
        super.onStop()
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent == null) return
        val openList = intent.getBooleanExtra(NotificationHelper.EXTRA_OPEN_LIST, false)
        val titleId = intent.getStringExtra(NotificationHelper.EXTRA_TITLE_ID)?.ifBlank { null }
        val chapterId = intent.getStringExtra(NotificationHelper.EXTRA_CHAPTER_ID)?.ifBlank { null }
        val link = intent.getStringExtra(NotificationHelper.EXTRA_LINK)?.ifBlank { null }
        val conversationId = intent.getStringExtra(NotificationHelper.EXTRA_CONVERSATION_ID)?.ifBlank { null }
        val conversationTitle = intent.getStringExtra(NotificationHelper.EXTRA_CONVERSATION_TITLE)?.ifBlank { null }
        if (!openList && titleId == null && chapterId == null && link == null && conversationId == null) return
        intent.removeExtra(NotificationHelper.EXTRA_OPEN_LIST)
        intent.removeExtra(NotificationHelper.EXTRA_TITLE_ID)
        intent.removeExtra(NotificationHelper.EXTRA_CHAPTER_ID)
        intent.removeExtra(NotificationHelper.EXTRA_LINK)
        intent.removeExtra(NotificationHelper.EXTRA_CONVERSATION_ID)
        intent.removeExtra(NotificationHelper.EXTRA_CONVERSATION_TITLE)
        val app = application as? TomiloApp ?: return
        app.container.pendingNotificationOpen.value = NotificationOpen(
            openList = openList || (titleId == null && chapterId == null && link == null),
            titleId = titleId,
            chapterId = chapterId,
            linkUrl = link,
            conversationId = conversationId,
            conversationTitle = conversationTitle,
        )
    }

}

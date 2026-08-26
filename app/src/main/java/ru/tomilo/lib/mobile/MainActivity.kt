package ru.tomilo.lib.mobile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import ru.tomilo.lib.mobile.push.NotificationHelper
import ru.tomilo.lib.mobile.push.NotificationOpen
import ru.tomilo.lib.mobile.push.NotificationsPollWorker
import ru.tomilo.lib.mobile.ui.navigation.TomiloNavHost
import ru.tomilo.lib.mobile.ui.theme.TomiloTheme
import ru.tomilo.lib.mobile.data.update.AppUpdateCheckWorker

class MainActivity : ComponentActivity() {
    private val requestNotifications = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            NotificationsPollWorker.enqueueNow(this)
            AppUpdateCheckWorker.enqueueNow(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        maybeRequestNotificationPermission()
        val app = application as TomiloApp
        handleNotificationIntent(intent)
        setContent {
            TomiloTheme {
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
        NotificationsPollWorker.enqueueNow(this)
        AppUpdateCheckWorker.enqueueNow(this)
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

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

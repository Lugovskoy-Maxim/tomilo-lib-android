package ru.tomilo.lib.mobile

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import ru.tomilo.lib.mobile.push.NotificationHelper
import ru.tomilo.lib.mobile.push.NotificationOpen
import ru.tomilo.lib.mobile.push.NotificationsPollWorker
import ru.tomilo.lib.mobile.ui.navigation.TomiloNavHost
import ru.tomilo.lib.mobile.ui.theme.TomiloTheme
import ru.tomilo.lib.mobile.data.update.AppUpdateCheckWorker
import ru.tomilo.lib.mobile.rustore.RuStoreEngagement
import ru.tomilo.lib.mobile.data.local.AdsConsent
import ru.tomilo.lib.mobile.data.local.ThemeMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as TomiloApp
        handleNotificationIntent(intent)
        setContent {
            val themePrefs = app.container.themePrefs
            val accentHex by themePrefs.accentHexFlow.collectAsState(initial = null)
            val themeMode by themePrefs.themeModeFlow.collectAsState(initial = ThemeMode.SYSTEM)
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> systemDark
            }
            // Системные панели следуют выбранной теме, а не системной ночи.
            androidx.compose.runtime.SideEffect {
                val statusBarStyle = if (darkTheme) {
                    SystemBarStyle.dark(Color.Transparent.toArgb())
                } else {
                    SystemBarStyle.light(Color.Transparent.toArgb(), Color.Transparent.toArgb())
                }
                val navBarStyle = if (darkTheme) {
                    SystemBarStyle.dark(Color.Transparent.toArgb())
                } else {
                    SystemBarStyle.light(Color.Transparent.toArgb(), Color.Transparent.toArgb())
                }
                enableEdgeToEdge(statusBarStyle = statusBarStyle, navigationBarStyle = navBarStyle)
            }
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
            TomiloTheme(accentColor = activeAccent, darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TomiloNavHost(container = app.container)
                    val adsConsent by app.container.adsConsentStore.consent.collectAsState()
                    if (adsConsent == AdsConsent.UNKNOWN) {
                        AlertDialog(
                            onDismissRequest = {},
                            title = { Text("Настройки рекламы") },
                            text = {
                                Column {
                                    Text(
                                        "Реклама помогает поддерживать Tomilo. Выберите, какие объявления " +
                                            "можно показывать. Без рекламы приложение продолжит работать без ограничений.",
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    TextButton(
                                        onClick = {
                                            app.container.adsConsentStore.set(AdsConsent.DENIED)
                                        },
                                    ) {
                                        Text("Не показывать рекламу", color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        app.container.adsConsentStore.set(AdsConsent.GRANTED)
                                    },
                                ) {
                                    Text("Персонализированная")
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = {
                                        app.container.adsConsentStore.set(AdsConsent.CONTEXTUAL)
                                    },
                                ) {
                                    Text("Только контекстная")
                                }
                            },
                        )
                    }
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

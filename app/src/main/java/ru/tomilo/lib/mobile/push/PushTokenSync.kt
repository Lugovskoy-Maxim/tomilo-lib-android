package ru.tomilo.lib.mobile.push

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import ru.rustore.sdk.pushclient.RuStorePushClient
import ru.tomilo.lib.mobile.BuildConfig
import ru.tomilo.lib.mobile.TomiloApp

/**
 * Досылает текущий push-токен (FCM и/или RuStore Push) серверу при старте
 * приложения — на случай, если токен уже существовал до входа в аккаунт
 * (onNewToken тогда не вызывается повторно). На устройствах без нужного
 * канала (Google Play Services для FCM, приложение RuStore для RuStore Push)
 * получение токена просто падает — тихо остаёмся на NotificationsPollWorker.
 */
object PushTokenSync {
    suspend fun syncIfNeeded(context: Context) {
        val app = context.applicationContext as? TomiloApp ?: return
        if (!app.container.authRepository.isLoggedIn()) return

        if (BuildConfig.HAS_FCM) {
            val token = runCatching {
                suspendCancellableCoroutine<String?> { cont ->
                    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                        cont.resumeWith(Result.success(if (task.isSuccessful) task.result else null))
                    }
                }
            }.getOrNull()
            if (token != null) {
                app.container.socialRepository.registerDeviceToken(
                    token = token,
                    appVersion = BuildConfig.VERSION_NAME,
                    provider = "fcm",
                )
            }
        }

        if (BuildConfig.RUSTORE_PUSH_PROJECT_ID.isNotBlank()) {
            val token = runCatching {
                suspendCancellableCoroutine<String?> { cont ->
                    RuStorePushClient.getToken()
                        .addOnSuccessListener { cont.resumeWith(Result.success(it)) }
                        .addOnFailureListener { cont.resumeWith(Result.success(null)) }
                }
            }.getOrNull()
            if (token != null) {
                app.container.socialRepository.registerDeviceToken(
                    token = token,
                    appVersion = BuildConfig.VERSION_NAME,
                    provider = "rustore",
                )
            }
        }
    }
}

package ru.tomilo.lib.mobile.push

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.rustore.sdk.pushclient.messaging.exception.RuStorePushClientException
import ru.rustore.sdk.pushclient.messaging.model.RemoteMessage
import ru.rustore.sdk.pushclient.messaging.service.RuStoreMessagingService
import ru.tomilo.lib.mobile.BuildConfig
import ru.tomilo.lib.mobile.TomiloApp

/**
 * Второй канал push (основной для rustore-флейвора — на этих устройствах
 * обычно нет Google Play Services, и FCM-токен никогда не выдаётся).
 * Данные приходят как data-only сообщение по тем же причинам, что и в FcmService:
 * это гарантирует вызов onMessageReceived вне зависимости от состояния приложения.
 */
class RuStorePushService : RuStoreMessagingService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        val app = application as? TomiloApp ?: return
        scope.launch {
            if (!app.container.authRepository.isLoggedIn()) return@launch
            app.container.socialRepository.registerDeviceToken(
                token = token,
                appVersion = BuildConfig.VERSION_NAME,
                provider = "rustore",
            )
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val title = data["title"]?.takeIf { it.isNotBlank() } ?: "TOMILO LIB"
        val body = data["body"]?.takeIf { it.isNotBlank() } ?: "У вас новое уведомление"
        val titleId = data["titleId"]?.takeIf { it.isNotBlank() }
        val chapterId = data["chapterId"]?.takeIf { it.isNotBlank() }
        val linkUrl = data["url"]?.takeIf { it.isNotBlank() }
        val conversationId = data["conversationId"]?.takeIf { it.isNotBlank() }
        val conversationTitle = data["conversationTitle"]?.takeIf { it.isNotBlank() }
        val tag = data["tag"] ?: "rustore-push"

        NotificationHelper.showUpdate(
            context = applicationContext,
            title = title,
            body = body,
            notificationId = NotificationHelper.idFor(tag),
            titleId = titleId,
            chapterId = chapterId,
            linkUrl = linkUrl,
            conversationId = conversationId,
            conversationTitle = conversationTitle,
        )
    }

    override fun onDeletedMessages() {
        // Часть уведомлений не доставлена (например, истёк TTL) — досинхронизируемся polling'ом.
        NotificationsPollWorker.enqueueNow(applicationContext)
    }

    override fun onError(errors: List<RuStorePushClientException>) {
        // Не критично: если дистрибьютора (RuStore) нет или пользователь не авторизован,
        // приложение остаётся на NotificationsPollWorker.
    }
}

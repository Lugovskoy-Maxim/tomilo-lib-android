package ru.tomilo.lib.mobile.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.BuildConfig
import ru.tomilo.lib.mobile.TomiloApp

/**
 * Данные приходят как data-only сообщение (не FCM "notification"-payload):
 * иначе система показывает трей сама и onMessageReceived не вызывается,
 * пока приложение не на переднем плане — из-за этого уведомления
 * на Android не доходили в фоне/при закрытом приложении.
 */
class FcmService : FirebaseMessagingService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        val app = application as? TomiloApp ?: return
        scope.launch {
            if (!app.container.authRepository.isLoggedIn()) return@launch
            app.container.socialRepository.registerDeviceToken(token, BuildConfig.VERSION_NAME)
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
        val tag = data["tag"] ?: "fcm"

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
        // Сообщения могли истечь в очереди FCM, пока устройство было офлайн.
        // Восстанавливаем состояние из серверной ленты при следующем запуске worker.
        NotificationsPollWorker.enqueueNow(applicationContext)
    }
}

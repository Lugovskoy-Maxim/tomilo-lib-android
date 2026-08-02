package ru.tomilo.lib.mobile.push

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ru.tomilo.lib.mobile.TomiloApp
import java.util.concurrent.TimeUnit

/**
 * Лёгкий polling in-app уведомлений (серверные Web Push — для сайта).
 * Показывает системный push, если появились непрочитанные.
 */
class NotificationsPollWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? TomiloApp ?: return Result.success()
        if (!app.container.authRepository.isLoggedIn()) return Result.success()

        val unread = app.container.socialRepository.notificationsUnread()
        if (unread <= 0) return Result.success()

        val prefs = applicationContext.getSharedPreferences("tomilo_push", Context.MODE_PRIVATE)
        val last = prefs.getInt("last_unread", 0)
        if (unread > last) {
            NotificationHelper.showUpdate(
                context = applicationContext,
                title = "Tomilo",
                body = if (unread == 1) "У вас новое уведомление"
                else "Непрочитанных уведомлений: $unread",
                notificationId = 1001,
            )
        }
        prefs.edit().putInt("last_unread", unread).apply()
        return Result.success()
    }

    companion object {
        private const val UNIQUE = "tomilo_notifications_poll"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<NotificationsPollWorker>(15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}

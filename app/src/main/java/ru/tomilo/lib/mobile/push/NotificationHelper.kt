package ru.tomilo.lib.mobile.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ru.tomilo.lib.mobile.MainActivity
import ru.tomilo.lib.mobile.R

object NotificationHelper {
    const val CHANNEL_ID = "tomilo_updates"
    const val APP_UPDATE_CHANNEL_ID = "tomilo_app_updates"
    private const val CHANNEL_NAME = "Обновления tomilo-lib"
    const val GROUP_KEY = "tomilo_updates_group"
    const val EXTRA_OPEN_LIST = "open_notifications"
    const val EXTRA_TITLE_ID = "notif_title_id"
    const val EXTRA_CHAPTER_ID = "notif_chapter_id"
    const val EXTRA_LINK = "notif_link"
    private const val SUMMARY_ID = 1000
    private const val APP_UPDATE_ID = 0x1230

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Новые главы, ответы и системные уведомления"
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
        manager.createNotificationChannel(
            NotificationChannel(
                APP_UPDATE_CHANNEL_ID,
                "Новые версии приложения",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Обновления tomilo-lib из официальных GitHub Releases"
                enableVibration(true)
            },
        )
    }

    fun canNotify(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun showUpdate(
        context: Context,
        title: String,
        body: String,
        notificationId: Int,
        titleId: String? = null,
        chapterId: String? = null,
        linkUrl: String? = null,
    ): Boolean {
        ensureChannel(context)
        if (!canNotify(context)) return false
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_LIST, true)
            titleId?.takeIf { it.isNotBlank() }?.let { putExtra(EXTRA_TITLE_ID, it) }
            chapterId?.takeIf { it.isNotBlank() }?.let { putExtra(EXTRA_CHAPTER_ID, it) }
            linkUrl?.takeIf { it.isNotBlank() }?.let { putExtra(EXTRA_LINK, it) }
        }
        val pending = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_tomilo)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setGroup(GROUP_KEY)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            showGroupSummary(context)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    private fun showGroupSummary(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_LIST, true)
        }
        val pending = PendingIntent.getActivity(
            context,
            SUMMARY_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val summary = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_tomilo)
            .setContentTitle("tomilo-lib")
            .setContentText("Новые уведомления")
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(SUMMARY_ID, summary)
        } catch (_: SecurityException) {
            // ignore
        }
    }

    fun idFor(notificationId: String): Int {
        val h = notificationId.hashCode()
        return 0x2100 + (h and 0x4FFF)
    }

    fun showAppUpdate(
        context: Context,
        versionName: String,
        releaseUrl: String,
    ): Boolean {
        ensureChannel(context)
        if (!canNotify(context)) return false
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_LINK, releaseUrl)
        }
        val pending = PendingIntent.getActivity(
            context,
            APP_UPDATE_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val body = "Доступна версия $versionName. Откройте официальный релиз, чтобы скачать обновление."
        val notification = NotificationCompat.Builder(context, APP_UPDATE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_tomilo)
            .setContentTitle("Обновление tomilo-lib")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .addAction(0, "Открыть релиз", pending)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
        return try {
            NotificationManagerCompat.from(context).notify(APP_UPDATE_ID, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}

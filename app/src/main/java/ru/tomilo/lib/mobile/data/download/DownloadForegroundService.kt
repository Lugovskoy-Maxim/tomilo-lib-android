package ru.tomilo.lib.mobile.data.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import ru.tomilo.lib.mobile.MainActivity
import ru.tomilo.lib.mobile.R
import ru.tomilo.lib.mobile.TomiloApp

/**
 * Foreground-сервис: скачивание глав продолжается, когда приложение в фоне.
 */
class DownloadForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            (application as? TomiloApp)?.container?.downloadManager?.cancel()
            stopSelfSafely()
            return START_NOT_STICKY
        }

        ensureChannel()
        val notification = buildNotification(
            title = "Скачивание офлайн",
            text = "Подготовка…",
            progress = 0,
            indeterminate = true,
            ongoing = true,
        )
        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                } else {
                    0
                },
            )
        } catch (_: Exception) {
            startForeground(NOTIFICATION_ID, notification)
        }

        val app = application as? TomiloApp
        if (app == null) {
            stopSelfSafely()
            return START_NOT_STICKY
        }

        app.container.downloadManager.runPendingFromService { state ->
            val nm = getSystemService(NotificationManager::class.java)
            if (state.finished) {
                val doneText = state.statusSummary.ifBlank {
                    "Готово: ${state.completedCount}/${state.items.size}"
                }
                nm?.notify(
                    NOTIFICATION_ID,
                    buildNotification(
                        title = "Офлайн: ${state.titleName.ifBlank { "загрузка" }}",
                        text = doneText,
                        progress = 100,
                        indeterminate = false,
                        ongoing = false,
                    ),
                )
                stopSelfSafely()
            } else {
                val pct = (state.overallFraction * 100).toInt().coerceIn(0, 100)
                val active = state.activeItem
                val line = buildString {
                    if (state.titleName.isNotBlank()) append(state.titleName).append(" · ")
                    append("${state.completedCount}/${state.items.size}")
                    active?.let { append(" · ").append(it.stageLabel) }
                }
                nm?.notify(
                    NOTIFICATION_ID,
                    buildNotification(
                        title = "Скачивание офлайн",
                        text = line,
                        progress = pct,
                        indeterminate = active?.stage == DownloadStage.Queued ||
                            active?.stage == DownloadStage.CheckingAccess,
                        ongoing = true,
                    ),
                )
            }
        }

        return START_STICKY
    }

    private fun stopSelfSafely() {
        try {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
        } catch (_: Exception) {
            // ignore
        }
        stopSelf()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Загрузки офлайн",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Прогресс скачивания глав"
            setShowBadge(false)
        }
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(
        title: String,
        text: String,
        progress: Int,
        indeterminate: Boolean,
        ongoing: Boolean,
    ): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val cancel = PendingIntent.getService(
            this,
            1,
            Intent(this, DownloadForegroundService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_tomilo)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setOnlyAlertOnce(true)
            .setOngoing(ongoing)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)

        if (ongoing) {
            builder.setProgress(100, progress, indeterminate)
            builder.addAction(0, "Отменить", cancel)
        } else {
            builder.setProgress(0, 0, false)
            builder.setAutoCancel(true)
        }
        return builder.build()
    }

    companion object {
        const val CHANNEL_ID = "tomilo_downloads"
        const val NOTIFICATION_ID = 4201
        private const val ACTION_STOP = "ru.tomilo.lib.mobile.DOWNLOAD_STOP"

        fun start(context: Context) {
            val intent = Intent(context, DownloadForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, DownloadForegroundService::class.java))
        }
    }
}

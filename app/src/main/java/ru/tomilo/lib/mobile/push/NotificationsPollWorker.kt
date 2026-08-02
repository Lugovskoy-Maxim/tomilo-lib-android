package ru.tomilo.lib.mobile.push

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ru.tomilo.lib.mobile.TomiloApp
import java.util.concurrent.TimeUnit

/**
 * Polling in-app уведомлений (в т.ч. new_chapter для тайтлов в закладках).
 * Показывает системный push по новым событиям.
 */
class NotificationsPollWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? TomiloApp ?: return Result.success()
        if (!app.container.authRepository.isLoggedIn()) return Result.success()

        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lastSeenId = prefs.getString(KEY_LAST_SEEN_ID, "").orEmpty()
        val knownIds = prefs.getStringSet(KEY_KNOWN_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()

        val list = app.container.socialRepository.notifications(page = 1)
            .getOrElse { emptyList() }

        if (list.isEmpty()) {
            // fallback: только счётчик
            val unread = app.container.socialRepository.notificationsUnread()
            val lastCount = prefs.getInt(KEY_LAST_UNREAD, 0)
            if (unread > lastCount && unread > 0) {
                NotificationHelper.showUpdate(
                    context = applicationContext,
                    title = "Tomilo",
                    body = if (unread == 1) "Новое уведомление"
                    else "Непрочитанных: $unread",
                    notificationId = 1001,
                )
            }
            prefs.edit().putInt(KEY_LAST_UNREAD, unread).apply()
            return Result.success()
        }

        // Новые = те, что раньше lastSeenId (список с сервера — новые сверху)
        val fresh = if (lastSeenId.isBlank() && knownIds.isEmpty()) {
            // первый запуск после установки — только непрочитанные new_chapter, без спама всей историей
            list.filter { !it.read() && isChapterRelated(it.type) }.take(5)
        } else {
            list.filter { n ->
                val id = n.stableId()
                id.isNotBlank() && id !in knownIds && id != lastSeenId
            }.take(8)
        }

        var shown = 0
        fresh.forEachIndexed { index, n ->
            val id = n.stableId()
            if (id.isBlank()) return@forEachIndexed
            knownIds.add(id)
            val title = n.title?.takeIf { it.isNotBlank() }
                ?: if (isChapterRelated(n.type)) "Новая глава" else "Tomilo"
            val body = n.message?.takeIf { it.isNotBlank() }
                ?: "У вас новое уведомление"
            NotificationHelper.showUpdate(
                context = applicationContext,
                title = title,
                body = body,
                notificationId = 2000 + (id.hashCode() and 0x0FFF) + index,
            )
            shown++
        }

        // обновить «известные» id (держим до 200)
        val newestId = list.firstOrNull()?.stableId().orEmpty()
        val trimmed = knownIds.toList().takeLast(200).toSet()
        prefs.edit()
            .putString(KEY_LAST_SEEN_ID, newestId.ifBlank { lastSeenId })
            .putStringSet(KEY_KNOWN_IDS, trimmed)
            .putInt(KEY_LAST_UNREAD, list.count { !it.read() })
            .apply()

        // если ничего не нашли по id, но unread вырос — generic
        if (shown == 0) {
            val unread = app.container.socialRepository.notificationsUnread()
            val lastCount = prefs.getInt(KEY_LAST_UNREAD_FALLBACK, 0)
            if (unread > lastCount && unread > 0) {
                val chapter = list.firstOrNull { !it.read() && isChapterRelated(it.type) }
                NotificationHelper.showUpdate(
                    context = applicationContext,
                    title = chapter?.title ?: "Tomilo",
                    body = chapter?.message
                        ?: if (unread == 1) "Новое уведомление" else "Непрочитанных: $unread",
                    notificationId = 1001,
                )
            }
            prefs.edit().putInt(KEY_LAST_UNREAD_FALLBACK, unread).apply()
        }

        return Result.success()
    }

    private fun isChapterRelated(type: String?): Boolean {
        val t = type?.lowercase().orEmpty()
        return t == "new_chapter" || t.contains("chapter") || t.contains("title_update")
    }

    companion object {
        private const val UNIQUE_PERIODIC = "tomilo_notifications_poll"
        private const val UNIQUE_ONCE = "tomilo_notifications_poll_once"
        private const val PREFS = "tomilo_push"
        private const val KEY_LAST_SEEN_ID = "last_seen_notif_id"
        private const val KEY_KNOWN_IDS = "known_notif_ids"
        private const val KEY_LAST_UNREAD = "last_unread"
        private const val KEY_LAST_UNREAD_FALLBACK = "last_unread_fallback"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // Минимум WorkManager для periodic — 15 мин
            val periodic = PeriodicWorkRequestBuilder<NotificationsPollWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodic,
            )

            // Сразу после старта приложения
            val once = OneTimeWorkRequestBuilder<NotificationsPollWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_ONCE,
                ExistingWorkPolicy.REPLACE,
                once,
            )
        }
    }
}

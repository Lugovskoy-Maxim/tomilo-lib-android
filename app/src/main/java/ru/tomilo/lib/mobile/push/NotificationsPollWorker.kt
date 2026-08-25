package ru.tomilo.lib.mobile.push

import android.content.Context
import androidx.work.BackoffPolicy
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
        val knownIds = loadKnownIds(prefs)

        val listResult = app.container.socialRepository.notifications(page = 1)
        val list = listResult.getOrElse {
            // Серверная лента может временно не ответить, но проверка закладок
            // остаётся независимым резервным каналом новых глав.
            pollBookmarkChapters(app, prefs, emptySet())
            return if (runAttemptCount < 3) Result.retry() else Result.success()
        }

        if (list.isEmpty()) {
            val unread = app.container.socialRepository.notificationsUnread()
            val lastCount = prefs.getInt(KEY_LAST_UNREAD, 0)
            val bookmarkNotifications = pollBookmarkChapters(app, prefs, emptySet())
            if (
                bookmarkNotifications == 0 &&
                unread > lastCount &&
                unread > 0 &&
                NotificationHelper.canNotify(applicationContext)
            ) {
                NotificationHelper.showUpdate(
                    context = applicationContext,
                    title = "TOMILO LIB",
                    body = if (unread == 1) "Новое уведомление"
                    else "Непрочитанных: $unread",
                    notificationId = 1001,
                )
            }
            prefs.edit().putInt(KEY_LAST_UNREAD, unread).apply()
            return Result.success()
        }

        val firstRun = lastSeenId.isBlank() && knownIds.isEmpty()
        val fresh = if (firstRun) {
            list.filter { !it.read() && isChapterRelated(it.type) }.take(5)
        } else {
            list.filter { n ->
                val id = n.stableId()
                id.isNotBlank() && id !in knownIds && !n.read()
            }.take(8)
        }

        // Не спамим историей и уже прочитанным: запоминаем id без показа.
        list.forEach { n ->
            val id = n.stableId()
            if (id.isNotBlank() && (firstRun || n.read())) knownIds.add(id)
        }

        val deliveredChapterTitles = linkedSetOf<String>()
        fresh.forEach { n ->
            val id = n.stableId()
            if (id.isBlank()) return@forEach
            val title = n.title?.takeIf { it.isNotBlank() }
                ?: if (isChapterRelated(n.type)) "Новая глава" else "TOMILO LIB"
            val body = n.message?.takeIf { it.isNotBlank() }
                ?: "У вас новое уведомление"
            val open = n.toOpenRequest()
            val delivered = NotificationHelper.showUpdate(
                context = applicationContext,
                title = title,
                body = body,
                notificationId = NotificationHelper.idFor(id),
                titleId = open.titleId,
                chapterId = open.chapterId,
                linkUrl = open.linkUrl,
            )
            if (delivered) {
                knownIds.add(id)
                if (isChapterRelated(n.type)) {
                    n.resolvedTitleId().takeIf { it.isNotBlank() }?.let(deliveredChapterTitles::add)
                }
            }
        }

        val newestId = list.firstOrNull()?.stableId().orEmpty()
        val unread = list.count { !it.read() }
        saveKnownIds(prefs, knownIds)
        prefs.edit()
            .putString(KEY_LAST_SEEN_ID, newestId.ifBlank { lastSeenId })
            .putInt(KEY_LAST_UNREAD, unread)
            .apply()

        pollBookmarkChapters(app, prefs, deliveredChapterTitles)

        return Result.success()
    }

    private suspend fun pollBookmarkChapters(
        app: TomiloApp,
        prefs: android.content.SharedPreferences,
        alreadyDeliveredTitleIds: Set<String>,
    ): Int {
        val bookmarks = app.container.socialRepository.bookmarks().getOrElse { return 0 }
        val current = bookmarks.mapNotNull { bookmark ->
            if (bookmark.category.equals("dropped", ignoreCase = true)) return@mapNotNull null
            val title = bookmark.resolvedTitle() ?: return@mapNotNull null
            val titleId = bookmark.resolvedTitleId().takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            val count = (title.totalChapters ?: title.chaptersCount)?.coerceAtLeast(0)
                ?: return@mapNotNull null
            BookmarkChapterSnapshot(
                titleId = titleId,
                titleName = bookmark.displayName(),
                chapterCount = count,
            )
        }.distinctBy { it.titleId }

        val oldIds = prefs.getString(KEY_BOOKMARK_IDS, "").orEmpty()
            .split(',')
            .filter { it.isNotBlank() }
            .toSet()
        val previous = oldIds.associateWith { id ->
            prefs.getInt(bookmarkCountKey(id), -1)
        }.filterValues { it >= 0 }
        val ready = prefs.getBoolean(KEY_BOOKMARK_SNAPSHOT_READY, false)
        val updates = if (ready) {
            findBookmarkChapterUpdates(previous, current, alreadyDeliveredTitleIds)
        } else {
            emptyList()
        }

        val deliveredIds = updates.mapNotNullTo(mutableSetOf()) { item ->
            val gained = item.chapterCount - (previous[item.titleId] ?: item.chapterCount)
            val body = if (gained == 1) {
                "В «${item.titleName}» появилась новая глава"
            } else {
                "В «${item.titleName}» появилось новых глав: $gained"
            }
            val shown = NotificationHelper.showUpdate(
                context = applicationContext,
                title = "Новые главы в закладках",
                body = body,
                notificationId = NotificationHelper.idFor("bookmark:${item.titleId}:${item.chapterCount}"),
                titleId = item.titleId,
            )
            item.titleId.takeIf { shown }
        }

        val currentIds = current.mapTo(linkedSetOf()) { it.titleId }
        val editor = prefs.edit()
            .putBoolean(KEY_BOOKMARK_SNAPSHOT_READY, true)
            .putString(KEY_BOOKMARK_IDS, currentIds.joinToString(","))
        (oldIds - currentIds).forEach { editor.remove(bookmarkCountKey(it)) }
        current.forEach { item ->
            val old = previous[item.titleId]
            val serverDelivered = item.titleId in alreadyDeliveredTitleIds
            val safeToAdvance = old == null ||
                item.chapterCount <= old ||
                item.titleId in deliveredIds ||
                serverDelivered
            if (safeToAdvance) editor.putInt(bookmarkCountKey(item.titleId), item.chapterCount)
        }
        editor.apply()
        return deliveredIds.size
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
        private const val KEY_KNOWN_IDS_CSV = "known_notif_ids_csv"
        private const val KEY_LAST_UNREAD = "last_unread"
        private const val KEY_BOOKMARK_SNAPSHOT_READY = "bookmark_snapshot_ready"
        private const val KEY_BOOKMARK_IDS = "bookmark_title_ids"
        private const val KEY_BOOKMARK_COUNT_PREFIX = "bookmark_chapter_count:"

        private fun bookmarkCountKey(titleId: String) = "$KEY_BOOKMARK_COUNT_PREFIX$titleId"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodic = PeriodicWorkRequestBuilder<NotificationsPollWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC,
                // Обновляем существующую задачу после установки новой версии,
                // чтобы новые ограничения и логика применялись без переустановки.
                ExistingPeriodicWorkPolicy.UPDATE,
                periodic,
            )

            enqueueNow(context)
        }

        fun enqueueNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val once = OneTimeWorkRequestBuilder<NotificationsPollWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_ONCE,
                // Повторный вызов не должен отменять уже идущий сетевой запрос.
                ExistingWorkPolicy.KEEP,
                once,
            )
        }

        private fun loadKnownIds(prefs: android.content.SharedPreferences): LinkedHashSet<String> {
            val csv = prefs.getString(KEY_KNOWN_IDS_CSV, "").orEmpty()
            val fromCsv = csv.split(',').map { it.trim() }.filter { it.isNotBlank() }
            val legacy = prefs.getStringSet(KEY_KNOWN_IDS, emptySet()).orEmpty()
            return LinkedHashSet<String>().apply {
                addAll(fromCsv)
                addAll(legacy)
            }
        }

        private fun saveKnownIds(prefs: android.content.SharedPreferences, ids: LinkedHashSet<String>) {
            val trimmed = ids.toList().takeLast(300)
            prefs.edit()
                .putString(KEY_KNOWN_IDS_CSV, trimmed.joinToString(","))
                .remove(KEY_KNOWN_IDS)
                .apply()
        }
    }
}

package ru.tomilo.lib.mobile.data.update

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
import ru.tomilo.lib.mobile.BuildConfig
import ru.tomilo.lib.mobile.push.NotificationHelper
import java.util.concurrent.TimeUnit

class AppUpdateCheckWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        // RuStore builds use the native store update flow from MainActivity.
        // GitHub remains a fallback only for Play/direct installations.
        if (BuildConfig.STORE_CHANNEL == "rustore") return Result.success()
        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val lastCheck = prefs.getLong(KEY_LAST_CHECK, 0L)
        if (runAttemptCount == 0 && now - lastCheck < MIN_CHECK_INTERVAL_MS) {
            return Result.success()
        }

        val app = applicationContext as? TomiloApp ?: return Result.success()
        val updater = app.container.appUpdateManager
        val release = updater.fetchLatest().getOrElse {
            return if (runAttemptCount < 3) Result.retry() else Result.success()
        }
        prefs.edit().putLong(KEY_LAST_CHECK, now).apply()
        if (!updater.isNewer(release)) return Result.success()

        val notificationKey = release.versionCode?.let { "code:$it" }
            ?: "name:${release.versionName}"
        if (prefs.getString(KEY_LAST_NOTIFIED, null) == notificationKey) {
            return Result.success()
        }
        val shown = NotificationHelper.showAppUpdate(
            context = applicationContext,
            versionName = release.versionName,
            releaseUrl = release.htmlUrl,
        )
        if (shown) prefs.edit().putString(KEY_LAST_NOTIFIED, notificationKey).apply()
        return Result.success()
    }

    companion object {
        private const val UNIQUE_PERIODIC = "tomilo_app_update_check"
        private const val UNIQUE_NOW = "tomilo_app_update_check_now"
        private const val PREFS = "tomilo_app_update"
        private const val KEY_LAST_CHECK = "last_successful_check"
        private const val KEY_LAST_NOTIFIED = "last_notified_release"
        private const val MIN_CHECK_INTERVAL_MS = 6L * 60L * 60L * 1000L

        private fun networkConstraints(): Constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<AppUpdateCheckWorker>(12, TimeUnit.HOURS)
                .setConstraints(networkConstraints())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
            enqueueNow(context)
        }

        fun enqueueNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<AppUpdateCheckWorker>()
                .setConstraints(networkConstraints())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_NOW,
                ExistingWorkPolicy.KEEP,
                request,
            )
        }
    }
}

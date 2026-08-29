package ru.tomilo.lib.mobile.rustore

import android.app.Activity
import android.content.Context
import ru.rustore.sdk.appupdate.manager.factory.RuStoreAppUpdateManagerFactory
import ru.rustore.sdk.appupdate.model.AppUpdateOptions
import ru.rustore.sdk.appupdate.model.AppUpdateType
import ru.rustore.sdk.appupdate.model.InstallStatus
import ru.rustore.sdk.appupdate.model.UpdateAvailability
import ru.rustore.sdk.review.RuStoreReviewManagerFactory
import ru.tomilo.lib.mobile.BuildConfig
import java.lang.ref.WeakReference

/**
 * Native RuStore flows. They are deliberately no-ops outside the RuStore
 * flavor, so the Play build keeps its regular update path and never receives
 * a store-specific dialog.
 */
object RuStoreEngagement {
    private const val PREFS = "rustore_engagement"
    private const val KEY_UPDATE_CHECKED_AT = "update_checked_at"
    private const val KEY_READ_CHAPTERS = "read_chapters"
    private const val KEY_REVIEW_REQUESTED_AT = "review_requested_at"
    private const val UPDATE_CHECK_INTERVAL_MS = 6L * 60L * 60L * 1000L
    private const val REVIEW_COOLDOWN_MS = 30L * 24L * 60L * 60L * 1000L
    private const val REVIEW_CHAPTERS_THRESHOLD = 7

    private var activityRef = WeakReference<Activity>(null)

    private fun isRuStoreBuild() = BuildConfig.STORE_CHANNEL == "rustore"

    fun attach(activity: Activity) {
        if (!isRuStoreBuild()) return
        activityRef = WeakReference(activity)
        checkForUpdate(activity)
    }

    fun detach(activity: Activity) {
        if (activityRef.get() === activity) activityRef = WeakReference(null)
    }

    /** Checks once per six hours and starts the RuStore flexible update UI only when needed. */
    private fun checkForUpdate(activity: Activity) {
        val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (now - prefs.getLong(KEY_UPDATE_CHECKED_AT, 0L) < UPDATE_CHECK_INTERVAL_MS) return
        prefs.edit().putLong(KEY_UPDATE_CHECKED_AT, now).apply()

        val manager = RuStoreAppUpdateManagerFactory.create(activity)
        manager.getAppUpdateInfo()
            .addOnSuccessListener { info ->
                val options = AppUpdateOptions.Builder()
                    .appUpdateType(AppUpdateType.FLEXIBLE)
                    .build()
                when {
                    info.installStatus == InstallStatus.DOWNLOADED -> {
                        // The download was already accepted by the user earlier.
                        manager.completeUpdate(options)
                    }
                    info.updateAvailability == UpdateAvailability.UPDATE_AVAILABLE -> {
                        manager.startUpdateFlow(info, options)
                    }
                }
            }
        // Absence/outdated version of RuStore is expected on other devices;
        // do not show an error or interrupt reading.
    }

    /**
     * A non-intrusive rating request after several completed chapters. RuStore
     * also enforces its own one-day rate limit; our monthly cooldown avoids
     * making a request while the reader is in a short session.
     */
    fun onChapterRead(context: Context) {
        if (!isRuStoreBuild()) return
        val activity = activityRef.get() ?: return
        if (activity.isFinishing || activity.isDestroyed) return

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val reads = prefs.getInt(KEY_READ_CHAPTERS, 0) + 1
        prefs.edit().putInt(KEY_READ_CHAPTERS, reads).apply()
        val now = System.currentTimeMillis()
        if (reads < REVIEW_CHAPTERS_THRESHOLD || now - prefs.getLong(KEY_REVIEW_REQUESTED_AT, 0L) < REVIEW_COOLDOWN_MS) {
            return
        }

        // Mark the attempt before requesting the form so repeated chapter
        // callbacks cannot open several SDK flows at once.
        prefs.edit().putLong(KEY_REVIEW_REQUESTED_AT, now).putInt(KEY_READ_CHAPTERS, 0).apply()
        val manager = RuStoreReviewManagerFactory.create(activity)
        manager.requestReviewFlow()
            .addOnSuccessListener { reviewInfo ->
                if (!activity.isFinishing && !activity.isDestroyed) {
                    activity.runOnUiThread { manager.launchReviewFlow(reviewInfo) }
                }
            }
        // Review SDK returns expected failures for an unavailable store,
        // unauthorized user or an already submitted review. No UI is needed.
    }
}

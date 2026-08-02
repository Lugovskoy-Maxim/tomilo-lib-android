package ru.tomilo.lib.mobile.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.adFrequencyDataStore by preferencesDataStore("tomilo_ad_frequency")

/**
 * Кулдаун межглавной рекламы (~1 раз в 10 минут).
 */
class AdFrequencyStore(private val context: Context) {
    private val lastInterChapterKey = longPreferencesKey("last_inter_chapter_ad_at")

    companion object {
        /** Минимальный интервал между показами между главами. */
        const val INTER_CHAPTER_INTERVAL_MS = 10L * 60L * 1000L // 10 минут
    }

    suspend fun lastInterChapterAt(): Long =
        context.adFrequencyDataStore.data.map { it[lastInterChapterKey] ?: 0L }.first()

    suspend fun canShowInterChapter(now: Long = System.currentTimeMillis()): Boolean {
        val last = lastInterChapterAt()
        if (last <= 0L) return true
        return now - last >= INTER_CHAPTER_INTERVAL_MS
    }

    suspend fun markInterChapterShown(now: Long = System.currentTimeMillis()) {
        context.adFrequencyDataStore.edit { it[lastInterChapterKey] = now }
    }

    /** Сколько мс осталось до следующего возможного показа (0 = можно). */
    suspend fun remainingCooldownMs(now: Long = System.currentTimeMillis()): Long {
        val last = lastInterChapterAt()
        if (last <= 0L) return 0L
        return (INTER_CHAPTER_INTERVAL_MS - (now - last)).coerceAtLeast(0L)
    }
}

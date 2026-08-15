package ru.tomilo.lib.mobile.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import ru.tomilo.lib.mobile.core.ReaderDirection
import ru.tomilo.lib.mobile.core.ReaderLayout
import ru.tomilo.lib.mobile.core.ReaderMode
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.readingDataStore by preferencesDataStore("tomilo_reading")

data class ReadingSettings(
    val autoScrollSpeed: Float = 1.5f, // dp per frame-ish multiplier
    val keepScreenOn: Boolean = true,
    val startFullscreen: Boolean = true,
    val volumeButtonsScroll: Boolean = false,
    val lastAutoScrollOn: Boolean = false,
)

data class ReadingPosition(
    val pageIndex: Int = 0,
    val scrollOffset: Int = 0,
)

class ReadingPrefs(private val context: Context) {
    private val speedKey = floatPreferencesKey("auto_scroll_speed")
    private val keepOnKey = booleanPreferencesKey("keep_screen_on")
    private val fullscreenKey = booleanPreferencesKey("start_fullscreen")
    private val volumeKey = booleanPreferencesKey("volume_buttons")
    private val pendingHistoryKey = stringSetPreferencesKey("pending_offline_history")

    val settingsFlow: Flow<ReadingSettings> = context.readingDataStore.data.map { p ->
        ReadingSettings(
            autoScrollSpeed = p[speedKey] ?: 1.5f,
            keepScreenOn = p[keepOnKey] ?: true,
            startFullscreen = p[fullscreenKey] ?: true,
            volumeButtonsScroll = p[volumeKey] ?: false,
        )
    }

    suspend fun setAutoScrollSpeed(speed: Float) {
        context.readingDataStore.edit { it[speedKey] = speed.coerceIn(0.4f, 5f) }
    }

    suspend fun setKeepScreenOn(value: Boolean) {
        context.readingDataStore.edit { it[keepOnKey] = value }
    }

    suspend fun setStartFullscreen(value: Boolean) {
        context.readingDataStore.edit { it[fullscreenKey] = value }
    }

    suspend fun readingPosition(chapterId: String): ReadingPosition {
        if (chapterId.isBlank()) return ReadingPosition()
        val pageKey = intPreferencesKey("position_${chapterId}_page")
        val offsetKey = intPreferencesKey("position_${chapterId}_offset")
        val prefs = context.readingDataStore.data.first()
        return ReadingPosition(
            pageIndex = (prefs[pageKey] ?: 0).coerceAtLeast(0),
            scrollOffset = (prefs[offsetKey] ?: 0).coerceAtLeast(0),
        )
    }

    suspend fun saveReadingPosition(chapterId: String, pageIndex: Int, scrollOffset: Int) {
        if (chapterId.isBlank()) return
        val pageKey = intPreferencesKey("position_${chapterId}_page")
        val offsetKey = intPreferencesKey("position_${chapterId}_offset")
        context.readingDataStore.edit {
            it[pageKey] = pageIndex.coerceAtLeast(0)
            it[offsetKey] = scrollOffset.coerceAtLeast(0)
        }
    }

    suspend fun markLocalRead(titleId: String, chapterId: String, queueSync: Boolean) {
        if (titleId.isBlank() || chapterId.isBlank()) return
        val readKey = stringSetPreferencesKey("offline_read_$titleId")
        context.readingDataStore.edit { prefs ->
            prefs[readKey] = prefs[readKey].orEmpty() + chapterId
            if (queueSync) {
                prefs[pendingHistoryKey] = prefs[pendingHistoryKey].orEmpty() + "$titleId|$chapterId"
            }
        }
    }

    suspend fun localReadIds(titleId: String): Set<String> {
        if (titleId.isBlank()) return emptySet()
        return context.readingDataStore.data.first()[stringSetPreferencesKey("offline_read_$titleId")]
            .orEmpty()
    }

    suspend fun pendingHistory(): Set<Pair<String, String>> =
        context.readingDataStore.data.first()[pendingHistoryKey]
            .orEmpty()
            .mapNotNull { raw ->
                val parts = raw.split('|', limit = 2)
                if (parts.size == 2 && parts.all { it.isNotBlank() }) parts[0] to parts[1] else null
            }
            .toSet()

    suspend fun markHistorySynced(titleId: String, chapterId: String) {
        context.readingDataStore.edit { prefs ->
            prefs[pendingHistoryKey] = prefs[pendingHistoryKey].orEmpty() - "$titleId|$chapterId"
        }
    }

    suspend fun layoutFor(titleId: String, titleType: String?): ReaderLayout {
        val stored = titleId.takeIf { it.isNotBlank() }?.let { id ->
            context.readingDataStore.data.first()[stringPreferencesKey("layout_$id")]
        }
        return when (stored) {
            "webtoon" -> ReaderLayout.WEBTOON
            "pager" -> ReaderLayout.PAGER
            else -> ReaderMode.inferLayout(titleType)
        }
    }

    suspend fun setLayoutFor(titleId: String, layout: ReaderLayout) {
        if (titleId.isBlank()) return
        context.readingDataStore.edit {
            it[stringPreferencesKey("layout_$titleId")] = when (layout) {
                ReaderLayout.WEBTOON -> "webtoon"
                ReaderLayout.PAGER -> "pager"
            }
        }
    }

    suspend fun directionFor(titleId: String, titleType: String?): ReaderDirection {
        val stored = titleId.takeIf { it.isNotBlank() }?.let { id ->
            context.readingDataStore.data.first()[stringPreferencesKey("direction_$id")]
        }
        return when (stored) {
            "ltr" -> ReaderDirection.LTR
            "rtl" -> ReaderDirection.RTL
            else -> ReaderMode.inferDirection(titleType)
        }
    }

    suspend fun setDirectionFor(titleId: String, direction: ReaderDirection) {
        if (titleId.isBlank()) return
        context.readingDataStore.edit {
            it[stringPreferencesKey("direction_$titleId")] = when (direction) {
                ReaderDirection.LTR -> "ltr"
                ReaderDirection.RTL -> "rtl"
            }
        }
    }
}

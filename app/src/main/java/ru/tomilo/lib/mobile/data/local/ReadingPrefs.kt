package ru.tomilo.lib.mobile.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.readingDataStore by preferencesDataStore("tomilo_reading")

data class ReadingSettings(
    val autoScrollSpeed: Float = 1.5f, // dp per frame-ish multiplier
    val keepScreenOn: Boolean = true,
    val startFullscreen: Boolean = true,
    val volumeButtonsScroll: Boolean = false,
    val lastAutoScrollOn: Boolean = false,
)

class ReadingPrefs(private val context: Context) {
    private val speedKey = floatPreferencesKey("auto_scroll_speed")
    private val keepOnKey = booleanPreferencesKey("keep_screen_on")
    private val fullscreenKey = booleanPreferencesKey("start_fullscreen")
    private val volumeKey = booleanPreferencesKey("volume_buttons")

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
}

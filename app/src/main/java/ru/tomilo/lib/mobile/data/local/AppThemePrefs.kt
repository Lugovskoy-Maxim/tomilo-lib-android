package ru.tomilo.lib.mobile.data.local

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore("tomilo_app_theme")

data class AccentPaletteItem(
    val id: String,
    val name: String,
    val hex: String,
    val color: Color,
    val description: String,
)

val DEFAULT_ACCENT_HEX = "#FF5F57"

val ACCENT_PALETTES = listOf(
    AccentPaletteItem("coral", "Коралловый", "#FF5F57", Color(0xFFFF5F57), "Фирменный стиль Tomilo"),
    AccentPaletteItem("crimson", "Рубин", "#F43F5E", Color(0xFFF43F5E), "Тёплый красный"),
    AccentPaletteItem("amber", "Янтарь", "#FFB300", Color(0xFFFFB300), "Золотой акцент"),
    AccentPaletteItem("orange", "Оранжевый", "#FF7043", Color(0xFFFF7043), "Закатный луч"),
    AccentPaletteItem("amethyst", "Аметист", "#9D4EDD", Color(0xFF9D4EDD), "Магический фиолетовый"),
    AccentPaletteItem("emerald", "Изумруд", "#10B981", Color(0xFF10B981), "Киберпанк-нефрит"),
    AccentPaletteItem("sapphire", "Сапфир", "#00B4D8", Color(0xFF00B4D8), "Морская лазурь"),
    AccentPaletteItem("cyan", "Циан", "#06B6D4", Color(0xFF06B6D4), "Электрическая волна"),
)

/** Первые акценты доступны всем, остальные — с Premium, как в макете настроек. */
const val FREE_ACCENT_COUNT = 4

fun accentUnlocked(hex: String?, premium: Boolean): Boolean {
    if (hex.isNullOrBlank() || hex.equals(DEFAULT_ACCENT_HEX, ignoreCase = true)) return true
    val index = ACCENT_PALETTES.indexOfFirst { it.hex.equals(hex, ignoreCase = true) }
    if (index < 0) return premium
    return premium || index < FREE_ACCENT_COUNT
}

/** Режим темы: следовать системе, принудительно тёмная или светлая. */
enum class ThemeMode(val storageValue: String) {
    SYSTEM("system"),
    DARK("dark"),
    LIGHT("light"),
    COFFEE("coffee"),
    ;

    companion object {
        fun fromStorage(value: String?): ThemeMode =
            entries.firstOrNull { it.storageValue == value } ?: SYSTEM
    }
}

class AppThemePrefs(private val context: Context) {
    private val accentKey = stringPreferencesKey("accent_color_hex")
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val shelfNotificationsKey = booleanPreferencesKey("shelf_notifications_enabled")

    val accentHexFlow: Flow<String?> = context.themeDataStore.data.map { prefs ->
        prefs[accentKey]
    }

    val themeModeFlow: Flow<ThemeMode> = context.themeDataStore.data.map { prefs ->
        ThemeMode.fromStorage(prefs[themeModeKey])
    }

    val shelfNotificationsFlow: Flow<Boolean> = context.themeDataStore.data.map { prefs ->
        prefs[shelfNotificationsKey] ?: true
    }

    suspend fun accentHex(): String? = accentHexFlow.first()

    suspend fun themeMode(): ThemeMode = themeModeFlow.first()

    suspend fun shelfNotificationsEnabled(): Boolean = shelfNotificationsFlow.first()

    suspend fun setAccentHex(hex: String?) {
        context.themeDataStore.edit { prefs ->
            if (hex == null || hex.isBlank() || hex.equals(DEFAULT_ACCENT_HEX, ignoreCase = true)) {
                prefs.remove(accentKey)
            } else {
                prefs[accentKey] = hex
            }
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { prefs ->
            if (mode == ThemeMode.SYSTEM) {
                prefs.remove(themeModeKey)
            } else {
                prefs[themeModeKey] = mode.storageValue
            }
        }
    }

    suspend fun setShelfNotificationsEnabled(enabled: Boolean) {
        context.themeDataStore.edit { prefs ->
            prefs[shelfNotificationsKey] = enabled
        }
    }

    companion object {
        fun resolveColor(hex: String?): Color {
            if (hex.isNullOrBlank()) return Color(0xFFFF5F57)
            val found = ACCENT_PALETTES.firstOrNull { it.hex.equals(hex, ignoreCase = true) }
            if (found != null) return found.color
            return try {
                Color(android.graphics.Color.parseColor(hex))
            } catch (_: Exception) {
                Color(0xFFFF5F57)
            }
        }
    }
}

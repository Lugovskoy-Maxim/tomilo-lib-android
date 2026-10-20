package ru.tomilo.lib.mobile.data.local

import java.time.LocalDate

/** Выбор сезонного оформления: календарь, ручной запуск или отключение. */
enum class SeasonalThemeMode(val storageValue: String, val label: String) {
    AUTO("auto", "Авто по календарю"),
    HALLOWEEN("halloween", "Хэллоуин"),
    NEW_YEAR("new_year", "Новый год"),
    SUMMER("summer", "Лето"),
    AUTUMN("autumn", "Осень"),
    OFF("off", "Выключено");

    companion object {
        fun fromStorage(value: String?): SeasonalThemeMode =
            entries.firstOrNull { it.storageValue == value } ?: AUTO
    }
}

/** Активные визуальные наборы. Новые сезоны подключаются здесь и в SeasonalThemeMode. */
enum class SeasonalTheme {
    NONE,
    HALLOWEEN,
    NEW_YEAR,
    SUMMER,
    AUTUMN,
}

fun resolveSeasonalTheme(mode: SeasonalThemeMode, date: LocalDate): SeasonalTheme = when (mode) {
    SeasonalThemeMode.OFF -> SeasonalTheme.NONE
    SeasonalThemeMode.HALLOWEEN -> SeasonalTheme.HALLOWEEN
    SeasonalThemeMode.NEW_YEAR -> SeasonalTheme.NEW_YEAR
    SeasonalThemeMode.SUMMER -> SeasonalTheme.SUMMER
    SeasonalThemeMode.AUTUMN -> SeasonalTheme.AUTUMN
    SeasonalThemeMode.AUTO -> {
        when {
            date.monthValue == 12 && date.dayOfMonth >= 15 -> SeasonalTheme.NEW_YEAR
            date.monthValue == 1 && date.dayOfMonth <= 15 -> SeasonalTheme.NEW_YEAR
            date.monthValue in 6..8 -> SeasonalTheme.SUMMER
            date.monthValue == 10 -> SeasonalTheme.HALLOWEEN
            date.monthValue in setOf(9, 11) -> SeasonalTheme.AUTUMN
            else -> SeasonalTheme.NONE
        }
    }
}

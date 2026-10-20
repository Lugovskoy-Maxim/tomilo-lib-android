package ru.tomilo.lib.mobile.data.local

import java.time.LocalDate

/** Выбор сезонного оформления: календарь, ручной запуск или отключение. */
enum class SeasonalThemeMode(val storageValue: String, val label: String) {
    AUTO("auto", "Авто по календарю"),
    HALLOWEEN("halloween", "Хэллоуин"),
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
}

fun resolveSeasonalTheme(mode: SeasonalThemeMode, date: LocalDate): SeasonalTheme = when (mode) {
    SeasonalThemeMode.OFF -> SeasonalTheme.NONE
    SeasonalThemeMode.HALLOWEEN -> SeasonalTheme.HALLOWEEN
    SeasonalThemeMode.AUTO -> {
        val halloweenStart = LocalDate.of(date.year, 10, 1)
        val halloweenEndExclusive = LocalDate.of(date.year, 11, 1)
        if (date >= halloweenStart && date < halloweenEndExclusive) {
            SeasonalTheme.HALLOWEEN
        } else {
            SeasonalTheme.NONE
        }
    }
}

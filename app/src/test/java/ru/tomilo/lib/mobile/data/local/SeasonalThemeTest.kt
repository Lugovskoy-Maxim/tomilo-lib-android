package ru.tomilo.lib.mobile.data.local

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class SeasonalThemeTest {
    @Test
    fun autoModeEnablesHalloweenForOctoberAndTurnsOffAtBoundary() {
        assertEquals(SeasonalTheme.AUTUMN, resolveSeasonalTheme(SeasonalThemeMode.AUTO, LocalDate.of(2026, 9, 30)))
        assertEquals(SeasonalTheme.HALLOWEEN, resolveSeasonalTheme(SeasonalThemeMode.AUTO, LocalDate.of(2026, 10, 1)))
        assertEquals(SeasonalTheme.HALLOWEEN, resolveSeasonalTheme(SeasonalThemeMode.AUTO, LocalDate.of(2026, 10, 31)))
        assertEquals(SeasonalTheme.AUTUMN, resolveSeasonalTheme(SeasonalThemeMode.AUTO, LocalDate.of(2026, 11, 1)))
        assertEquals(SeasonalTheme.NONE, resolveSeasonalTheme(SeasonalThemeMode.AUTO, LocalDate.of(2026, 12, 14)))
    }

    @Test
    fun autoModeSelectsNewYearSummerAndAutumnRanges() {
        assertEquals(SeasonalTheme.NEW_YEAR, resolveSeasonalTheme(SeasonalThemeMode.AUTO, LocalDate.of(2026, 12, 15)))
        assertEquals(SeasonalTheme.NEW_YEAR, resolveSeasonalTheme(SeasonalThemeMode.AUTO, LocalDate.of(2027, 1, 15)))
        assertEquals(SeasonalTheme.NONE, resolveSeasonalTheme(SeasonalThemeMode.AUTO, LocalDate.of(2027, 1, 16)))
        assertEquals(SeasonalTheme.SUMMER, resolveSeasonalTheme(SeasonalThemeMode.AUTO, LocalDate.of(2026, 6, 1)))
        assertEquals(SeasonalTheme.AUTUMN, resolveSeasonalTheme(SeasonalThemeMode.AUTO, LocalDate.of(2026, 9, 1)))
    }

    @Test
    fun manualModeOverridesCalendarAndOffAlwaysDisables() {
        val winterDate = LocalDate.of(2026, 1, 5)
        assertEquals(SeasonalTheme.HALLOWEEN, resolveSeasonalTheme(SeasonalThemeMode.HALLOWEEN, winterDate))
        assertEquals(SeasonalTheme.SUMMER, resolveSeasonalTheme(SeasonalThemeMode.SUMMER, winterDate))
        assertEquals(SeasonalTheme.NONE, resolveSeasonalTheme(SeasonalThemeMode.OFF, LocalDate.of(2026, 10, 20)))
    }

    @Test
    fun unknownStoredModeFallsBackToAutomatic() {
        assertEquals(SeasonalThemeMode.AUTO, SeasonalThemeMode.fromStorage("unknown"))
    }
}

package ru.tomilo.lib.mobile.data.local

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class SeasonalThemeTest {
    @Test
    fun autoModeEnablesHalloweenForOctoberAndTurnsOffAtBoundary() {
        assertEquals(SeasonalTheme.NONE, resolveSeasonalTheme(SeasonalThemeMode.AUTO, LocalDate.of(2026, 9, 30)))
        assertEquals(SeasonalTheme.HALLOWEEN, resolveSeasonalTheme(SeasonalThemeMode.AUTO, LocalDate.of(2026, 10, 1)))
        assertEquals(SeasonalTheme.HALLOWEEN, resolveSeasonalTheme(SeasonalThemeMode.AUTO, LocalDate.of(2026, 10, 31)))
        assertEquals(SeasonalTheme.NONE, resolveSeasonalTheme(SeasonalThemeMode.AUTO, LocalDate.of(2026, 11, 1)))
    }

    @Test
    fun manualModeOverridesCalendarAndOffAlwaysDisables() {
        val winterDate = LocalDate.of(2026, 1, 5)
        assertEquals(SeasonalTheme.HALLOWEEN, resolveSeasonalTheme(SeasonalThemeMode.HALLOWEEN, winterDate))
        assertEquals(SeasonalTheme.NONE, resolveSeasonalTheme(SeasonalThemeMode.OFF, LocalDate.of(2026, 10, 20)))
    }

    @Test
    fun unknownStoredModeFallsBackToAutomatic() {
        assertEquals(SeasonalThemeMode.AUTO, SeasonalThemeMode.fromStorage("unknown"))
    }
}

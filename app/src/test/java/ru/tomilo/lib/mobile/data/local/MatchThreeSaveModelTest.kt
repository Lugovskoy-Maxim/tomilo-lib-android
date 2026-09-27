package ru.tomilo.lib.mobile.data.local

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MatchThreeSaveModelTest {
    private val json = Json

    @Test
    fun olderSaveWithoutActiveSessionRemainsReadable() {
        val save = json.decodeFromString<MatchThreeSave>(
            """{"lives":3,"lifeStamp":123,"records":[],"customLevels":[]}""",
        )

        assertEquals(3, save.lives)
        assertNull(save.activeSession)
    }

    @Test
    fun activeGuestSessionRoundTripsAllProgress() {
        val session = MatchThreeLocalSession(
            level = 7,
            board = List(64) { it % 5 },
            obstacles = List(64) { if (it == 11) 2 else -1 },
            moves = 13,
            collected = 9,
            target = 24,
            targetColor = 3,
            hammer = 1,
            rainbow = 0,
            shuffle = 1,
        )
        val save = MatchThreeSave(lives = 4, lifeStamp = 123, activeSession = session)

        assertEquals(save, json.decodeFromString<MatchThreeSave>(json.encodeToString(save)))
    }
}

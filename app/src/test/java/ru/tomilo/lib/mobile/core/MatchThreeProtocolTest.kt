package ru.tomilo.lib.mobile.core

import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import ru.tomilo.lib.mobile.data.api.PillMatchActionDto

class MatchThreeProtocolTest {
    @Test fun equalColoredReplacementsStillPopOnlyConfirmedCells() {
        val before = List(64) { it % 5 }
        val previous = MatchThreeMotion.settle(before)
        val wave = MatchThreeMotion.remoteWave(before, setOf(8), before, listOf(0 to 8, null to 0), previous)
        assertEquals(setOf(8), wave.popping.filter { it.pop }.map { it.cell }.toSet())
        assertEquals(previous.first { it.cell == 0 }.id, wave.landing.first { it.cell == 8 }.id)
        assertTrue(wave.landing.first { it.cell == 0 }.enter)
        assertEquals(previous.first { it.cell == 24 }.id, wave.landing.first { it.cell == 24 }.id)
        assertEquals(64, wave.landing.map { it.cell }.toSet().size)
    }
    @Test fun shellDamageNeverInventsAGemExplosion() {
        val board = List(64) { it % 5 }
        val wave = MatchThreeMotion.remoteWave(board, emptySet(), board, emptyList(), MatchThreeMotion.settle(board))
        assertTrue(wave.popping.none { it.pop })
        assertTrue(wave.landing.all { it.cell == it.fromCell && !it.enter })
    }
    @Test fun squareAndRuneHintsRespectBlockedCells() {
        val board = MutableList(64) { (it / 8 + it % 8) % 5 }
        board[0] = 0; board[1] = 0; board[8] = 0; board[9] = 1; board[10] = 0
        val obstacles = (0 until 64).filter { it !in setOf(0, 1, 8, 9, 10) }.associateWith { MatchThreeEngine.Obstacle.ROCK }
        val squareSwap = board.toMutableList().also { it[9] = board[10]; it[10] = board[9] }
        assertTrue(MatchThreeEngine.matches(squareSwap, obstacles).isEmpty())
        assertEquals(9 to 10, MatchThreeEngine.serverHint(board, obstacles, emptyList()))
        val sealed = (0 until 64).filter { it !in setOf(0, 1) }.associateWith { MatchThreeEngine.Obstacle.CHAIN }
        val runes = MutableList<String?>(64) { null }.also { it[0] = "wisp" }
        assertEquals(0 to 1, MatchThreeEngine.serverHint(board, sealed, runes))
        assertNull(MatchThreeEngine.serverHint(board, sealed + (1 to MatchThreeEngine.Obstacle.ICE), runes))
    }
    @Test fun protocolReadsLayersRunesFlightsAndLegacyResponses() {
        val json = Json { ignoreUnknownKeys = true }
        val action = json.decodeFromString<PillMatchActionDto>("""{"session":{"specials":["wisp",null],"obstacleHealth":[0,2]},"frames":[{"clearedCells":[8],"damagedCells":[9],"falls":[{"from":null,"to":0}],"flights":[{"from":1,"to":9}],"afterSpecials":["row"],"combo":"test"}]}""")
        assertEquals("wisp", action.session.specials[0]); assertEquals(2, action.session.obstacleHealth[1])
        assertEquals(listOf(9), action.frames[0].damagedCells); assertNull(action.frames[0].falls[0].from)
        assertEquals(9, action.frames[0].flights[0].to)
        assertTrue(json.decodeFromString<PillMatchActionDto>("""{"session":{}}""").frames.isEmpty())
    }
}

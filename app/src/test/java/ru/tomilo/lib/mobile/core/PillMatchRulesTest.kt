package ru.tomilo.lib.mobile.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PillMatchRulesTest {
    @Test
    fun initialBoardHasNoFreeMatchAndStillHasAPlayableMove() {
        assertEquals(64, PillMatchRules.initialBoard.size)
        assertTrue(PillMatchRules.findMatches(PillMatchRules.initialBoard).isEmpty())
        assertNotNull(PillMatchRules.swap(PillMatchRules.initialBoard, first = 9, second = 17))
    }

    @Test
    fun onlyAcceptsAdjacentSwapsThatMakeALineThroughTheSwappedCells() {
        assertNull(PillMatchRules.swap(PillMatchRules.initialBoard, first = 10, second = 11))
        assertNull(PillMatchRules.swap(PillMatchRules.initialBoard, first = 9, second = 18))
        assertTrue(PillMatchRules.isAdjacent(9, 17))
        assertFalse(PillMatchRules.isAdjacent(9, 18))
    }

    @Test
    fun clearsMatchedCellsAndCountsOnlyCoralPills() {
        val move = PillMatchRules.swap(PillMatchRules.initialBoard, first = 9, second = 17)

        assertNotNull(move)
        assertEquals(setOf(17, 18, 19), move?.matchedCells)
        assertEquals(3, move?.coralMatched)
        assertTrue(PillMatchRules.findMatches(move!!.board).isEmpty())
    }
}

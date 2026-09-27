package ru.tomilo.lib.mobile.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MatchThreeEngineTest {
    @Test
    fun longLinesAndCrossesCountEachCellOnce() {
        val board = List(64) { (it / 8 + it % 8) % 5 }.toMutableList()
        (8..12).forEach { board[it] = 4 }
        listOf(2, 10, 18, 26).forEach { board[it] = 4 }
        val matched = MatchThreeEngine.matches(board, emptyMap())
        assertTrue(matched.containsAll(setOf(8, 9, 10, 11, 12, 2, 18, 26)))
        assertEquals(8, matched.intersect(setOf(8, 9, 10, 11, 12, 2, 18, 26)).size)
    }

    @Test
    fun replacementUsesOneDeterministicStreamAndRejectsBadObstacleIndices() {
        val board = MatchThreeEngine.createBoard(51, emptyMap())
        val first = MatchThreeEngine.clearAt(board, emptyMap(), 17, 0, 912)
        val repeated = MatchThreeEngine.clearAt(board, emptyMap(), 17, 0, 912)
        assertEquals(first, repeated)
        assertTrue(runCatching { MatchThreeEngine.createBoard(1, mapOf(64 to MatchThreeEngine.Obstacle.ROCK)) }.isFailure)
    }

    @Test
    fun hammerRemovesEachObstacleKindWithoutCountingAGem() {
        val board = MatchThreeEngine.createBoard(71, emptyMap())
        MatchThreeEngine.Obstacle.entries.forEach { obstacle ->
            val result = MatchThreeEngine.clearAt(board, mapOf(17 to obstacle), 17, board[17], 123)
            assertNotNull(result)
            assertEquals(0, result?.collected)
            assertEquals(0, result?.cleared)
            assertTrue(17 !in result!!.obstacles)
        }
    }

    @Test
    fun gravityStopsAtObstacleBoundaries() {
        val barriers = mapOf(24 to MatchThreeEngine.Obstacle.ROCK)
        val board = MatchThreeEngine.createBoard(207, barriers)
        val lowerSegmentBefore = listOf(board[32], board[40], board[48], board[56])
        val result = MatchThreeEngine.clearAt(board, barriers, 16, targetColor = 4, seed = 13)!!

        assertEquals("gems below the rock stay in their own gravity segment", lowerSegmentBefore, listOf(result.board[32], result.board[40], result.board[48], result.board[56]))
        assertEquals(MatchThreeEngine.Obstacle.ROCK, result.obstacles[24])
    }

    @Test
    fun clearingNonTargetGemDoesNotIncrementTargetCollection() {
        val board = MatchThreeEngine.createBoard(302, emptyMap())
        val index = board.indices.first { board[it] != 4 }
        val result = MatchThreeEngine.clearAt(board, emptyMap(), index, targetColor = 4, seed = 5)!!

        assertEquals(0, result.collected)
        assertTrue(result.matchedCells.contains(index))
    }

    @Test
    fun cascadesResolveDeterministicallyAfterInitialClear() {
        val color = 0
        val board = List(64) { index -> (index / 8 + index % 8) % MatchThreeEngine.COLORS }.toMutableList().apply {
            this[0] = color; this[1] = 1; this[2] = color
        }.toList()
        assertTrue(MatchThreeEngine.matches(board, emptyMap()).isEmpty())
        val seed = generateSequence(0, Int::inc).first { Random(it).nextInt(MatchThreeEngine.COLORS) == color }
        val first = MatchThreeEngine.clearAt(board, emptyMap(), index = 57, targetColor = color, seed = seed)!!
        assertTrue(first.matchedCells.containsAll(setOf(0, 1, 2, 57)))
        assertTrue(first.cleared >= 4)
        val repeated = MatchThreeEngine.clearAt(board, emptyMap(), index = 57, targetColor = color, seed = seed)
        assertEquals(first, repeated)
    }
    @Test
    fun hammerRemovesObstacleWithoutCountingItAsCollectedGem() {
        val board = List(MatchThreeEngine.SIZE * MatchThreeEngine.SIZE) { 0 }
        val result = MatchThreeEngine.clearAt(
            board = board,
            obstacles = mapOf(10 to MatchThreeEngine.Obstacle.ROCK),
            index = 10,
            targetColor = 0,
            seed = 1,
        )

        assertNotNull(result)
        assertEquals(0, result?.collected)
        assertEquals(0, result?.cleared)
        assertEquals(emptyMap<Int, MatchThreeEngine.Obstacle>(), result?.obstacles)
    }

    @Test
    fun hammerClearsGemWhenCellHasNoObstacle() {
        val board = MatchThreeEngine.createBoard(seed = 29, obstacles = emptyMap())
        val result = MatchThreeEngine.clearAt(board, emptyMap(), 10, targetColor = 1, seed = 2)

        assertNotNull(result)
        assertEquals(1, result?.cleared)
        assertEquals(setOf(10), result?.matchedCells)
    }

    @Test
    fun swapsMustBeAdjacentAndWithinBoard() {
        val board = MatchThreeEngine.createBoard(42, emptyMap())

        assertNull(MatchThreeEngine.swap(board, emptyMap(), 0, 2, targetColor = 0, seed = 1))
        assertNull(MatchThreeEngine.swap(board, emptyMap(), 63, 64, targetColor = 0, seed = 1))
    }

    @Test
    fun swipeDirectionUsesThresholdAndDominantAxisWithoutWrappingRows() {
        assertNull(MatchThreeEngine.swipeTarget(10, deltaX = 8f, deltaY = 0f, threshold = 10f))
        assertEquals(11, MatchThreeEngine.swipeTarget(10, deltaX = 18f, deltaY = 7f, threshold = 10f))
        assertEquals(9, MatchThreeEngine.swipeTarget(10, deltaX = -18f, deltaY = 4f, threshold = 10f))
        assertEquals(18, MatchThreeEngine.swipeTarget(10, deltaX = 3f, deltaY = 16f, threshold = 10f))
        assertEquals(2, MatchThreeEngine.swipeTarget(10, deltaX = 5f, deltaY = -17f, threshold = 10f))
        assertNull(MatchThreeEngine.swipeTarget(7, deltaX = 20f, deltaY = 0f, threshold = 10f))
        assertNull(MatchThreeEngine.swipeTarget(0, deltaX = 0f, deltaY = -20f, threshold = 10f))
    }

    @Test
    fun generatedAndShuffledBoardsHaveNoFreeMatchesAndAtLeastOneLegalMove() {
        for (levelNumber in 1..120) {
            val level = MatchThreeEngine.level(levelNumber)
            val board = MatchThreeEngine.createBoard(level.seed, level.obstacles)
            assertEquals("generated board $levelNumber starts with a match", emptySet<Int>(), MatchThreeEngine.matches(board, level.obstacles))
            assertTrue("generated board $levelNumber has no legal move", MatchThreeEngine.hasLegalMove(board, level.obstacles))

            val shuffled = MatchThreeEngine.shuffle(board, level.obstacles, seed = level.seed + 1)
            assertEquals("shuffled board $levelNumber starts with a match", emptySet<Int>(), MatchThreeEngine.matches(shuffled, level.obstacles))
            assertTrue("shuffled board $levelNumber has no legal move", MatchThreeEngine.hasLegalMove(shuffled, level.obstacles))
        }
    }

    @Test
    fun completelyBlockedLayoutFailsFastInsteadOfSearchingForever() {
        val blocked = (0 until MatchThreeEngine.SIZE * MatchThreeEngine.SIZE)
            .associateWith { MatchThreeEngine.Obstacle.ROCK }

        val result = runCatching { MatchThreeEngine.createBoard(seed = 7, obstacles = blocked) }

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("возможных ходов"))
    }

    @Test
    fun obstacleGeneratorIsSeededAndHonorsRequestedDensity() {
        val first = MatchThreeEngine.generateObstacles(seed = 991, count = 12)
        val repeated = MatchThreeEngine.generateObstacles(seed = 991, count = 12)

        assertEquals(first, repeated)
        assertEquals(12, first.size)
        assertTrue(first.keys.none { it in setOf(0, 7, 56, 63, 27, 28, 35, 36) })
        assertEquals(56, MatchThreeEngine.generateObstacles(seed = 1, count = 99).size)
        assertEquals(0, MatchThreeEngine.generateObstacles(seed = 1, count = -5).size)
    }

    @Test
    fun progressionDoesNotSkipUncompletedLevelsAndResumesAtFrontier() {
        assertEquals(1, MatchThreeEngine.nextUnlockedLevel(emptyList()))
        assertEquals(3, MatchThreeEngine.nextUnlockedLevel(listOf(1, 2, 5, 7)))
        assertEquals(6, MatchThreeEngine.nextUnlockedLevel(listOf(1, 2, 3, 4, 5)))
    }
}

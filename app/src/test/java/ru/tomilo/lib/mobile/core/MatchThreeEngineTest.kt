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
            val resolved = result!!
            assertEquals(0, resolved.collected)
            assertEquals(0, resolved.cleared)
            assertEquals(board, resolved.board)
            assertTrue(resolved.steps.isEmpty())
            assertTrue(17 !in resolved.obstacles)
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

        val resolved = result!!
        assertEquals(0, resolved.collected)
        assertEquals(0, resolved.cleared)
        assertEquals(board, resolved.board)
        assertTrue(resolved.steps.isEmpty())
        assertEquals(0, resolved.board[10])
        assertEquals(emptyMap<Int, MatchThreeEngine.Obstacle>(), resolved.obstacles)
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
    fun hintPointsAtASwapThatActuallyClears() {
        val level = MatchThreeEngine.level(3)
        val board = MatchThreeEngine.createBoard(level.seed, level.obstacles)
        val snapshot = board.toList()
        val hint = MatchThreeEngine.hint(board, level.obstacles)
        assertNotNull(hint)
        val move = MatchThreeEngine.swap(board, level.obstacles, hint!!.first, hint.second, level.color, seed = 1)
        assertNotNull(move)
        assertEquals(snapshot, board)
        assertTrue(hint.first in move!!.matchedCells || hint.second in move.matchedCells)
        assertTrue(move.cleared >= 3)
        assertEquals(move.board, move.steps.last().board)
        assertEquals(move.obstacles, move.steps.last().obstacles)
        val swapped = board.toMutableList()
        val held = swapped[hint.first]
        swapped[hint.first] = swapped[hint.second]
        swapped[hint.second] = held
        var look = swapped.toList()
        var gems = MatchThreeMotion.settle(look)
        for (step in move.steps) {
            val wave = MatchThreeMotion.wave(look, step.matched, step.board, step.obstacles, gems)
            assertEquals(wave.landing.size, wave.landing.map { it.cell }.distinct().size)
            wave.landing.forEach { sprite ->
                assertEquals(step.board[sprite.cell], sprite.color)
                if (sprite.enter) {
                    assertEquals(-1, sprite.fromCell)
                    assertTrue(sprite.spawnRows >= 1)
                } else {
                    assertEquals(look[sprite.fromCell], sprite.color)
                    assertEquals(sprite.fromCell % MatchThreeEngine.SIZE, sprite.cell % MatchThreeEngine.SIZE)
                    assertTrue(sprite.fromCell <= sprite.cell)
                }
            }
            for (col in 0 until MatchThreeEngine.SIZE) {
                val froms = wave.landing.filter { !it.enter && it.cell % MatchThreeEngine.SIZE == col }.sortedBy { it.cell }.map { it.fromCell }
                assertEquals(froms.sorted(), froms)
            }
            val popped = wave.popping.filter { it.pop }.map { it.cell }.toSet()
            assertEquals(step.matched.filter { look[it] >= 0 }.toSet(), popped)
            look = step.board
            gems = wave.landing.map { it.copy(fromCell = it.cell, pop = false, enter = false, spawnRows = 0) }
        }
        assertEquals(move.board, look)
    }

    @Test
    fun cascadeStepsEndOnTheResolvedBoard() {
        val board = MatchThreeEngine.createBoard(seed = 29, obstacles = emptyMap())
        val result = MatchThreeEngine.clearAt(board, emptyMap(), 10, targetColor = 1, seed = 2)!!
        assertEquals(result.board, result.steps.last().board)
        assertEquals(result.matchedCells, result.steps.flatMap { it.matched }.toSet())
        assertEquals(result.obstacles, result.steps.last().obstacles)
    }

    @Test
    fun gravityWaveDropsSurvivorsInOrderAndSpawnsAboveThem() {
        val size = MatchThreeEngine.SIZE
        val rocks = (0 until size * size).filter { it % size != 0 }.associateWith { MatchThreeEngine.Obstacle.ROCK }
        val before = MutableList(size * size) { 0 }
        val column = (0 until size).map { it * size }
        listOf(0, 1, 2, 3, 4, 0, 1, 2).forEachIndexed { row, color -> before[column[row]] = color }
        val cleared = 6 * size
        val snapshot = before.toList()
        val result = MatchThreeEngine.clearAt(before, rocks, cleared, targetColor = 4, seed = 9)!!
        assertEquals(snapshot, before)
        assertEquals(1, result.steps.size)
        assertEquals(result.board, result.steps.last().board)
        assertEquals(result.obstacles, result.steps.last().obstacles)
        assertEquals(rocks, result.obstacles)
        assertEquals(before[0], result.board[size])
        assertEquals(before[size], result.board[2 * size])
        assertEquals(before[2 * size], result.board[3 * size])
        assertEquals(before[3 * size], result.board[4 * size])
        assertEquals(before[4 * size], result.board[5 * size])
        assertEquals(before[5 * size], result.board[6 * size])
        assertEquals(before[7 * size], result.board[7 * size])
        assertTrue(result.board[0] in 0 until MatchThreeEngine.COLORS)
        val step = result.steps.single()
        val wave = MatchThreeMotion.wave(before, step.matched, step.board, rocks, MatchThreeMotion.settle(before))
        val landed = wave.landing.associateBy { it.cell }
        assertEquals(-1, landed.getValue(0).fromCell)
        assertTrue(landed.getValue(0).enter)
        assertEquals(result.board[0], landed.getValue(0).color)
        assertEquals(0, landed.getValue(size).fromCell)
        assertEquals(before[0], landed.getValue(size).color)
        assertEquals(7 * size, landed.getValue(7 * size).fromCell)
        assertEquals(before[7 * size], landed.getValue(7 * size).color)
        assertTrue(wave.popping.any { it.pop && it.cell == cleared })
    }

    @Test
    fun illegalAdjacentSwapAndObstacleSwipeReturnNoMove() {
        var illegalFrom = -1
        var illegalTo = -1
        var illegalBoard: List<Int> = emptyList()
        var illegalObstacles: Map<Int, MatchThreeEngine.Obstacle> = emptyMap()
        for (levelNumber in 1..30) {
            val level = MatchThreeEngine.level(levelNumber)
            val board = MatchThreeEngine.createBoard(level.seed, level.obstacles)
            val hint = MatchThreeEngine.hint(board, level.obstacles) ?: continue
            for (from in board.indices) {
                if (from in level.obstacles) continue
                for (to in listOf(from + 1, from + MatchThreeEngine.SIZE)) {
                    if (to !in board.indices || to in level.obstacles || !MatchThreeEngine.adjacent(from, to)) continue
                    if (from == hint.first && to == hint.second) continue
                    if (MatchThreeEngine.swap(board, level.obstacles, from, to, level.color, seed = 3) == null) {
                        illegalFrom = from
                        illegalTo = to
                        illegalBoard = board
                        illegalObstacles = level.obstacles
                        break
                    }
                }
                if (illegalFrom >= 0) break
            }
            if (illegalFrom >= 0) break
        }
        assertTrue(illegalFrom >= 0)
        val snapshot = illegalBoard.toList()
        assertNull(MatchThreeEngine.swap(illegalBoard, illegalObstacles, illegalFrom, illegalTo, targetColor = 0, seed = 3))
        assertEquals(snapshot, illegalBoard)
        val open = MatchThreeEngine.createBoard(1, emptyMap())
        val openCopy = open.toList()
        assertNull(MatchThreeEngine.swap(open, mapOf(1 to MatchThreeEngine.Obstacle.ROCK), 0, 1, targetColor = 0, seed = 1))
        assertNull(MatchThreeEngine.swap(open, mapOf(0 to MatchThreeEngine.Obstacle.ICE), 0, 1, targetColor = 0, seed = 1))
        assertNull(MatchThreeEngine.swap(open, mapOf(1 to MatchThreeEngine.Obstacle.CHAIN), 0, 1, targetColor = 0, seed = 1))
        assertEquals(openCopy, open)
    }

    @Test
    fun iceAndChainBreakFromAnAdjacentClearAndRockDoesNot() {
        val board = MatchThreeEngine.createBoard(19, emptyMap())
        val snapshot = board.toList()
        val obstacles = mapOf(
            9 to MatchThreeEngine.Obstacle.ICE,
            11 to MatchThreeEngine.Obstacle.CHAIN,
            2 to MatchThreeEngine.Obstacle.ROCK,
            40 to MatchThreeEngine.Obstacle.ROCK,
        )
        val result = MatchThreeEngine.clearAt(board, obstacles, 10, targetColor = board[10], seed = 4)!!
        assertEquals(snapshot, board)
        val first = result.steps.first()
        assertTrue(10 in first.matched)
        assertTrue(9 !in first.obstacles)
        assertTrue(11 !in first.obstacles)
        assertEquals(MatchThreeEngine.Obstacle.ROCK, first.obstacles[2])
        assertEquals(MatchThreeEngine.Obstacle.ROCK, first.obstacles[40])
        assertEquals(result.board, result.steps.last().board)
        assertEquals(result.obstacles, result.steps.last().obstacles)
        val hammer = MatchThreeEngine.clearAt(board, mapOf(10 to MatchThreeEngine.Obstacle.ROCK, 11 to MatchThreeEngine.Obstacle.ICE), 10, board[10], 3)!!
        assertEquals(0, hammer.collected)
        assertEquals(0, hammer.cleared)
        assertTrue(hammer.steps.isEmpty())
        assertEquals(snapshot, hammer.board)
        assertEquals(MatchThreeEngine.Obstacle.ICE, hammer.obstacles[11])
        assertTrue(10 !in hammer.obstacles)
    }

    @Test
    fun progressionDoesNotSkipUncompletedLevelsAndResumesAtFrontier() {
        assertEquals(1, MatchThreeEngine.nextUnlockedLevel(emptyList()))
        assertEquals(3, MatchThreeEngine.nextUnlockedLevel(listOf(1, 2, 5, 7)))
        assertEquals(6, MatchThreeEngine.nextUnlockedLevel(listOf(1, 2, 3, 4, 5)))
    }

    @Test
    fun serverOpeningMatchesPublishedGenerator() {
        val empty = IntArray(64) { -1 }
        assertEquals(
            listOf(3, 0, 2, 4, 4, 1, 3, 3, 4, 4, 2, 2, 0, 2, 1, 0, 2, 0, 1, 3, 1, 0, 0, 2, 2, 3, 1, 1, 3, 2, 3, 0, 3, 2, 3, 4, 1, 3, 0, 1, 2, 1, 0, 3, 2, 0, 1, 0, 3, 4, 1, 4, 3, 2, 1, 3, 4, 4, 3, 1, 4, 4, 0, 0),
            MatchThreeEngine.serverOpening(1, empty),
        )
        assertEquals(
            listOf(2, 2, 1, 3, 0, 1, 3, 1, 0, 0, 1, 0, 2, 2, 3, 3, 4, 4, 0, 3, 0, 1, 0, 0, 0, 3, 3, 0, 0, 3, 2, 1, 2, 2, 3, 2, 2, 0, 1, 2, 2, 1, 2, 4, 4, 1, 4, 4, 1, 3, 0, 2, 1, 2, 1, 4, 1, 0, 1, 4, 1, 2, 2, 3),
            MatchThreeEngine.serverOpening(810, empty),
        )
        val rocks = IntArray(64) { -1 }.also { cells ->
            cells[3] = 0
            cells[12] = 1
            cells[20] = 2
            cells[45] = 0
        }
        assertEquals(
            listOf(4, 2, 3, 4, 0, 3, 2, 2, 1, 2, 3, 1, 4, 4, 2, 3, 2, 4, 4, 2, 0, 0, 3, 3, 1, 4, 1, 4, 4, 2, 4, 4, 1, 3, 0, 4, 2, 0, 4, 1, 4, 1, 3, 1, 3, 3, 0, 3, 1, 0, 4, 0, 3, 0, 0, 3, 1, 0, 1, 0, 0, 3, 4, 1),
            MatchThreeEngine.serverOpening(431, rocks),
        )
        assertTrue(MatchThreeEngine.serverBoardPlayable(1, empty))
    }

    @Test
    fun generatedLayoutStaysPublishable() {
        val generated = MatchThreeEngine.generatePlayable(seed = 810, count = 8, kind = null)
        assertNotNull(generated)
        assertTrue(MatchThreeEngine.acceptsLayout(generated!!.first, generated.second))
        assertEquals(8, generated.second.size)
        val rocks = MatchThreeEngine.generateObstacles(seed = 15, count = 5, kind = MatchThreeEngine.Obstacle.ROCK)
        assertEquals(5, rocks.size)
        assertTrue(rocks.values.all { it == MatchThreeEngine.Obstacle.ROCK })
    }
}

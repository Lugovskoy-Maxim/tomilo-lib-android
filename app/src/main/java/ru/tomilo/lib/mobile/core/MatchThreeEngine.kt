package ru.tomilo.lib.mobile.core

import kotlin.random.Random

/** Deterministic game rules kept independent from Compose. */
object MatchThreeEngine {
    const val SIZE = 8
    const val COLORS = 5

    enum class Obstacle { ROCK, ICE, CHAIN }
    data class Level(val number: Int, val target: Int, val moves: Int, val color: Int, val obstacles: Map<Int, Obstacle> = emptyMap(), val seed: Int = number)
    data class Move(val board: List<Int>, val obstacles: Map<Int, Obstacle>, val collected: Int, val cleared: Int)

    fun createBoard(seed: Int, obstacles: Map<Int, Obstacle>): List<Int> {
        repeat(40) { attempt ->
            val random = Random(seed + attempt)
            val board = MutableList(SIZE * SIZE) { random.nextInt(COLORS) }
            for (i in board.indices) {
                var attempts = 0
                while (attempts < COLORS * 2 && formsLine(board, i, obstacles)) {
                    board[i] = random.nextInt(COLORS)
                    attempts++
                }
            }
            if (hasLegalMove(board, obstacles)) return board
        }
        return List(SIZE * SIZE) { (it + seed) % COLORS }
    }

    fun level(number: Int): Level {
        val n = number.coerceAtLeast(1)
        val blocks = (n / 2).coerceIn(0, 12)
        val obstacles = linkedMapOf<Int, Obstacle>()
        val random = Random(n * 7_919)
        repeat(blocks) {
            var index = random.nextInt(SIZE * SIZE)
            while (index in obstacles || index in setOf(0, 7, 56, 63, 27, 28, 35, 36)) index = random.nextInt(SIZE * SIZE)
            obstacles[index] = when (it % 3) { 0 -> Obstacle.ROCK; 1 -> Obstacle.ICE; else -> Obstacle.CHAIN }
        }
        return Level(n, target = (14 + n * 2).coerceAtMost(40), moves = (24 - n / 3).coerceAtLeast(15), color = (n - 1) % COLORS, obstacles = obstacles, seed = n * 431)
    }

    fun swap(board: List<Int>, obstacles: Map<Int, Obstacle>, from: Int, to: Int, targetColor: Int, seed: Int): Move? {
        if (board.size != SIZE * SIZE || !adjacent(from, to) || from in obstacles || to in obstacles) return null
        val next = board.toMutableList()
        val value = next[from]; next[from] = next[to]; next[to] = value
        val matched = matches(next, obstacles)
        if (matched.isEmpty() || from !in matched && to !in matched) return null
        return resolve(next, obstacles, matched, targetColor, seed)
    }

    fun clearAt(board: List<Int>, obstacles: Map<Int, Obstacle>, index: Int, targetColor: Int, seed: Int): Move? {
        if (index !in board.indices) return null
        val nextObstacles = obstacles.toMutableMap().apply { remove(index) }
        val next = board.toMutableList()
        return resolve(next, nextObstacles, setOf(index), targetColor, seed)
    }

    fun clearColor(board: List<Int>, obstacles: Map<Int, Obstacle>, color: Int, targetColor: Int, seed: Int): Move {
        val matches = board.indices.filter { it !in obstacles && board[it] == color }.toSet()
        return resolve(board.toMutableList(), obstacles, matches, targetColor, seed)
    }

    fun shuffle(board: List<Int>, obstacles: Map<Int, Obstacle>, seed: Int): List<Int> {
        val random = Random(seed)
        val movable = board.indices.filter { it !in obstacles }
        repeat(40) {
            val next = board.toMutableList()
            val values = movable.map { board[it] }.shuffled(random)
            movable.forEachIndexed { i, index -> next[index] = values[i] }
            if (hasLegalMove(next, obstacles)) return next
        }
        return createBoard(seed + 1, obstacles)
    }

    private fun hasLegalMove(board: List<Int>, obstacles: Map<Int, Obstacle>): Boolean {
        for (from in board.indices) {
            if (from in obstacles) continue
            for (to in listOf(from + 1, from + SIZE)) {
                if (to !in board.indices || to in obstacles || !adjacent(from, to)) continue
                val swapped = board.toMutableList()
                val held = swapped[from]; swapped[from] = swapped[to]; swapped[to] = held
                val matched = matches(swapped, obstacles)
                if (from in matched || to in matched) return true
            }
        }
        return false
    }

    private fun resolve(board: MutableList<Int>, obstacles: Map<Int, Obstacle>, initial: Set<Int>, targetColor: Int, seed: Int): Move {
        val nextObstacles = obstacles.toMutableMap()
        var cells = initial
        var collected = 0
        var cleared = 0
        var loopSeed = seed
        repeat(8) {
            if (cells.isEmpty()) return@repeat
            collected += cells.count { board[it] == targetColor }
            cleared += cells.size
            val touching = linkedSetOf<Int>()
            for (cell in cells) for (near in neighbors(cell)) if (near in nextObstacles) touching += near
            touching.forEach { index ->
                when (nextObstacles[index]) {
                    Obstacle.ROCK -> Unit
                    Obstacle.ICE, Obstacle.CHAIN -> nextObstacles.remove(index)
                    null -> Unit
                }
            }
            cells.forEach { board[it] = -1 }
            for (column in 0 until SIZE) {
                var segmentEnd = SIZE - 1
                while (segmentEnd >= 0) {
                    if (column + segmentEnd * SIZE in nextObstacles) { segmentEnd--; continue }
                    var segmentStart = segmentEnd
                    while (segmentStart > 0 && column + (segmentStart - 1) * SIZE !in nextObstacles) segmentStart--
                    val segment = (segmentStart..segmentEnd).map { column + it * SIZE }
                    val values = segment.map { board[it] }.filter { it >= 0 }
                    val missing = segment.size - values.size
                    segment.forEachIndexed { i, index -> board[index] = if (i < missing) Random(loopSeed + index * 97).nextInt(COLORS) else values[i - missing] }
                    segmentEnd = segmentStart - 1
                }
            }
            cells = matches(board, nextObstacles)
            loopSeed++
        }
        return Move(board, nextObstacles, collected, cleared)
    }

    private fun matches(board: List<Int>, obstacles: Map<Int, Obstacle>): Set<Int> {
        val found = linkedSetOf<Int>()
        for (row in 0 until SIZE) for (col in 0 until SIZE) {
            val start = row * SIZE + col
            if (start in obstacles || board[start] < 0) continue
            val color = board[start]
            if (col <= SIZE - 3 && (1..2).all { start + it !in obstacles && board[start + it] == color }) found += start..start + 2
            if (row <= SIZE - 3 && (1..2).all { start + it * SIZE !in obstacles && board[start + it * SIZE] == color }) found += listOf(start, start + SIZE, start + 2 * SIZE)
        }
        return found
    }

    private fun formsLine(board: List<Int>, i: Int, obstacles: Map<Int, Obstacle>): Boolean {
        val row = i / SIZE; val col = i % SIZE; val color = board[i]
        return (col >= 2 && i - 1 !in obstacles && i - 2 !in obstacles && board[i - 1] == color && board[i - 2] == color) ||
            (row >= 2 && i - SIZE !in obstacles && i - 2 * SIZE !in obstacles && board[i - SIZE] == color && board[i - 2 * SIZE] == color)
    }

    fun adjacent(a: Int, b: Int): Boolean = a in 0 until SIZE * SIZE && b in 0 until SIZE * SIZE && kotlin.math.abs(a / SIZE - b / SIZE) + kotlin.math.abs(a % SIZE - b % SIZE) == 1
    private fun neighbors(i: Int): List<Int> = buildList { if (i / SIZE > 0) add(i - SIZE); if (i / SIZE < SIZE - 1) add(i + SIZE); if (i % SIZE > 0) add(i - 1); if (i % SIZE < SIZE - 1) add(i + 1) }
}

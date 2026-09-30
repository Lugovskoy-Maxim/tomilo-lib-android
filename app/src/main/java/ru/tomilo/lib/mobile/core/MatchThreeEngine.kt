package ru.tomilo.lib.mobile.core

import kotlin.math.floor
import kotlin.random.Random

/** Deterministic game rules kept independent from Compose. */
object MatchThreeEngine {
    const val SIZE = 8
    const val COLORS = 5

    enum class Obstacle { ROCK, ICE, CHAIN }
    data class Level(val number: Int, val target: Int, val moves: Int, val color: Int, val obstacles: Map<Int, Obstacle> = emptyMap(), val seed: Int = number)
    data class CascadeStep(val matched: Set<Int>, val board: List<Int>, val obstacles: Map<Int, Obstacle>)
    data class Move(
        val board: List<Int>,
        val obstacles: Map<Int, Obstacle>,
        val collected: Int,
        val cleared: Int,
        val matchedCells: Set<Int> = emptySet(),
        val steps: List<CascadeStep> = emptyList(),
    )

    fun createBoard(seed: Int, obstacles: Map<Int, Obstacle>): List<Int> {
        require(obstacles.keys.all { it in 0 until SIZE * SIZE }) { "Препятствие находится за пределами поля" }
        val safeObstacles = obstacles
        repeat(512) { attempt ->
            val random = Random(seed + attempt)
            val board = MutableList(SIZE * SIZE) { random.nextInt(COLORS) }
            for (i in board.indices) {
                var attempts = 0
                while (attempts < COLORS * 2 && formsLine(board, i, safeObstacles)) {
                    board[i] = random.nextInt(COLORS)
                    attempts++
                }
            }
            if (matches(board, safeObstacles).isEmpty() && hasLegalMove(board, safeObstacles)) return board
        }
        throw IllegalArgumentException("Расстановка препятствий не оставляет возможных ходов")
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

    fun nextUnlockedLevel(completedLevels: Collection<Int>): Int {
        val completed = completedLevels.toHashSet()
        var level = 1
        while (level in completed && level < Int.MAX_VALUE) level++
        return level
    }

    fun generateObstacles(seed: Int, count: Int, kind: Obstacle? = null): Map<Int, Obstacle> {
        val reserved = setOf(0, 7, 56, 63, 27, 28, 35, 36)
        val candidates = (0 until SIZE * SIZE).filterNot { it in reserved }.shuffled(Random(seed))
        val random = Random(seed xor 0x5F3759DF)
        return candidates.take(count.coerceIn(0, candidates.size)).sorted().associateWith {
            kind ?: Obstacle.entries[random.nextInt(Obstacle.entries.size)]
        }
    }

    /** Client filler and the server opener both leave a legal move, and the field stays within the publish cap. */
    fun acceptsLayout(seed: Int, obstacles: Map<Int, Obstacle>): Boolean {
        if (obstacles.size > 24 || obstacles.keys.any { it !in 0 until SIZE * SIZE }) return false
        if (runCatching { createBoard(seed, obstacles) }.isFailure) return false
        return serverBoardPlayable(seed, IntArray(SIZE * SIZE) { index -> obstacles[index]?.ordinal ?: -1 })
    }

    fun generatePlayable(seed: Int, count: Int, kind: Obstacle? = null): Pair<Int, Map<Int, Obstacle>>? {
        val base = seed.coerceAtLeast(0)
        repeat(40) { step ->
            val next = (base.toLong() + step).toInt().let { if (it < 0) it and Int.MAX_VALUE else it }
            val map = generateObstacles(next, count, kind)
            if (acceptsLayout(next, map)) return next to map
        }
        return null
    }

    internal fun serverOpening(seed: Int, obstacles: IntArray): List<Int> = serverCreate(seed, obstacles).toList()

    internal fun serverBoardPlayable(seed: Int, obstacles: IntArray): Boolean {
        if (obstacles.size != SIZE * SIZE) return false
        val board = serverCreate(seed, obstacles)
        return serverMatches(board, obstacles).isEmpty() && serverHasMove(board, obstacles)
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
        val cells = if (index in obstacles) emptySet() else setOf(index)
        return resolve(next, nextObstacles, cells, targetColor, seed)
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
            if (matches(next, obstacles).isEmpty() && hasLegalMove(next, obstacles)) return next
        }
        return createBoard(seed + 1, obstacles)
    }

    /** First swap that clears a line, or null when the board is stuck. */
    fun hint(board: List<Int>, obstacles: Map<Int, Obstacle>): Pair<Int, Int>? {
        if (board.size != SIZE * SIZE) return null
        for (from in board.indices) {
            if (from in obstacles) continue
            for (to in listOf(from + 1, from + SIZE)) {
                if (to !in board.indices || to in obstacles || !adjacent(from, to)) continue
                val swapped = board.toMutableList()
                val held = swapped[from]; swapped[from] = swapped[to]; swapped[to] = held
                val matched = matches(swapped, obstacles)
                if (from in matched || to in matched) return from to to
            }
        }
        return null
    }

    internal fun hasLegalMove(board: List<Int>, obstacles: Map<Int, Obstacle>): Boolean = hint(board, obstacles) != null

    /** Maps a completed drag from one cell to its dominant adjacent direction. */
    fun swipeTarget(index: Int, deltaX: Float, deltaY: Float, threshold: Float): Int? {
        if (index !in 0 until SIZE * SIZE || threshold <= 0f) return null
        if (kotlin.math.abs(deltaX) < threshold && kotlin.math.abs(deltaY) < threshold) return null
        val destination = when {
            kotlin.math.abs(deltaX) >= kotlin.math.abs(deltaY) -> index + if (deltaX > 0f) 1 else -1
            else -> index + if (deltaY > 0f) SIZE else -SIZE
        }
        return destination.takeIf { adjacent(index, it) }
    }

    private fun resolve(board: MutableList<Int>, obstacles: Map<Int, Obstacle>, initial: Set<Int>, targetColor: Int, seed: Int): Move {
        val random = Random(seed)
        val nextObstacles = obstacles.toMutableMap()
        var cells = initial
        var collected = 0
        var cleared = 0
        val clearedCells = linkedSetOf<Int>()
        val steps = mutableListOf<CascadeStep>()
        repeat(8) {
            if (cells.isEmpty()) return@repeat
            collected += cells.count { board[it] == targetColor }
            cleared += cells.size
            clearedCells += cells
            val clearedNow = cells.toSet()
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
                    segment.forEachIndexed { i, index -> board[index] = if (i < missing) random.nextInt(COLORS) else values[i - missing] }
                    segmentEnd = segmentStart - 1
                }
            }
            steps += CascadeStep(clearedNow, board.toList(), nextObstacles.toMap())
            cells = matches(board, nextObstacles)
        }
        return Move(board.toList(), nextObstacles.toMap(), collected, cleared, clearedCells, steps)
    }

    internal fun matches(board: List<Int>, obstacles: Map<Int, Obstacle>): Set<Int> {
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

    private fun serverCreate(seed: Int, obstacles: IntArray): IntArray {
        repeat(40) { attempt ->
            val rng = ServerRng((seed.toLong() + attempt).toInt())
            val board = IntArray(SIZE * SIZE) { floor(rng.next() * COLORS).toInt() }
            for (index in board.indices) {
                var guard = 0
                while (guard++ < COLORS * 2 && serverFormsLine(board, index, obstacles)) board[index] = floor(rng.next() * COLORS).toInt()
            }
            if (serverHasMove(board, obstacles)) return board
        }
        repeat(80) { attempt ->
            val board = IntArray(SIZE * SIZE) { cell -> Math.floorMod(cell + seed + COLORS * 1000, COLORS) }
            val rng = ServerRng((seed.toLong() + 10_000 + attempt).toInt())
            for (index in board.indices) {
                var reroll = 0
                while (reroll < COLORS * 2 && serverFormsLine(board, index, obstacles)) {
                    board[index] = floor(rng.next() * COLORS).toInt()
                    reroll++
                }
            }
            if (serverHasMove(board, obstacles)) return board
        }
        return IntArray(SIZE * SIZE) { index -> index % COLORS }
    }

    private fun serverFormsLine(board: IntArray, index: Int, obstacles: IntArray): Boolean {
        val row = index / SIZE
        val col = index % SIZE
        val color = board[index]
        var horizontal = 1
        var x = col - 1
        while (x >= 0 && obstacles[row * SIZE + x] < 0 && board[row * SIZE + x] == color) { horizontal++; x-- }
        x = col + 1
        while (x < SIZE && obstacles[row * SIZE + x] < 0 && board[row * SIZE + x] == color) { horizontal++; x++ }
        var vertical = 1
        var y = row - 1
        while (y >= 0 && obstacles[y * SIZE + col] < 0 && board[y * SIZE + col] == color) { vertical++; y-- }
        y = row + 1
        while (y < SIZE && obstacles[y * SIZE + col] < 0 && board[y * SIZE + col] == color) { vertical++; y++ }
        return horizontal >= 3 || vertical >= 3
    }

    private fun serverMatches(board: IntArray, obstacles: IntArray): Set<Int> {
        val found = HashSet<Int>()
        for (row in 0 until SIZE) {
            var col = 0
            while (col < SIZE) {
                val cell = row * SIZE + col
                if (obstacles[cell] >= 0) { col++; continue }
                val color = board[cell]
                var end = col + 1
                while (end < SIZE && obstacles[row * SIZE + end] < 0 && board[row * SIZE + end] == color) end++
                if (end - col >= 3) for (x in col until end) found += row * SIZE + x
                col = end
            }
        }
        for (col in 0 until SIZE) {
            var row = 0
            while (row < SIZE) {
                val cell = row * SIZE + col
                if (obstacles[cell] >= 0) { row++; continue }
                val color = board[cell]
                var end = row + 1
                while (end < SIZE && obstacles[end * SIZE + col] < 0 && board[end * SIZE + col] == color) end++
                if (end - row >= 3) for (y in row until end) found += y * SIZE + col
                row = end
            }
        }
        return found
    }

    private fun serverHasMove(board: IntArray, obstacles: IntArray): Boolean {
        for (from in board.indices) {
            if (obstacles[from] >= 0) continue
            for (to in intArrayOf(from + 1, from + SIZE)) {
                if (to >= board.size || obstacles[to] >= 0 || !adjacent(from, to)) continue
                val swapped = board.copyOf()
                val held = swapped[from]
                swapped[from] = swapped[to]
                swapped[to] = held
                val matched = serverMatches(swapped, obstacles)
                if (from in matched || to in matched) return true
            }
        }
        return false
    }
}

private class ServerRng(seed: Int) {
    private var state: Int = if (seed == 0) 0x6d2b79f5 else seed

    fun next(): Double {
        state += 0x6d2b79f5
        val mixed = imul(state xor (state ushr 15), 1 or state)
        val value = mixed xor (mixed + imul(mixed xor (mixed ushr 7), 61 or mixed))
        return (value xor (value ushr 14)).toUInt().toDouble() / 4294967296.0
    }

    private fun imul(a: Int, b: Int): Int = (a.toLong() * b.toLong()).toInt()
}

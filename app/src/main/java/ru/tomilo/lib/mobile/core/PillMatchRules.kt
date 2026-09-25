package ru.tomilo.lib.mobile.core

/** Rules for the compact 8×8 pill matching game. */
object PillMatchRules {
    const val BOARD_SIZE = 8
    const val COLOR_COUNT = 5
    const val CORAL_COLOR = 0

    val initialBoard: List<Int> = listOf(
        0, 1, 4, 3, 0, 2, 1, 4,
        4, 0, 1, 2, 3, 4, 0, 1,
        2, 3, 0, 0, 2, 3, 4, 2,
        1, 4, 2, 3, 4, 1, 2, 0,
        0, 2, 4, 1, 3, 0, 4, 2,
        3, 1, 0, 2, 4, 3, 1, 0,
        4, 0, 3, 1, 2, 4, 0, 3,
        1, 2, 4, 0, 3, 1, 2, 4,
    )

    data class Move(
        val board: List<Int>,
        val matchedCells: Set<Int>,
        val coralMatched: Int,
    )

    fun isAdjacent(first: Int, second: Int): Boolean {
        if (first !in 0 until BOARD_SIZE * BOARD_SIZE || second !in 0 until BOARD_SIZE * BOARD_SIZE) return false
        return kotlin.math.abs(first / BOARD_SIZE - second / BOARD_SIZE) +
            kotlin.math.abs(first % BOARD_SIZE - second % BOARD_SIZE) == 1
    }

    fun findMatches(board: List<Int>): Set<Int> {
        if (board.size != BOARD_SIZE * BOARD_SIZE) return emptySet()
        val found = mutableSetOf<Int>()

        for (row in 0 until BOARD_SIZE) {
            var start = 0
            for (column in 1..BOARD_SIZE) {
                if (column < BOARD_SIZE && board[row * BOARD_SIZE + column] == board[row * BOARD_SIZE + start]) continue
                if (column - start >= 3) for (x in start until column) found += row * BOARD_SIZE + x
                start = column
            }
        }
        for (column in 0 until BOARD_SIZE) {
            var start = 0
            for (row in 1..BOARD_SIZE) {
                if (row < BOARD_SIZE && board[row * BOARD_SIZE + column] == board[start * BOARD_SIZE + column]) continue
                if (row - start >= 3) for (y in start until row) found += y * BOARD_SIZE + column
                start = row
            }
        }
        return found
    }

    /** A move only resolves lines formed through one of the swapped cells. */
    fun swap(board: List<Int>, first: Int, second: Int, refillSeed: Int = 0): Move? {
        if (board.size != BOARD_SIZE * BOARD_SIZE || !isAdjacent(first, second)) return null
        if (findMatches(board).isNotEmpty()) return null

        val swapped = board.toMutableList()
        val held = swapped[first]
        swapped[first] = swapped[second]
        swapped[second] = held

        val matches = findMatches(swapped)
        if (first !in matches && second !in matches) return null

        val coralMatched = matches.count { swapped[it] == CORAL_COLOR }
        val refilled = refillMatches(swapped, matches, refillSeed)
        return Move(refilled, matches, coralMatched)
    }

    private fun refillMatches(board: List<Int>, matches: Set<Int>, seed: Int): List<Int> {
        val result = board.toMutableList()
        matches.sorted().forEach { index ->
            val startColor = Math.floorMod(index * 7 + seed + 1, COLOR_COUNT)
            for (offset in 0 until COLOR_COUNT) {
                val color = (startColor + offset) % COLOR_COUNT
                result[index] = color
                if (!hasLineThrough(result, index)) break
            }
        }
        return result
    }

    private fun hasLineThrough(board: List<Int>, index: Int): Boolean {
        val row = index / BOARD_SIZE
        val column = index % BOARD_SIZE
        val color = board[index]
        var horizontal = 1
        for (x in column - 1 downTo 0) {
            if (board[row * BOARD_SIZE + x] != color) break
            horizontal++
        }
        for (x in column + 1 until BOARD_SIZE) {
            if (board[row * BOARD_SIZE + x] != color) break
            horizontal++
        }
        var vertical = 1
        for (y in row - 1 downTo 0) {
            if (board[y * BOARD_SIZE + column] != color) break
            vertical++
        }
        for (y in row + 1 until BOARD_SIZE) {
            if (board[y * BOARD_SIZE + column] != color) break
            vertical++
        }
        return horizontal >= 3 || vertical >= 3
    }
}

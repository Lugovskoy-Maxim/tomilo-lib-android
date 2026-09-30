package ru.tomilo.lib.mobile.core

import ru.tomilo.lib.mobile.core.MatchThreeEngine.Obstacle

/** How gems travel between two settled boards. Rules stay in [MatchThreeEngine]. */
object MatchThreeMotion {
    const val SWAP_MS = 150
    const val POP_MS = 160
    const val FALL_MS = 220
    const val NUDGE_MS = 180

    data class Sprite(
        val id: Long,
        val color: Int,
        val fromCell: Int,
        val cell: Int,
        val pop: Boolean = false,
        val enter: Boolean = false,
        val spawnRows: Int = 0,
    )

    data class Wave(val popping: List<Sprite>, val landing: List<Sprite>)

    fun settle(board: List<Int>, previous: List<Sprite> = emptyList()): List<Sprite> {
        if (board.size != MatchThreeEngine.SIZE * MatchThreeEngine.SIZE) return emptyList()
        var nextId = (previous.maxOfOrNull { it.id } ?: 0L) + 1L
        val kept = previous.filter { !it.pop && !it.enter }.associateBy { it.cell }
        return board.mapIndexedNotNull { index, color ->
            if (color < 0) null
            else Sprite(
                id = kept[index]?.takeIf { it.color == color }?.id ?: nextId++,
                color = color,
                fromCell = index,
                cell = index,
            )
        }
    }

    /** One gravity pass: matched gems pop, survivors fall in order, holes fill from the top. */
    fun wave(
        before: List<Int>,
        matched: Set<Int>,
        after: List<Int>,
        obstacles: Map<Int, Obstacle>,
        previous: List<Sprite>,
    ): Wave {
        val size = MatchThreeEngine.SIZE
        val known = previous.filter { !it.pop }.associate { it.cell to it.id }
        val assigned = HashMap<Int, Long>()
        var nextId = (previous.maxOfOrNull { it.id } ?: 0L) + 1L
        fun idFor(cell: Int): Long = known[cell] ?: assigned.getOrPut(cell) { nextId++ }

        val popping = matched.mapNotNull { cell ->
            val color = before.getOrNull(cell) ?: return@mapNotNull null
            if (color < 0) null else Sprite(idFor(cell), color, cell, cell, pop = true)
        }
        val landing = mutableListOf<Sprite>()
        for (col in 0 until size) {
            var end = size - 1
            while (end >= 0) {
                val endIndex = col + end * size
                if (endIndex in obstacles) {
                    end--
                    continue
                }
                var start = end
                while (start > 0 && col + (start - 1) * size !in obstacles) start--
                val segment = (start..end).map { col + it * size }
                val survivors = segment.filter { it !in matched && (before.getOrNull(it) ?: -1) >= 0 }
                val spawnCount = segment.size - survivors.size
                segment.forEachIndexed { i, dest ->
                    if (i < spawnCount) {
                        val color = after.getOrElse(dest) { 0 }.coerceAtLeast(0)
                        landing += Sprite(nextId++, color, -1, dest, enter = true, spawnRows = spawnCount - i)
                    } else {
                        val src = survivors[i - spawnCount]
                        landing += Sprite(idFor(src), before[src], src, dest)
                    }
                }
                end = start - 1
            }
        }
        for ((cell, _) in obstacles) {
            val color = before.getOrNull(cell) ?: -1
            if (color >= 0 && landing.none { it.cell == cell || it.fromCell == cell }) {
                landing += Sprite(idFor(cell), color, cell, cell)
            }
        }
        val hold = landing.filter { !it.enter }.map { sprite ->
            if (sprite.fromCell >= 0) sprite.copy(cell = sprite.fromCell, fromCell = sprite.fromCell, spawnRows = 0) else sprite
        }
        return Wave(popping + hold, landing)
    }

    /** Remote boards can differ after several cascades, so changed cells swap in place. */
    fun crossfade(before: List<Int>, after: List<Int>, previous: List<Sprite>): Wave {
        val known = previous.filter { !it.pop }.associateBy { it.cell }
        var nextId = (previous.maxOfOrNull { it.id } ?: 0L) + 1L
        val popping = mutableListOf<Sprite>()
        val landing = mutableListOf<Sprite>()
        val count = minOf(before.size, after.size)
        for (index in 0 until count) {
            val old = before[index]
            val new = after[index]
            if (old >= 0 && old == new) {
                landing += Sprite(known[index]?.id ?: nextId++, new, index, index)
            } else {
                if (old >= 0) popping += Sprite(known[index]?.id ?: nextId++, old, index, index, pop = true)
                if (new >= 0) landing += Sprite(nextId++, new, index, index, enter = true)
            }
        }
        val hold = landing.filter { !it.enter }
        return Wave(popping + hold, landing)
    }
}

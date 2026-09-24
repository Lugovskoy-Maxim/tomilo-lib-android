package ru.tomilo.lib.mobile.core

/** Shared card rank rules used by the collection, forge and shop. */
object CardEconomy {
    private val ranks = listOf("F", "C", "B", "A", "S", "SSS")

    fun rank(stageValue: String?, rarityValue: String): String {
        val normalizedStage = stageValue?.trim()?.uppercase()
        return when (normalizedStage) {
            "SSS", "SS", "R" -> "SSS"
            "S", "A", "B" -> normalizedStage
            "C", "D" -> "C"
            "F", "E" -> "F"
            else -> when (rarityValue.trim().lowercase()) {
                "legendary" -> "SSS"
                "epic" -> "S"
                "rare" -> "C"
                else -> "F"
            }
        }
    }

    fun nextForgeRank(rank: String): String? = when (rank) {
        "F" -> "C"
        "C" -> "B"
        "B" -> "A"
        "A" -> "S"
        "S" -> "SSS"
        else -> null
    }

    fun sellPrice(roulettePrice: Int, rank: String): Int {
        val base = ((roulettePrice.takeIf { it > 0 } ?: 250) / 2 - 25).coerceAtLeast(0)
        val rankIndex = ranks.indexOf(rank).coerceAtLeast(0)
        return base * (1..rankIndex).fold(1) { value, _ -> value * 3 }
    }
}

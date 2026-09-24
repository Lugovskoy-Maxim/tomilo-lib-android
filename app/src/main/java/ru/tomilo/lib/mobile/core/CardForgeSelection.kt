package ru.tomilo.lib.mobile.core

data class CardForgeSelectionUpdate(
    val cardIds: List<String>,
    val message: String? = null,
)

/** Pure selection rules shared by the forge screen and its regression tests. */
object CardForgeSelection {
    fun toggle(
        selectedIds: List<String>,
        ranksByCardId: Map<String, String>,
        cardId: String,
        cardRank: String,
        availableCopies: Int,
        requiredMaterials: Int,
    ): CardForgeSelectionUpdate {
        val id = cardId.trim()
        if (id.isBlank()) {
            return CardForgeSelectionUpdate(
                selectedIds,
                "Эту карточку нельзя использовать: сервер не вернул её ID. Обновите коллекцию.",
            )
        }

        val selectedIndex = selectedIds.lastIndexOf(id)
        val selectedCopies = selectedIds.count { it == id }
        val maxCopies = availableCopies.coerceAtLeast(0)
        val maxMaterials = requiredMaterials.coerceAtLeast(0)
        if (selectedIndex >= 0 && (selectedIds.size >= maxMaterials || selectedCopies >= maxCopies)) {
            return CardForgeSelectionUpdate(selectedIds.toMutableList().apply { removeAt(selectedIndex) })
        }

        if (selectedIds.any { ranksByCardId[it] != cardRank }) {
            return CardForgeSelectionUpdate(selectedIds, "Для перековки выберите карточки одного ранга.")
        }

        if (selectedCopies >= maxCopies) {
            return CardForgeSelectionUpdate(selectedIds, "В коллекции нет дополнительных копий этой карточки.")
        }

        if (selectedIds.size >= maxMaterials) {
            return CardForgeSelectionUpdate(
                selectedIds,
                "Все материалы уже выбраны. Снимите отметку с карточки, чтобы заменить её.",
            )
        }

        return CardForgeSelectionUpdate(selectedIds + id)
    }

    /** Retains the selection order while dropping cards/copies no longer present in the collection. */
    fun reconcile(selectedIds: List<String>, availableCopiesById: Map<String, Int>): List<String> {
        val keptCopies = mutableMapOf<String, Int>()
        return selectedIds.filter { id ->
            val limit = availableCopiesById[id]?.coerceAtLeast(0) ?: 0
            val kept = keptCopies[id] ?: 0
            if (kept >= limit) {
                false
            } else {
                keptCopies[id] = kept + 1
                true
            }
        }
    }
}

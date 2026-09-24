package ru.tomilo.lib.mobile.core

import org.junit.Assert.assertEquals
import org.junit.Test

class CardForgeSelectionTest {
    @Test
    fun allowsOwnedDuplicateCopiesButNeverOverfillsTheForge() {
        val ranks = mapOf("card-a" to "C", "card-b" to "C", "card-c" to "C", "card-zero" to "C")
        val first = CardForgeSelection.toggle(emptyList(), ranks, "card-a", "C", 2, 3)
        val second = CardForgeSelection.toggle(first.cardIds, ranks, "card-a", "C", 2, 3)
        val noCopies = CardForgeSelection.toggle(second.cardIds, ranks, "card-zero", "C", 0, 3)
        val deselectedCopy = CardForgeSelection.toggle(second.cardIds, ranks, "card-a", "C", 2, 3)
        val third = CardForgeSelection.toggle(second.cardIds, ranks, "card-b", "C", 1, 3)
        val overCapacity = CardForgeSelection.toggle(third.cardIds, ranks, "card-c", "C", 1, 3)

        assertEquals(listOf("card-a"), first.cardIds)
        assertEquals(listOf("card-a", "card-a"), second.cardIds)
        assertEquals("В коллекции нет дополнительных копий этой карточки.", noCopies.message)
        assertEquals(listOf("card-a"), deselectedCopy.cardIds)
        assertEquals(listOf("card-a", "card-a", "card-b"), third.cardIds)
        assertEquals("Все материалы уже выбраны. Снимите отметку с карточки, чтобы заменить её.", overCapacity.message)
    }

    @Test
    fun togglingSelectedDuplicateRemovesOnlyOneCopy() {
        val update = CardForgeSelection.toggle(
            selectedIds = listOf("card-a", "card-a"),
            ranksByCardId = mapOf("card-a" to "B"),
            cardId = "card-a",
            cardRank = "B",
            availableCopies = 2,
            requiredMaterials = 3,
        )

        assertEquals(listOf("card-a"), update.cardIds)
        assertEquals(null, update.message)
    }

    @Test
    fun rejectsMixedRanksAndMissingIdsWithoutChangingSelection() {
        val selected = listOf("card-a")
        val ranks = mapOf("card-a" to "C", "card-b" to "B")
        val mixedRank = CardForgeSelection.toggle(selected, ranks, "card-b", "B", 1, 3)
        val missingId = CardForgeSelection.toggle(selected, ranks, "  ", "C", 1, 3)

        assertEquals(selected, mixedRank.cardIds)
        assertEquals("Для перековки выберите карточки одного ранга.", mixedRank.message)
        assertEquals(selected, missingId.cardIds)
        assertEquals("Эту карточку нельзя использовать: сервер не вернул её ID. Обновите коллекцию.", missingId.message)
    }

    @Test
    fun reconcileDropsMissingCardsAndCopiesRemovedFromCollection() {
        assertEquals(
            listOf("card-a", "card-b"),
            CardForgeSelection.reconcile(
                selectedIds = listOf("card-a", "card-a", "card-b", "deleted"),
                availableCopiesById = mapOf("card-a" to 1, "card-b" to 2),
            ),
        )
    }
}

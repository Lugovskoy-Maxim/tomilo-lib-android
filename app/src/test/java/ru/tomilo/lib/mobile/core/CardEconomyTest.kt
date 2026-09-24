package ru.tomilo.lib.mobile.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CardEconomyTest {
    @Test
    fun explicitForgeRankTakesPrecedenceAndNormalizesLegacyRanks() {
        assertEquals("SSS", CardEconomy.rank(" ss ", "common"))
        assertEquals("SSS", CardEconomy.rank("R", "common"))
        assertEquals("C", CardEconomy.rank("D", "legendary"))
        assertEquals("F", CardEconomy.rank(null, "common"))
    }

    @Test
    fun rarityProvidesFallbackWhenServerRankIsMissing() {
        assertEquals("SSS", CardEconomy.rank(null, "legendary"))
        assertEquals("S", CardEconomy.rank(null, "epic"))
        assertEquals("C", CardEconomy.rank(null, "rare"))
        assertEquals("F", CardEconomy.rank(null, "unknown"))
    }

    @Test
    fun forgeProgressionStopsAtTopRank() {
        assertEquals("C", CardEconomy.nextForgeRank("F"))
        assertEquals("SSS", CardEconomy.nextForgeRank("S"))
        assertNull(CardEconomy.nextForgeRank("SSS"))
    }

    @Test
    fun sellPriceUsesRouletteBaseAndTriplesEachRankStep() {
        assertEquals(100, CardEconomy.sellPrice(250, "F"))
        assertEquals(300, CardEconomy.sellPrice(250, "C"))
        assertEquals(24_300, CardEconomy.sellPrice(250, "SSS"))
        assertEquals(100, CardEconomy.sellPrice(0, "invalid"))
    }
}

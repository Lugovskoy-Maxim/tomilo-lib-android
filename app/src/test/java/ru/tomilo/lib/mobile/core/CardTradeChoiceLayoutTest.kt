package ru.tomilo.lib.mobile.core

import org.junit.Assert.assertEquals
import org.junit.Test

class CardTradeChoiceLayoutTest {
    @Test
    fun widensChoiceAtFontScale130WhereLabelsAlreadyWrap() {
        assertEquals(3, CardTradeChoiceLayout.labelLines(1.3f))
        assertEquals(144, CardTradeChoiceLayout.widthDp(1.3f))
    }

    @Test
    fun keepsDefaultChoiceWidthForNormalFontScale() {
        assertEquals(2, CardTradeChoiceLayout.labelLines(1f))
        assertEquals(104, CardTradeChoiceLayout.widthDp(1f))
    }
}

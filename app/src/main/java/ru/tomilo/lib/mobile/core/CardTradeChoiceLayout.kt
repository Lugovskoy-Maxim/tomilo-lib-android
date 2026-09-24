package ru.tomilo.lib.mobile.core

/** Keeps trade card choices wide enough when labels wrap at large system font scales. */
object CardTradeChoiceLayout {
    fun widthDp(fontScale: Float): Int = if (fontScale >= 1.3f) 144 else 104

    fun labelLines(fontScale: Float): Int = if (fontScale > 1.15f) 3 else 2
}

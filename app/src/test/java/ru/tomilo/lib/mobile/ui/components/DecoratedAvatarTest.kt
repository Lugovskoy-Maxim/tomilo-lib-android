package ru.tomilo.lib.mobile.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class DecoratedAvatarTest {
    @Test
    fun `placeholder uses two letters for a single username`() {
        assertEquals("TO", avatarInitials("tomilo"))
    }

    @Test
    fun `placeholder uses first and last name initials`() {
        assertEquals("АП", avatarInitials("Анна Петрова"))
    }

    @Test
    fun `placeholder is branded when username is absent`() {
        assertEquals("T", avatarInitials(null))
        assertEquals("T", avatarInitials(" --- "))
    }
}

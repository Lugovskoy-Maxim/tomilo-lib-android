package ru.tomilo.lib.mobile.data.api

import org.junit.Assert.assertEquals
import org.junit.Test

class UserDtoTest {
    @Test
    fun `reads chapter count returned by own profile endpoint`() {
        val user = NetworkModule.json.decodeFromString<UserDto>(
            """{"_id":"user-1","chaptersReadCount":42}""",
        )

        assertEquals(42, user.readChaptersTotal())
    }

    @Test
    fun `keeps compatibility with legacy chaptersRead field`() {
        val user = NetworkModule.json.decodeFromString<UserDto>(
            """{"_id":"user-1","chaptersRead":17}""",
        )

        assertEquals(17, user.readChaptersTotal())
    }

    @Test
    fun `prefers canonical profile counter and never displays negative values`() {
        assertEquals(9, UserDto(chaptersRead = 3, chaptersReadCount = 9).readChaptersTotal())
        assertEquals(0, UserDto(chaptersReadCount = -1).readChaptersTotal())
    }
}

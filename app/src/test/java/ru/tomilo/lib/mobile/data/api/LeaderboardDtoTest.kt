package ru.tomilo.lib.mobile.data.api

import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LeaderboardDtoTest {
    @Test
    fun `reads optional rank movement from leaderboard response`() {
        val response = NetworkModule.json.decodeFromString<LeaderboardResponseDto>(
            """{"users":[{"_id":"reader-1","username":"Reader","rankChange":{"day":3,"week":5}},{"_id":"reader-2","username":"Reader 2"}]}""",
        )

        assertEquals(3, response.users[0].rankChange?.day)
        assertEquals(5, response.users[0].rankChange?.week)
        assertNull(response.users[1].rankChange)
    }
}

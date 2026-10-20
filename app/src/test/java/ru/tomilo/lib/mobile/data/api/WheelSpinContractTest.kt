package ru.tomilo.lib.mobile.data.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WheelSpinContractTest {
    @Test
    fun paidAttemptCarriesIdempotencyAndAccountGuard() {
        val json = Json.encodeToString(
            WheelSpinRequest.serializer(),
            WheelSpinRequest(
                useCoins = true,
                requestId = "wheel-request-1234",
                expectedUserId = "user-123",
            ),
        )
        val fields = Json.parseToJsonElement(json).jsonObject

        assertTrue(fields.getValue("useCoins").jsonPrimitive.boolean)
        assertEquals("wheel-request-1234", fields.getValue("requestId").jsonPrimitive.content)
        assertEquals("user-123", fields.getValue("expectedUserId").jsonPrimitive.content)
    }

    @Test
    fun wheelResponseReadsPaidAttemptLimitsAndReplayMarker() {
        val wheel = Json.decodeFromString<WheelDto>("""
            {"spinRequestIdSupported":true,"paidSpinCostCoins":250,"paidSpinsUsed":1,"paidSpinsLimit":2}
        """)
        val result = Json.decodeFromString<WheelSpinResultDto>("""
            {"requestId":"wheel-request-1234","replayed":true,"label":"Награда"}
        """)

        assertTrue(wheel.spinRequestIdSupported)
        assertEquals(250, wheel.paidSpinCostCoins)
        assertEquals(1, wheel.paidSpinsUsed)
        assertEquals(2, wheel.paidSpinsLimit)
        assertTrue(result.replayed)
    }
}

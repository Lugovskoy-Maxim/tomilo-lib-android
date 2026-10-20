package ru.tomilo.lib.mobile.data.api

import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkModuleCachePolicyTest {
    @Test
    fun `adult-filtered discovery can use anonymous cache safely`() {
        listOf("titles/latest-updates", "titles/popular", "titles/random").forEach { path ->
            val request = Request.Builder()
                .url("https://tomilo-lib.ru/api/$path?includeAdult=false")
                .get()
                .build()

            assertTrue(NetworkModule.shouldOmitAuthorizationForAdultFilteredDiscovery(request))
        }
    }

    @Test
    fun `adult or unspecified discovery never drops authorization`() {
        listOf(
            "https://tomilo-lib.ru/api/titles/latest-updates?includeAdult=true",
            "https://tomilo-lib.ru/api/titles/latest-updates",
            "https://tomilo-lib.ru/api/users/profile/history?includeAdult=false",
        ).forEach { url ->
            val request = Request.Builder().url(url).get().build()
            assertFalse(NetworkModule.shouldOmitAuthorizationForAdultFilteredDiscovery(request))
        }
    }

    @Test
    fun `only GET requests may use anonymous discovery results`() {
        val request = Request.Builder()
            .url("https://tomilo-lib.ru/api/titles/latest-updates?includeAdult=false")
            .post(ByteArray(0).toRequestBody())
            .build()

        assertFalse(NetworkModule.shouldOmitAuthorizationForAdultFilteredDiscovery(request))
    }
}

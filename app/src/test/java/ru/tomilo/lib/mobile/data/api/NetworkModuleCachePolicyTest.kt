package ru.tomilo.lib.mobile.data.api

import okhttp3.Request
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Response
import okhttp3.Protocol
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

    @Test
    fun `explicit adult discovery bypasses the public http cache`() {
        listOf("titles/latest-updates", "titles/popular", "titles").forEach { path ->
            val request = Request.Builder()
                .url("https://tomilo-lib.ru/api/$path?includeAdult=true")
                .get()
                .build()

            assertFalse(NetworkModule.shouldUsePublicHttpCache(request))
        }
    }

    @Test
    fun `adult filtered discovery remains eligible for public cache`() {
        val request = Request.Builder()
            .url("https://tomilo-lib.ru/api/titles/latest-updates?includeAdult=false")
            .get()
            .build()

        assertTrue(NetworkModule.shouldUsePublicHttpCache(request))
    }

    @Test
    fun `authenticated catalog requests bypass cache even with adult filter disabled`() {
        val request = Request.Builder()
            .url("https://tomilo-lib.ru/api/titles?includeAdult=false&page=1")
            .header("Authorization", "Bearer user-token")
            .get()
            .build()

        assertFalse(NetworkModule.shouldUsePublicHttpCache(request))
        assertTrue(NetworkModule.shouldBypassHttpCache(request))
    }

    @Test
    fun `anonymous public discovery can still use cache`() {
        val request = Request.Builder()
            .url("https://tomilo-lib.ru/api/titles/latest-updates?includeAdult=false")
            .get()
            .build()

        assertFalse(NetworkModule.shouldBypassHttpCache(request))
    }

    @Test
    fun `random feed can use cache but one-item random draw cannot`() {
        val feed = Request.Builder()
            .url("https://tomilo-lib.ru/api/titles/random?limit=12&includeAdult=false")
            .get()
            .build()
        val singleDraw = Request.Builder()
            .url("https://tomilo-lib.ru/api/titles/random?limit=1&includeAdult=false")
            .get()
            .build()

        assertTrue(NetworkModule.shouldUsePublicHttpCache(feed))
        assertFalse(NetworkModule.shouldUsePublicHttpCache(singleDraw))
        assertTrue(NetworkModule.shouldBypassHttpCache(singleDraw))
    }

    @Test
    fun serverCacheDirectivesArePreservedAndFallbackIsLimitedToEligiblePublicResponses() {
        val publicRequest = Request.Builder()
            .url("https://tomilo-lib.ru/api/titles/latest-updates?includeAdult=false")
            .get()
            .build()

        assertNull(NetworkModule.cacheControlForNetworkResponse(
            publicRequest,
            response(publicRequest, headers = mapOf("Cache-Control" to "public, max-age=60, stale-while-revalidate=300")),
        ))
        assertNull(NetworkModule.cacheControlForNetworkResponse(
            publicRequest,
            response(publicRequest, headers = mapOf("Cache-Control" to "private, max-age=60")),
        ))
        assertNull(NetworkModule.cacheControlForNetworkResponse(
            publicRequest,
            response(publicRequest, headers = mapOf("Cache-Control" to "public, no-store")),
        ))
        assertEquals("public, max-age=120", NetworkModule.cacheControlForNetworkResponse(
            publicRequest,
            response(publicRequest),
        ))
        assertEquals("no-store", NetworkModule.cacheControlForNetworkResponse(
            publicRequest,
            response(publicRequest, headers = mapOf("Set-Cookie" to "session=example")),
        ))
        assertEquals("no-store", NetworkModule.cacheControlForNetworkResponse(
            publicRequest,
            response(publicRequest, code = 503),
        ))
        assertNull(NetworkModule.cacheControlForNetworkResponse(
            publicRequest,
            response(publicRequest, code = 304, headers = mapOf("Cache-Control" to "public, max-age=60")),
        ))
        assertEquals("public, max-age=120", NetworkModule.cacheControlForNetworkResponse(
            publicRequest,
            response(publicRequest, code = 304),
        ))

        val privateRequest = Request.Builder()
            .url("https://tomilo-lib.ru/api/users/profile")
            .get()
            .build()
        assertEquals("no-store", NetworkModule.cacheControlForNetworkResponse(
            privateRequest,
            response(privateRequest, headers = mapOf("Cache-Control" to "public, max-age=60")),
        ))
    }

    private fun response(
        request: Request,
        code: Int = 200,
        headers: Map<String, String> = emptyMap(),
    ): Response {
        var builder = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code in 200..299) "OK" else "Error")
            .body("{}".toResponseBody("application/json".toMediaType()))
        headers.forEach { (name, value) -> builder = builder.header(name, value) }
        return builder.build()
    }
}

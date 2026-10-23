package ru.tomilo.lib.mobile.data.api

import okhttp3.Call
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class CoverRequestCoalescingInterceptorTest {
    @Test
    fun singleCoverRequestReturnsWithoutReadingOrBufferingItsBody() {
        val interceptor = CoverRequestCoalescingInterceptor()
        val request = coverRequest()
        val sourceRequests = AtomicInteger()
        val bytes = byteArrayOf(3, 1, 4, 1, 5)

        val response = interceptor.intercept(
            FakeChain(request) { current ->
                Response.Builder()
                    .request(current)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .header("Content-Type", "image/jpeg")
                    .header("Cache-Control", "public, max-age=120")
                    .body(TrackingResponseBody(bytes, sourceRequests))
                    .build()
            },
        )

        assertEquals("the interceptor must leave a solo cover response streaming", 0, sourceRequests.get())
        response.use { assertArrayEquals(bytes, it.body!!.bytes()) }
        assertTrue("the caller should read the body after receiving the response", sourceRequests.get() > 0)
    }

    @Test
    fun unknownLengthCoverRemainsStreamingWhenAnotherRequestIsWaiting() {
        val interceptor = CoverRequestCoalescingInterceptor()
        val executor = Executors.newFixedThreadPool(2)
        val request = coverRequest()
        val networkCalls = AtomicInteger()
        val sourceRequests = AtomicInteger()
        val firstEntered = CountDownLatch(1)
        val releaseFirst = CountDownLatch(1)
        val bytes = byteArrayOf(2, 7, 1, 8)

        try {
            val first = executor.submit<Response> {
                interceptor.intercept(
                    FakeChain(request) { current ->
                        networkCalls.incrementAndGet()
                        firstEntered.countDown()
                        if (!releaseFirst.await(2, TimeUnit.SECONDS)) {
                            throw IOException("Timed out waiting for the test to release the response")
                        }
                        Response.Builder()
                            .request(current)
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .header("Content-Type", "image/jpeg")
                            .header("Cache-Control", "public, max-age=120")
                            .body(TrackingResponseBody(bytes, sourceRequests, declaredLength = -1L))
                            .build()
                    },
                )
            }
            assertTrue(firstEntered.await(2, TimeUnit.SECONDS))

            val second = executor.submit<Response> {
                interceptor.intercept(
                    FakeChain(request) { current ->
                        networkCalls.incrementAndGet()
                        imageResponse(current, bytes, "public, max-age=120")
                    },
                )
            }
            awaitWaiter(interceptor)
            releaseFirst.countDown()

            val firstResponse = first.get(2, TimeUnit.SECONDS)
            val secondResponse = second.get(2, TimeUnit.SECONDS)
            assertEquals(2, networkCalls.get())
            assertEquals("unknown-length bodies must not be peeked", 0, sourceRequests.get())
            firstResponse.close()
            secondResponse.close()
        } finally {
            releaseFirst.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun simultaneousCoverRequestsShareOneBodyAndKeepServerHeaders() {
        val interceptor = CoverRequestCoalescingInterceptor()
        val executor = Executors.newFixedThreadPool(2)
        val request = coverRequest()
        val networkCalls = AtomicInteger()
        val firstEntered = CountDownLatch(1)
        val releaseFirst = CountDownLatch(1)
        val bytes = byteArrayOf(1, 2, 3, 4, 5)

        try {
            val first = executor.submit<Response> {
                interceptor.intercept(
                    FakeChain(request) { current ->
                        networkCalls.incrementAndGet()
                        firstEntered.countDown()
                        if (!releaseFirst.await(2, TimeUnit.SECONDS)) {
                            throw IOException("Timed out waiting for the test to release the response")
                        }
                        imageResponse(current, bytes, "public, max-age=120")
                    },
                )
            }
            assertTrue(firstEntered.await(2, TimeUnit.SECONDS))

            val second = executor.submit<Response> {
                interceptor.intercept(
                    FakeChain(request) { current ->
                        networkCalls.incrementAndGet()
                        imageResponse(current, bytes, "public, max-age=120")
                    },
                )
            }
            awaitWaiter(interceptor)
            assertEquals(1, networkCalls.get())

            releaseFirst.countDown()
            first.get(2, TimeUnit.SECONDS).use { response ->
                assertArrayEquals(bytes, response.body!!.bytes())
                assertEquals("public, max-age=120", response.header("Cache-Control"))
            }
            second.get(2, TimeUnit.SECONDS).use { response ->
                assertArrayEquals(bytes, response.body!!.bytes())
                assertEquals("public, max-age=120", response.header("Cache-Control"))
                assertNotNull(response.request.tag(CoalescedCoverRequestTag::class.java))
            }

            assertEquals(1, networkCalls.get())
            assertEquals(0, interceptor.activeFlightCount)
        } finally {
            releaseFirst.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun noStoreCoverResponseIsNotShared() {
        val interceptor = CoverRequestCoalescingInterceptor()
        val executor = Executors.newFixedThreadPool(2)
        val request = coverRequest()
        val networkCalls = AtomicInteger()
        val firstEntered = CountDownLatch(1)
        val releaseFirst = CountDownLatch(1)
        val bytes = byteArrayOf(9, 8, 7)

        try {
            val first = executor.submit<Response> {
                interceptor.intercept(
                    FakeChain(request) { current ->
                        networkCalls.incrementAndGet()
                        firstEntered.countDown()
                        if (!releaseFirst.await(2, TimeUnit.SECONDS)) {
                            throw IOException("Timed out waiting for the test to release the response")
                        }
                        imageResponse(current, bytes, "no-store")
                    },
                )
            }
            assertTrue(firstEntered.await(2, TimeUnit.SECONDS))

            val second = executor.submit<Response> {
                interceptor.intercept(
                    FakeChain(request) { current ->
                        networkCalls.incrementAndGet()
                        imageResponse(current, bytes, "no-store")
                    },
                )
            }
            awaitWaiter(interceptor)
            releaseFirst.countDown()

            first.get(2, TimeUnit.SECONDS).close()
            second.get(2, TimeUnit.SECONDS).close()
            assertEquals(2, networkCalls.get())
            assertEquals(0, interceptor.activeFlightCount)
        } finally {
            releaseFirst.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun chapterImageRequestsAreNotCoalesced() {
        val interceptor = CoverRequestCoalescingInterceptor()
        val request = Request.Builder().url("https://cdn.example/chapter/page-1.webp").build()
        val networkCalls = AtomicInteger()

        repeat(2) {
            interceptor.intercept(
                FakeChain(request) { current ->
                    networkCalls.incrementAndGet()
                    imageResponse(current, byteArrayOf(1), "public, max-age=120")
                },
            ).close()
        }

        assertEquals(2, networkCalls.get())
        assertNull(request.tag(CoalescedCoverRequestTag::class.java))
    }

    private fun coverRequest(): Request = Request.Builder()
        .url("https://cdn.example/titles/abc123/cover.jpg")
        .header("Accept", "image/avif,image/webp,image/*")
        .header("Referer", "https://tomilo-lib.ru/")
        .build()

    private fun imageResponse(request: Request, bytes: ByteArray, cacheControl: String): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .header("Content-Type", "image/jpeg")
            .header("Cache-Control", cacheControl)
            .body(bytes.toResponseBody("image/jpeg".toMediaType()))
            .build()

    private class TrackingResponseBody(
        private val bytes: ByteArray,
        private val sourceRequests: AtomicInteger,
        private val declaredLength: Long = bytes.size.toLong(),
    ) : ResponseBody() {
        override fun contentType() = "image/jpeg".toMediaType()
        override fun contentLength(): Long = declaredLength
        override fun source(): Buffer {
            sourceRequests.incrementAndGet()
            return Buffer().write(bytes)
        }
    }

    private fun awaitWaiter(interceptor: CoverRequestCoalescingInterceptor) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2)
        while (interceptor.activeWaiterCount == 0 && System.nanoTime() < deadline) {
            Thread.sleep(2)
        }
        assertEquals(1, interceptor.activeWaiterCount)
    }

    private class FakeChain(
        private val initialRequest: Request,
        private val onProceed: (Request) -> Response,
    ) : Interceptor.Chain {
        override fun request(): Request = initialRequest

        override fun proceed(request: Request): Response = onProceed(request)

        override fun connection(): Connection? = null

        override fun call(): Call = testClient.newCall(initialRequest)

        override fun connectTimeoutMillis(): Int = 1_000

        override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this

        override fun readTimeoutMillis(): Int = 1_000

        override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this

        override fun writeTimeoutMillis(): Int = 1_000

        override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this

        private companion object {
            val testClient = OkHttpClient()
        }
    }
}

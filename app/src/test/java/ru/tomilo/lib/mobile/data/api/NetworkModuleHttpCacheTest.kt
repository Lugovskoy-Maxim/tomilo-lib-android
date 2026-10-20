package ru.tomilo.lib.mobile.data.api

import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class NetworkModuleHttpCacheTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `etag revalidation merges 304 with the cached public response`() {
        val requestCount = AtomicInteger()
        val conditionalTag = AtomicReference<String?>()
        val server = ServerSocket(0)
        val serverFailure = AtomicReference<Throwable?>()
        val worker = thread(name = "conditional-cache-test-server") {
            try {
                repeat(2) { requestIndex ->
                    server.accept().use { socket ->
                        val reader = BufferedReader(
                            InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1),
                        )
                        val headers = mutableListOf<String>()
                        while (true) {
                            val line = reader.readLine() ?: break
                            if (line.isEmpty()) break
                            headers += line
                        }
                        requestCount.incrementAndGet()
                        val writer = BufferedWriter(
                            OutputStreamWriter(socket.getOutputStream(), StandardCharsets.ISO_8859_1),
                        )
                        if (requestIndex == 0) {
                            val body = "{\"success\":true,\"data\":[{\"id\":\"title-1\"}]}"
                            writer.write(
                                "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: application/json\r\n" +
                                    "Cache-Control: public, max-age=0\r\n" +
                                    "ETag: W/\"updates-v1\"\r\n" +
                                    "Content-Length: ${body.toByteArray(StandardCharsets.UTF_8).size}\r\n" +
                                    "Connection: close\r\n\r\n" + body,
                            )
                        } else {
                            conditionalTag.set(
                                headers.firstOrNull { it.startsWith("If-None-Match:", ignoreCase = true) }
                                    ?.substringAfter(':')?.trim(),
                            )
                            writer.write(
                                "HTTP/1.1 304 Not Modified\r\n" +
                                    "Cache-Control: public, max-age=60\r\n" +
                                    "ETag: W/\"updates-v1\"\r\n" +
                                    "Connection: close\r\n\r\n",
                            )
                        }
                        writer.flush()
                    }
                }
            } catch (failure: Throwable) {
                serverFailure.set(failure)
            }
        }

        val cache = Cache(temporaryFolder.newFolder("http-cache"), 1024L * 1024L)
        val client = OkHttpClient.Builder()
            .cache(cache)
            .addNetworkInterceptor { chain ->
                val response = chain.proceed(chain.request())
                val localCacheControl = NetworkModule.cacheControlForNetworkResponse(
                    chain.request(),
                    response,
                )
                if (localCacheControl == null) response
                else response.newBuilder().header("Cache-Control", localCacheControl).build()
            }
            .build()
        val request = Request.Builder()
            .url("http://127.0.0.1:${server.localPort}/api/titles/latest-updates?includeAdult=false")
            .get()
            .build()

        try {
            val first = client.newCall(request).execute().use { response ->
                assertEquals(200, response.code)
                response.body!!.string()
            }
            val second = client.newCall(request).execute().use { response ->
                assertEquals(200, response.code)
                assertEquals(304, response.networkResponse?.code)
                assertNotNull(response.cacheResponse)
                response.body!!.string()
            }

            worker.join(2_000)
            assertEquals(null, serverFailure.get())
            assertEquals(first, second)
            assertEquals("W/\"updates-v1\"", conditionalTag.get())
            assertEquals(2, requestCount.get())
        } finally {
            client.connectionPool.evictAll()
            cache.close()
            server.close()
            worker.join(2_000)
        }
    }
}

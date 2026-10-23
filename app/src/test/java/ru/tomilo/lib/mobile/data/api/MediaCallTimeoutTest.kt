package ru.tomilo.lib.mobile.data.api

import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class MediaCallTimeoutTest {
    @Test
    fun `media call timeout cancels a slow response`() {
        val server = ServerSocket(0)
        val worker = thread(name = "media-call-timeout-test-server") {
            runCatching {
                server.accept().use { socket ->
                    val reader = BufferedReader(
                        InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1),
                    )
                    while (true) {
                        val line = reader.readLine() ?: break
                        if (line.isEmpty()) break
                    }
                    Thread.sleep(500L)
                    runCatching {
                        BufferedWriter(
                            OutputStreamWriter(socket.getOutputStream(), StandardCharsets.ISO_8859_1),
                        ).use { writer ->
                            writer.write("HTTP/1.1 200 OK\r\nContent-Length: 1\r\nConnection: close\r\n\r\nx")
                            writer.flush()
                        }
                    }
                }
            }
        }

        val client = NetworkModule.withMediaCallTimeout(OkHttpClient.Builder(), 100).build()
        val request = Request.Builder()
            .url("http://127.0.0.1:${server.localPort}/page.webp")
            .build()

        try {
            val startedAt = System.nanoTime()
            val failure = runCatching { client.newCall(request).execute().use { it.body?.bytes() } }.exceptionOrNull()
            val elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)

            assertNotNull("expected the request-level timeout to cancel the slow response", failure)
            assertTrue("request took ${elapsedMillis}ms despite a 100ms timeout", elapsedMillis < 400L)
            worker.join(2_000L)
        } finally {
            server.close()
            worker.join(2_000L)
            client.connectionPool.evictAll()
        }
    }
}

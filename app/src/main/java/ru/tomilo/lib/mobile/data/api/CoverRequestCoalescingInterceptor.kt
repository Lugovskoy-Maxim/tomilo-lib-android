package ru.tomilo.lib.mobile.data.api

import okhttp3.Handshake
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.BufferedSource
import okio.buffer
import okio.source
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger

/** Marks a response supplied to a simultaneous duplicate cover request. */
internal object CoalescedCoverRequestTag

/**
 * Collapses simultaneous GETs for the same public cover while preserving the exact server response.
 * It does not retain responses after the in-flight request completes and skips uncacheable or large bodies.
 */
internal class CoverRequestCoalescingInterceptor : Interceptor {
    private class Flight {
        val response = CompletableFuture<CoverResponseSnapshot?>()
        val waiters = AtomicInteger(0)
    }

    private val inFlight = ConcurrentHashMap<String, Flight>()

    internal val activeFlightCount: Int
        get() = inFlight.size

    internal val activeWaiterCount: Int
        get() = inFlight.values.sumOf { it.waiters.get() }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!isEligibleRequest(request)) return chain.proceed(request)

        val key = requestKey(request)
        val created = Flight()
        val existing = inFlight.putIfAbsent(key, created)
        if (existing != null) return awaitExisting(chain, request, existing)

        try {
            val response = chain.proceed(request)
            val snapshot = try {
                // Do not consume or buffer a single cover just to prepare for a duplicate
                // that may never arrive. Sharing is only useful when another caller is
                // already waiting for this exact response.
                if (created.waiters.get() > 0) snapshotIfShareable(response) else null
            } catch (error: IOException) {
                created.response.completeExceptionally(error)
                return response
            }
            created.response.complete(snapshot)
            return response
        } catch (error: IOException) {
            created.response.completeExceptionally(error)
            throw error
        } catch (error: RuntimeException) {
            created.response.completeExceptionally(error)
            throw error
        } finally {
            inFlight.remove(key, created)
        }
    }

    private fun awaitExisting(
        chain: Interceptor.Chain,
        request: Request,
        flight: Flight,
    ): Response {
        flight.waiters.incrementAndGet()
        val snapshot = try {
            flight.response.get(MAX_WAIT_SECONDS, TimeUnit.SECONDS)
        } catch (_: ExecutionException) {
            null
        } catch (_: TimeoutException) {
            null
        } catch (error: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IOException("Interrupted while waiting for the in-flight cover request", error)
        } finally {
            flight.waiters.decrementAndGet()
        }

        return snapshot?.responseFor(request) ?: chain.proceed(request)
    }

    private fun isEligibleRequest(request: Request): Boolean {
        if (request.method != "GET" || request.header("Authorization") != null) return false
        if (request.header("Range") != null || request.cacheControl.noStore) return false

        val path = request.url.encodedPath.lowercase()
        return path.substringAfterLast('/').startsWith("cover.") || "/covers/" in path
    }

    private fun requestKey(request: Request): String = buildString {
        append(request.method)
        append('\n')
        append(request.url)
        request.headers.names().sortedBy(String::lowercase).forEach { name ->
            append('\n')
            append(name.lowercase())
            append(':')
            request.headers.values(name).forEach { value ->
                append(value)
                append('\u0000')
            }
        }
    }

    private fun snapshotIfShareable(response: Response): CoverResponseSnapshot? {
        if (!response.isSuccessful || response.body == null) return null

        val cacheDirectives = response.headers.values("Cache-Control")
            .flatMap { it.split(',') }
            .map { it.substringBefore('=').trim() }
        if (cacheDirectives.any { it.equals("no-store", ignoreCase = true) || it.equals("private", ignoreCase = true) }) {
            return null
        }
        if (response.headers.values("Set-Cookie").isNotEmpty()) return null
        if (response.headers.values("Vary").any { value -> value.split(',').any { it.trim() == "*" } }) {
            return null
        }

        val body = response.body ?: return null
        val contentLength = body.contentLength()
        // Unknown-length and oversized streams stay streaming. Peeking would otherwise
        // make callers wait for up to 4 MiB before Coil can begin consuming the body.
        if (contentLength < 0L || contentLength > MAX_COVER_BYTES) return null

        val bytes = response.peekBody(MAX_COVER_BYTES + 1).bytes()
        if (bytes.size.toLong() > MAX_COVER_BYTES) return null
        if (contentLength >= 0 && bytes.size.toLong() != contentLength) return null

        return CoverResponseSnapshot(
            protocol = response.protocol,
            code = response.code,
            message = response.message,
            handshake = response.handshake,
            headers = response.headers,
            networkResponse = response.networkResponse,
            cacheResponse = response.cacheResponse,
            priorResponse = response.priorResponse,
            sentRequestAtMillis = response.sentRequestAtMillis,
            receivedResponseAtMillis = response.receivedResponseAtMillis,
            contentType = body.contentType(),
            bytes = bytes,
        )
    }

    private data class CoverResponseSnapshot(
        val protocol: Protocol,
        val code: Int,
        val message: String,
        val handshake: Handshake?,
        val headers: Headers,
        val networkResponse: Response?,
        val cacheResponse: Response?,
        val priorResponse: Response?,
        val sentRequestAtMillis: Long,
        val receivedResponseAtMillis: Long,
        val contentType: MediaType?,
        val bytes: ByteArray,
    ) {
        fun responseFor(request: Request): Response = Response.Builder()
            .request(
                request.newBuilder()
                    .tag(CoalescedCoverRequestTag::class.java, CoalescedCoverRequestTag)
                    .build(),
            )
            .protocol(protocol)
            .code(code)
            .message(message)
            .handshake(handshake)
            .headers(headers)
            .networkResponse(networkResponse)
            .cacheResponse(cacheResponse)
            .priorResponse(priorResponse)
            .sentRequestAtMillis(sentRequestAtMillis)
            .receivedResponseAtMillis(receivedResponseAtMillis)
            .body(ReplayableByteArrayBody(bytes, contentType))
            .build()
    }

    private class ReplayableByteArrayBody(
        private val bytes: ByteArray,
        private val mediaType: MediaType?,
    ) : ResponseBody() {
        private val bufferedSource: BufferedSource by lazy {
            ByteArrayInputStream(bytes).source().buffer()
        }

        override fun contentType(): MediaType? = mediaType

        override fun contentLength(): Long = bytes.size.toLong()

        override fun source(): BufferedSource = bufferedSource
    }

    private companion object {
        const val MAX_COVER_BYTES = 4L * 1024L * 1024L
        const val MAX_WAIT_SECONDS = 21L
    }
}

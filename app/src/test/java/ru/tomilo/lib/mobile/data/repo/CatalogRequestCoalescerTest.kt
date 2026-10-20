package ru.tomilo.lib.mobile.data.repo

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class CatalogRequestCoalescerTest {
    @Test
    fun firstPageRequestsShareKeysOnlyForPublicAdultFilteredData() {
        assertEquals(
            "updates:24:adult=false",
            CatalogRequestKeys.latestUpdates(page = 1, limit = 24, includeAdult = false),
        )
        assertEquals(null, CatalogRequestKeys.latestUpdates(page = 2, limit = 24, includeAdult = false))
        assertEquals(null, CatalogRequestKeys.latestUpdates(page = 1, limit = 24, includeAdult = true))
        assertEquals("popular:24:adult=false", CatalogRequestKeys.popular(limit = 24, includeAdult = false))
        assertEquals(null, CatalogRequestKeys.popular(limit = 24, includeAdult = null))
        assertEquals("random:12:adult=false", CatalogRequestKeys.randomTitles(limit = 12, includeAdult = false))
        assertEquals(null, CatalogRequestKeys.randomTitles(limit = 1, includeAdult = false))
        assertEquals(null, CatalogRequestKeys.randomTitles(limit = 12, includeAdult = true))
    }

    @Test
    fun overlappingPublicRequestsShareOneFetch() = runBlocking {
        val coalescer = CatalogRequestCoalescer()
        val calls = AtomicInteger()
        val entered = CompletableDeferred<Unit>()
        val secondStarted = CompletableDeferred<Unit>()
        val release = CompletableDeferred<String>()

        val first = async {
            coalescer.run("popular:24:adult=false") {
                calls.incrementAndGet()
                entered.complete(Unit)
                release.await()
            }
        }
        entered.await()
        val second = async {
            secondStarted.complete(Unit)
            coalescer.run("popular:24:adult=false") {
                calls.incrementAndGet()
                "unexpected second request"
            }
        }
        secondStarted.await()
        yield()
        release.complete("shared response")

        assertEquals("shared response", first.await())
        assertEquals("shared response", second.await())
        assertEquals(1, calls.get())
    }

    @Test
    fun accountScopedRequestsAreNotShared() = runBlocking {
        val coalescer = CatalogRequestCoalescer()
        val calls = AtomicInteger()
        val first = coalescer.run<String>(null) { calls.incrementAndGet(); "first" }
        val second = coalescer.run<String>(null) { calls.incrementAndGet(); "second" }

        assertTrue(first != second)
        assertEquals(2, calls.get())
    }
}

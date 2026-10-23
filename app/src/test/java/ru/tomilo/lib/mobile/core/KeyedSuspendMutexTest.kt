package ru.tomilo.lib.mobile.core

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class KeyedSuspendMutexTest {
    @Test
    fun sequentialChapterPagesDoNotAccumulateRegistryEntries() = runBlocking {
        val locks = KeyedSuspendMutex<String>()

        repeat(1_000) { page ->
            locks.withLock("chapter/page-$page.webp") { page }
            assertEquals("completed page keys must not accumulate", 0, locks.activeKeyCount)
        }
    }

    @Test
    fun concurrentCallersShareLockAndRegistryDropsIdleKey() = runBlocking {
        val locks = KeyedSuspendMutex<String>()
        val firstEntered = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        val secondEntered = CompletableDeferred<Unit>()
        val active = AtomicInteger()
        val maximumActive = AtomicInteger()

        val first = launch(Dispatchers.Default) {
            locks.withLock("chapter/page.webp") {
                maximumActive.updateMaximum(active.incrementAndGet())
                firstEntered.complete(Unit)
                releaseFirst.await()
                active.decrementAndGet()
            }
        }
        firstEntered.await()

        val second = launch(start = CoroutineStart.UNDISPATCHED) {
            locks.withLock("chapter/page.webp") {
                maximumActive.updateMaximum(active.incrementAndGet())
                secondEntered.complete(Unit)
                active.decrementAndGet()
            }
        }

        assertEquals(1, locks.activeKeyCount)
        releaseFirst.complete(Unit)
        secondEntered.await()
        first.join()
        second.join()

        assertEquals("same-page work must never overlap", 1, maximumActive.get())
        assertEquals("idle page keys should be removed", 0, locks.activeKeyCount)
    }

    @Test
    fun cancellationOfWaitingCallerReleasesItsRegistryReference() = runBlocking {
        val locks = KeyedSuspendMutex<String>()
        val firstEntered = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()

        val first = launch(Dispatchers.Default) {
            locks.withLock("chapter/page.webp") {
                firstEntered.complete(Unit)
                releaseFirst.await()
            }
        }
        firstEntered.await()

        val waiting = launch(start = CoroutineStart.UNDISPATCHED) {
            locks.withLock("chapter/page.webp") {
                error("cancelled waiter must not enter the protected block")
            }
        }
        assertEquals(1, locks.activeKeyCount)

        waiting.cancelAndJoin()
        releaseFirst.complete(Unit)
        first.join()

        assertTrue(waiting.isCancelled)
        assertEquals("cancelled/idle page keys should be removed", 0, locks.activeKeyCount)
    }

    private fun AtomicInteger.updateMaximum(candidate: Int) {
        updateAndGet { current -> maxOf(current, candidate) }
    }
}

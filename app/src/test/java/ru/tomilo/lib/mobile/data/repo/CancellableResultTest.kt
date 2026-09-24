package ru.tomilo.lib.mobile.data.repo

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class CancellableResultTest {
    @Test
    fun returnsSuccessfulValue() = runBlocking {
        val result = runCatchingCancellable { "ok" }

        assertEquals("ok", result.getOrThrow())
    }

    @Test
    fun capturesOrdinaryFailureAsResult() = runBlocking {
        val failure = IllegalStateException("network failed")

        val result = runCatchingCancellable<String> { throw failure }

        assertSame(failure, result.exceptionOrNull())
    }

    @Test
    fun rethrowsCancellationInsteadOfReturningFailure() = runBlocking {
        val cancellation = CancellationException("screen left")

        try {
            runCatchingCancellable<String> { throw cancellation }
            throw AssertionError("Cancellation must propagate")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
    }
}

package ru.tomilo.lib.mobile.core

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CultivationMutationReconcilerTest {
    @Test
    fun synchronizesAfterSuccessfulMutation() = runBlocking {
        var syncCalls = 0

        val outcome = runCultivationMutationAndSync(
            mutation = { Result.success("Медитация начата") },
            synchronize = {
                syncCalls += 1
                true
            },
        )

        assertEquals(1, syncCalls)
        assertEquals("Медитация начата", outcome.actionResult.getOrThrow())
        assertTrue(outcome.stateSynchronized)
    }

    @Test
    fun synchronizesAfterFailedMutationAndPreservesUnknownState() = runBlocking {
        val failure = IllegalStateException("timeout")
        var syncCalls = 0

        val outcome = runCultivationMutationAndSync<String>(
            mutation = { Result.failure(failure) },
            synchronize = {
                syncCalls += 1
                false
            },
        )

        assertEquals(1, syncCalls)
        assertSame(failure, outcome.actionResult.exceptionOrNull())
        assertFalse(outcome.stateSynchronized)
    }

    @Test
    fun synchronizesAfterUnexpectedMutationException() = runBlocking {
        val failure = IllegalStateException("unexpected timeout")
        var syncCalls = 0

        val outcome = runCultivationMutationAndSync<String>(
            mutation = { throw failure },
            synchronize = {
                syncCalls += 1
                true
            },
        )

        assertEquals(1, syncCalls)
        assertSame(failure, outcome.actionResult.exceptionOrNull())
        assertTrue(outcome.stateSynchronized)
    }
}

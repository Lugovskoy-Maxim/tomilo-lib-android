package ru.tomilo.lib.mobile.core

import kotlinx.coroutines.CancellationException

internal data class CultivationMutationOutcome<T>(
    val actionResult: Result<T>,
    val stateSynchronized: Boolean,
)

/** Refreshes server state after every mutation response, including an ambiguous network failure. */
internal suspend fun <T> runCultivationMutationAndSync(
    mutation: suspend () -> Result<T>,
    synchronize: suspend () -> Boolean,
): CultivationMutationOutcome<T> {
    val result = try {
        mutation()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }
    val synchronized = try {
        synchronize()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        false
    }
    return CultivationMutationOutcome(result, synchronized)
}

package ru.tomilo.lib.mobile.data.repo

import kotlinx.coroutines.CancellationException

/** Wrap repository failures as Result without turning coroutine cancellation into a UI error. */
internal suspend inline fun <T> runCatchingCancellable(
    crossinline block: suspend () -> T,
): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (failure: Throwable) {
    Result.failure(failure)
}

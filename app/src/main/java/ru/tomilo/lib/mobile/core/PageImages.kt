package ru.tomilo.lib.mobile.core

import android.content.Context
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ErrorResult
import coil.request.ImageRequest

/** Запросы страниц читалки: при повторе обходим битый Coil/OkHttp-кеш. */
object PageImages {
    const val MAX_ATTEMPTS = 3

    fun request(context: Context, data: Any, attempt: Int = 0): ImageRequest {
        val bypassCache = attempt > 0
        val source = retrySource(data, attempt)
        return ImageRequest.Builder(context)
            .data(source)
            .crossfade(attempt == 0)
            .memoryCachePolicy(if (bypassCache) CachePolicy.DISABLED else CachePolicy.ENABLED)
            .diskCachePolicy(if (bypassCache) CachePolicy.DISABLED else CachePolicy.ENABLED)
            // DISABLED здесь запрещает сам сетевой запрос, а не только кеш.
            .networkCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    @OptIn(ExperimentalCoilApi::class)
    fun evict(context: Context, data: Any) {
        val key = data.toString()
        if (key.isBlank()) return
        runCatching { context.imageLoader.diskCache?.remove(key) }
    }

    fun prefetch(context: Context, data: Any, attempt: Int = 0) {
        if (data.toString().isBlank()) return
        val loader = context.imageLoader
        loader.enqueue(
            ImageRequest.Builder(context)
                .data(retrySource(data, attempt))
                .memoryCachePolicy(if (attempt > 0) CachePolicy.DISABLED else CachePolicy.ENABLED)
                .diskCachePolicy(if (attempt > 0) CachePolicy.DISABLED else CachePolicy.ENABLED)
                .networkCachePolicy(CachePolicy.ENABLED)
                .listener(
                    onSuccess = { _, result ->
                        val ok = result.drawable.intrinsicWidth >= 8 &&
                            result.drawable.intrinsicHeight >= 8
                        if (!ok) {
                            evict(context, data)
                            if (attempt + 1 < MAX_ATTEMPTS) prefetch(context, data, attempt + 1)
                        }
                    },
                    onError = { _, _: ErrorResult ->
                        evict(context, data)
                        if (attempt + 1 < MAX_ATTEMPTS) prefetch(context, data, attempt + 1)
                    },
                )
                .build(),
        )
    }

    private fun retrySource(data: Any, attempt: Int): Any {
        if (data !is String) return data
        val candidate = MediaUrl.candidate(data, attempt)
        if (!candidate.startsWith("http://") && !candidate.startsWith("https://")) return candidate
        if (attempt <= 0) return candidate
        val separator = if ('?' in candidate) '&' else '?'
        return "$candidate${separator}tomilo_retry=$attempt"
    }
}

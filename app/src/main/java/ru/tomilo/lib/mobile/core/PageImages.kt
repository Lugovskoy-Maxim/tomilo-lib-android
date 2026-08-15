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
        return ImageRequest.Builder(context)
            .data(data)
            .crossfade(attempt == 0)
            .memoryCachePolicy(if (bypassCache) CachePolicy.WRITE_ONLY else CachePolicy.ENABLED)
            .diskCachePolicy(if (bypassCache) CachePolicy.WRITE_ONLY else CachePolicy.ENABLED)
            .networkCachePolicy(if (bypassCache) CachePolicy.DISABLED else CachePolicy.ENABLED)
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
                .data(data)
                .memoryCachePolicy(if (attempt > 0) CachePolicy.WRITE_ONLY else CachePolicy.ENABLED)
                .diskCachePolicy(if (attempt > 0) CachePolicy.WRITE_ONLY else CachePolicy.ENABLED)
                .networkCachePolicy(if (attempt > 0) CachePolicy.DISABLED else CachePolicy.ENABLED)
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
}

package ru.tomilo.lib.mobile.core

import android.content.Context
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale

/** Запросы страниц читалки: при повторе обходим битый Coil/OkHttp-кеш. */
object PageImages {
    const val MAX_ATTEMPTS = 3

    // Держим ширину на уровне Full HD телефона, но разрешаем длинной странице
    // до 16K по высоте. Это сохраняет чёткость текста у склеенных WebP и всё
    // ещё ограничивает размер software bitmap, защищая приложение от OOM/GPU.
    private const val MAX_DECODE_WIDTH_PX = 1_080
    private const val MAX_DECODE_HEIGHT_PX = 16_384

    fun request(context: Context, data: Any, attempt: Int = 0): ImageRequest {
        val bypassCache = attempt > 0
        val source = retrySource(data, attempt)
        return ImageRequest.Builder(context)
            .data(source)
            .size(MAX_DECODE_WIDTH_PX, MAX_DECODE_HEIGHT_PX)
            .scale(Scale.FIT)
            .precision(Precision.INEXACT)
            .allowHardware(false)
            // Crossfade и memory cache одновременно удерживают несколько
            // огромных bitmap. Исходный WebP остаётся в дисковом HTTP/Coil-кеше.
            .crossfade(false)
            .memoryCachePolicy(CachePolicy.DISABLED)
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

    private fun retrySource(data: Any, attempt: Int): Any {
        if (data !is String) return data
        val candidate = MediaUrl.candidate(data, attempt)
        if (!candidate.startsWith("http://") && !candidate.startsWith("https://")) return candidate
        if (attempt <= 0) return candidate
        val separator = if ('?' in candidate) '&' else '?'
        return "$candidate${separator}tomilo_retry=$attempt"
    }
}

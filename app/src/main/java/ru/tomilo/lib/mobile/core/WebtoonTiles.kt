package ru.tomilo.lib.mobile.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.tomilo.lib.mobile.data.api.NetworkModule
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class PageDimensions(
    val width: Int = 0,
    val height: Int = 0,
) {
    fun isValid(): Boolean = width > 0 && height > 0
}

data class WebtoonTile(
    val index: Int,
    val top: Int,
    val width: Int,
    val height: Int,
)

data class SourceRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val width: Int get() = (right - left).coerceAtLeast(0)
    val height: Int get() = (bottom - top).coerceAtLeast(0)
}

/**
 * Декодирует длинные страницы вебтуна регионами. Bitmap каждой плитки не выше
 * 4096 px, поэтому изображение не обрезается лимитом GPU и не требует держать
 * полный WebP высотой 10–30K в оперативной памяти.
 */
object WebtoonTiles {
    private const val MAX_TILE_SOURCE_HEIGHT = 4_096
    // Степенной inSampleSize у Android не должен перескочить с ~1500 сразу
    // до 750 px на исходниках шириной 3K — сохраняем запас для QHD-экранов.
    private const val MAX_DECODE_WIDTH = 2_048
    private const val CACHE_TRIM_AT = 600L * 1024L * 1024L
    private const val CACHE_TARGET = 450L * 1024L * 1024L

    private val sourceLocks = ConcurrentHashMap<String, Mutex>()
    @Volatile private var mediaClient: OkHttpClient? = null

    fun split(dimensions: PageDimensions): List<WebtoonTile> {
        if (!dimensions.isValid()) return emptyList()
        return buildList {
            var top = 0
            var index = 0
            while (top < dimensions.height) {
                val height = minOf(MAX_TILE_SOURCE_HEIGHT, dimensions.height - top)
                add(WebtoonTile(index = index++, top = top, width = dimensions.width, height = height))
                top += height
            }
        }
    }

    /**
     * Серверные pageDimensions могут не совпасть с файлом (сжатие при загрузке,
     * webp). Без пересчёта соседние плитки захватывают чужие пиксели и наплывают.
     */
    fun mapTileToSource(
        tile: WebtoonTile,
        claimed: PageDimensions,
        sourceWidth: Int,
        sourceHeight: Int,
    ): SourceRect {
        val srcW = sourceWidth.coerceAtLeast(1)
        val srcH = sourceHeight.coerceAtLeast(1)
        val claimW = if (claimed.width > 0) claimed.width else tile.width.coerceAtLeast(1)
        val claimH = if (claimed.height > 0) claimed.height else (tile.top + tile.height).coerceAtLeast(1)
        val top = scale(tile.top, claimH, srcH).coerceIn(0, srcH - 1)
        val bottom = scale(tile.top + tile.height, claimH, srcH).coerceIn(top + 1, srcH)
        val right = scale(tile.width.coerceAtLeast(1), claimW, srcW).coerceIn(1, srcW)
        return SourceRect(left = 0, top = top, right = right, bottom = bottom)
    }

    suspend fun decode(
        context: Context,
        source: String,
        tile: WebtoonTile,
        claimed: PageDimensions,
        retry: Int = 0,
    ): Bitmap = withContext(Dispatchers.IO) {
        val file = sourceFile(context.applicationContext, source, retry)
        decodeRegion(file, tile, claimed)
    }

    suspend fun measureSource(context: Context, source: String, retry: Int = 0): PageDimensions =
        withContext(Dispatchers.IO) {
            val file = sourceFile(context.applicationContext, source, retry)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, options)
            PageDimensions(options.outWidth.coerceAtLeast(0), options.outHeight.coerceAtLeast(0))
        }

    suspend fun measureLocalSources(sources: List<String>): List<PageDimensions> =
        withContext(Dispatchers.IO) {
            sources.map { source ->
                val file = localFile(source)
                if (file == null || !file.isFile) return@map PageDimensions()
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, options)
                PageDimensions(options.outWidth.coerceAtLeast(0), options.outHeight.coerceAtLeast(0))
            }
        }

    @Suppress("DEPRECATION")
    private fun decodeRegion(file: File, tile: WebtoonTile, claimed: PageDimensions): Bitmap {
        val decoder = BitmapRegionDecoder.newInstance(file.absolutePath, false)
            ?: error("Формат страницы не поддерживает плиточное чтение")
        return try {
            val sourceWidth = decoder.width.coerceAtLeast(1)
            val sourceHeight = decoder.height.coerceAtLeast(1)
            val region = mapTileToSource(tile, claimed, sourceWidth, sourceHeight)
            var sample = 1
            while (sourceWidth / sample > MAX_DECODE_WIDTH) sample *= 2
            val options = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = decoder.decodeRegion(
                Rect(region.left, region.top, region.right, region.bottom),
                options,
            ) ?: error("Не удалось декодировать фрагмент страницы")
            cropToRequested(decoded, region.width, region.height, sample)
        } finally {
            decoder.recycle()
            file.setLastModified(System.currentTimeMillis())
        }
    }

    /** JPEG/WebP MCU может отдать пиксели соседней плитки — без обрезки куски наплывают. */
    private fun cropToRequested(bitmap: Bitmap, regionWidth: Int, regionHeight: Int, sample: Int): Bitmap {
        val expectedW = (regionWidth / sample).coerceAtLeast(1)
        val expectedH = (regionHeight / sample).coerceAtLeast(1)
        if (bitmap.width <= expectedW && bitmap.height <= expectedH) return bitmap
        val width = minOf(expectedW, bitmap.width)
        val height = minOf(expectedH, bitmap.height)
        val cropped = Bitmap.createBitmap(bitmap, 0, 0, width, height)
        if (cropped != bitmap) bitmap.recycle()
        return cropped
    }

    private fun scale(value: Int, from: Int, to: Int): Int {
        if (from <= 0 || from == to) return value
        return ((value.toLong() * to) / from).toInt()
    }

    private suspend fun sourceFile(context: Context, source: String, retry: Int): File {
        localFile(source)?.takeIf { it.isFile }?.let { return it }
        val cacheDir = File(context.cacheDir, "webtoon_sources").apply { mkdirs() }
        val key = sha256(source)
        val destination = File(cacheDir, "$key.source")
        val lock = sourceLocks.getOrPut(key) { Mutex() }
        return lock.withLock {
            if (isImage(destination)) {
                destination.setLastModified(System.currentTimeMillis())
                return@withLock destination
            }
            destination.delete()
            val part = File(cacheDir, "$key.part")
            var lastError: Throwable? = null
            val candidates = MediaUrl.candidates(source).ifEmpty { listOf(source) }
            val ordered = candidates.drop(retry % candidates.size) + candidates.take(retry % candidates.size)
            for (candidate in ordered) {
                try {
                    part.delete()
                    val request = Request.Builder()
                        .url(candidate)
                        .header("Accept", "image/avif,image/webp,image/*,*/*;q=0.8")
                        .get()
                        .build()
                    client(context).newCall(request).execute().use { response ->
                        if (!response.isSuccessful) error("HTTP ${response.code}")
                        val body = response.body ?: error("Пустой ответ изображения")
                        body.byteStream().use { input ->
                            part.outputStream().use { output -> input.copyTo(output) }
                        }
                    }
                    if (!isImage(part)) error("Сервер вернул повреждённое изображение")
                    if (!part.renameTo(destination)) {
                        part.copyTo(destination, overwrite = true)
                        part.delete()
                    }
                    trimCache(cacheDir, destination)
                    return@withLock destination
                } catch (failure: Throwable) {
                    lastError = failure
                    part.delete()
                }
            }
            throw lastError ?: IllegalStateException("Не удалось загрузить страницу")
        }
    }

    private fun client(context: Context): OkHttpClient = mediaClient ?: synchronized(this) {
        mediaClient ?: NetworkModule.createMediaClient(context).also { mediaClient = it }
    }

    private fun localFile(source: String): File? = runCatching {
        when {
            source.startsWith("file:") -> File(requireNotNull(Uri.parse(source).path))
            source.startsWith("/") -> File(source)
            else -> null
        }
    }.getOrNull()

    private fun isImage(file: File): Boolean {
        if (!file.isFile || file.length() < 64L) return false
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        return options.outWidth > 0 && options.outHeight > 0
    }

    private fun trimCache(directory: File, keep: File) {
        val files = directory.listFiles()?.filter { it.isFile && it.extension == "source" }.orEmpty()
        var total = files.sumOf { it.length() }
        if (total <= CACHE_TRIM_AT) return
        files.sortedBy { it.lastModified() }.forEach { file ->
            if (total <= CACHE_TARGET) return
            if (file != keep) {
                val size = file.length()
                if (file.delete()) total -= size
            }
        }
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray())
        .joinToString("") { "%02x".format(it) }
}

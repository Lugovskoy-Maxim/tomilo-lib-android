package ru.tomilo.lib.mobile.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.tomilo.lib.mobile.data.api.NetworkModule
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

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

/** Запрос региона, выровненный по MCU, и сдвиг обрезки обратно к исходной плитке. */
data class AlignedDecode(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val cropLeft: Int,
    val cropTop: Int,
    val contentWidth: Int,
    val contentHeight: Int,
)

/**
 * Плиточный декод не смог отдать фрагмент. [file] — уже скачанный оригинал,
 * его можно отдать в Coil без второго HTTP. Кэш `webtoon_sources` при этом стирается.
 */
class PageDecodeFallback(
    val file: File?,
    message: String,
) : IllegalStateException(message)

/**
 * Декодирует длинные страницы вебтуна регионами. Высоту плитки снаружи
 * ограничивают примерно одним экраном, но не выше 4096 px.
 */
object WebtoonTiles {
    const val MAX_TILE_SOURCE_HEIGHT = 4_096
    // Степенной inSampleSize у Android не должен перескочить с ~1500 сразу
    // до 750 px на исходниках шириной 3K — сохраняем запас для QHD-экранов.
    private const val MAX_DECODE_WIDTH = 2_048
    private const val CACHE_TRIM_AT = 600L * 1024L * 1024L
    private const val CACHE_TARGET = 450L * 1024L * 1024L
    private const val DOWNLOAD_PERMITS = 2

    private val sourceLocks = ConcurrentHashMap<String, Mutex>()
    private val downloads = Semaphore(DOWNLOAD_PERMITS)
    private val activeDecodes = ConcurrentHashMap<String, AtomicInteger>()
    private val poisoned = ConcurrentHashMap.newKeySet<String>()
    @Volatile private var mediaClient: OkHttpClient? = null

    fun split(dimensions: PageDimensions, maxTileHeight: Int = MAX_TILE_SOURCE_HEIGHT): List<WebtoonTile> {
        if (!dimensions.isValid()) return emptyList()
        val tileHeight = maxTileHeight.coerceIn(1, MAX_TILE_SOURCE_HEIGHT)
        return buildList {
            var top = 0
            var index = 0
            while (top < dimensions.height) {
                val height = minOf(tileHeight, dimensions.height - top)
                add(WebtoonTile(index = index++, top = top, width = dimensions.width, height = height))
                top += height
            }
        }
    }

    /** Высота плитки в пикселях исходника, чтобы на экране она была около одного экрана. */
    fun maxTileHeightFor(dimensions: PageDimensions, screenWidthPx: Int, screenHeightPx: Int): Int {
        val screenW = screenWidthPx.coerceAtLeast(1)
        val screenH = screenHeightPx.coerceAtLeast(1)
        val imageW = dimensions.width.coerceAtLeast(1)
        val raw = (screenH.toLong() * imageW / screenW).toInt()
        return raw.coerceIn(512, MAX_TILE_SOURCE_HEIGHT)
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

    /** Выравнивает регион по 16 px внутри кадра. Лишнее потом обрезается. */
    fun alignToMcu(rect: SourceRect, sourceWidth: Int, sourceHeight: Int): AlignedDecode {
        val srcW = sourceWidth.coerceAtLeast(1)
        val srcH = sourceHeight.coerceAtLeast(1)
        val reqLeft = rect.left.coerceIn(0, srcW - 1)
        val reqTop = rect.top.coerceIn(0, srcH - 1)
        val reqRight = rect.right.coerceIn(reqLeft + 1, srcW)
        val reqBottom = rect.bottom.coerceIn(reqTop + 1, srcH)
        val alignedLeft = (reqLeft / 16) * 16
        val alignedTop = (reqTop / 16) * 16
        var alignedRight = ((reqRight + 15) / 16) * 16
        var alignedBottom = ((reqBottom + 15) / 16) * 16
        if (alignedRight > srcW) alignedRight = srcW
        if (alignedBottom > srcH) alignedBottom = srcH
        if (alignedRight <= alignedLeft) alignedRight = (alignedLeft + 1).coerceAtMost(srcW)
        if (alignedBottom <= alignedTop) alignedBottom = (alignedTop + 1).coerceAtMost(srcH)
        return AlignedDecode(
            left = alignedLeft,
            top = alignedTop,
            right = alignedRight,
            bottom = alignedBottom,
            cropLeft = reqLeft - alignedLeft,
            cropTop = reqTop - alignedTop,
            contentWidth = reqRight - reqLeft,
            contentHeight = reqBottom - reqTop,
        )
    }

    fun isMostlyBlack(bitmap: Bitmap): Boolean {
        if (bitmap.isRecycled || bitmap.width < 2 || bitmap.height < 2) return true
        val stepX = (bitmap.width / 16).coerceAtLeast(1)
        val stepY = (bitmap.height / 16).coerceAtLeast(1)
        var dark = 0
        var total = 0
        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val color = bitmap.getPixel(x, y)
                val r = (color shr 16) and 0xFF
                val g = (color shr 8) and 0xFF
                val b = color and 0xFF
                if (r < 12 && g < 12 && b < 12) dark++
                total++
                x += stepX
            }
            y += stepY
        }
        return total > 0 && dark * 100 / total >= 96
    }

    suspend fun decode(
        context: Context,
        source: String,
        tile: WebtoonTile,
        claimed: PageDimensions,
        retry: Int = 0,
    ): Bitmap = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val file = sourceFile(appContext, source, retry)
        decodeRegion(appContext, file, tile, claimed)
    }

    suspend fun measureSource(context: Context, source: String, retry: Int = 0): PageDimensions =
        withContext(Dispatchers.IO) {
            if (source.isBlank()) return@withContext PageDimensions()
            val file = sourceFile(context.applicationContext, source, retry)
            boundsOf(file)
        }

    suspend fun measureLocalSources(sources: List<String>): List<PageDimensions> =
        withContext(Dispatchers.IO) {
            sources.map { source ->
                if (source.isBlank()) return@map PageDimensions()
                val file = localFile(source)
                if (file == null || !file.isFile) return@map PageDimensions()
                boundsOf(file)
            }
        }

    /** Уже скачанный оригинал: локальный файл или кэш плиток. Без сети. */
    fun peekCachedFile(context: Context, source: String): File? {
        if (source.isBlank()) return null
        localFile(source)?.takeIf { it.isFile }?.let { return it }
        if (isLocalSource(source)) return null
        val destination = cacheFile(context.applicationContext, source)
        if (destination.absolutePath in poisoned) return null
        return destination.takeIf { isImage(it) }
    }

    fun evict(context: Context, source: String) {
        if (source.isBlank() || isLocalSource(source)) return
        val destination = cacheFile(context.applicationContext, source)
        poisoned.remove(destination.absolutePath)
        destination.delete()
    }

    private fun decodeRegion(context: Context, file: File, tile: WebtoonTile, claimed: PageDimensions): Bitmap {
        acquire(file)
        try {
            val decoder = openRegionDecoder(file)
            if (decoder == null) {
                poison(context, file)
                throw PageDecodeFallback(file, "Формат страницы не поддерживает плиточное чтение")
            }
            try {
                val sourceWidth = decoder.width.coerceAtLeast(1)
                val sourceHeight = decoder.height.coerceAtLeast(1)
                val region = mapTileToSource(tile, claimed, sourceWidth, sourceHeight)
                var sample = 1
                while (sourceWidth / sample > MAX_DECODE_WIDTH) sample *= 2
                var bitmap = decodeOnce(decoder, region, sourceWidth, sourceHeight, sample)
                if (bitmap != null && !isMostlyBlack(bitmap)) return bitmap
                bitmap?.let { if (!it.isRecycled) it.recycle() }
                if (sample != 1) {
                    bitmap = decodeOnce(decoder, region, sourceWidth, sourceHeight, 1)
                    if (bitmap != null && !isMostlyBlack(bitmap)) return bitmap
                    bitmap?.let { if (!it.isRecycled) it.recycle() }
                }
                poison(context, file)
                throw PageDecodeFallback(file, "Не удалось декодировать фрагмент страницы")
            } finally {
                decoder.recycle()
                if (file.exists()) file.setLastModified(System.currentTimeMillis())
            }
        } finally {
            release(file)
        }
    }

    @Suppress("DEPRECATION")
    private fun openRegionDecoder(file: File): BitmapRegionDecoder? = try {
        BitmapRegionDecoder.newInstance(file.absolutePath, false)
    } catch (failure: Throwable) {
        if (failure is CancellationException) throw failure
        null
    }

    private fun decodeOnce(
        decoder: BitmapRegionDecoder,
        region: SourceRect,
        sourceWidth: Int,
        sourceHeight: Int,
        sample: Int,
    ): Bitmap? {
        val aligned = alignToMcu(region, sourceWidth, sourceHeight)
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample.coerceAtLeast(1)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = decoder.decodeRegion(
            Rect(aligned.left, aligned.top, aligned.right, aligned.bottom),
            options,
        ) ?: return null
        return cropAligned(decoded, aligned, options.inSampleSize.coerceAtLeast(1))
    }

    private fun cropAligned(bitmap: Bitmap, aligned: AlignedDecode, sample: Int): Bitmap {
        val cropX = (aligned.cropLeft / sample).coerceAtLeast(0)
        val cropY = (aligned.cropTop / sample).coerceAtLeast(0)
        val wantW = (aligned.contentWidth / sample).coerceAtLeast(1)
        val wantH = (aligned.contentHeight / sample).coerceAtLeast(1)
        if (cropX == 0 && cropY == 0 && bitmap.width <= wantW && bitmap.height <= wantH) return bitmap
        val width = minOf(wantW, bitmap.width - cropX).coerceAtLeast(1)
        val height = minOf(wantH, bitmap.height - cropY).coerceAtLeast(1)
        if (cropX >= bitmap.width || cropY >= bitmap.height) return bitmap
        val cropped = Bitmap.createBitmap(bitmap, cropX, cropY, width, height)
        if (cropped != bitmap) bitmap.recycle()
        return cropped
    }

    private fun boundsOf(file: File): PageDimensions {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        return PageDimensions(options.outWidth.coerceAtLeast(0), options.outHeight.coerceAtLeast(0))
    }

    private fun scale(value: Int, from: Int, to: Int): Int {
        if (from <= 0 || from == to) return value
        return ((value.toLong() * to) / from).toInt()
    }

    private suspend fun sourceFile(context: Context, source: String, retry: Int): File {
        localFile(source)?.takeIf { it.isFile }?.let { return it }
        if (isLocalSource(source)) error("Локальная страница не найдена")
        val cacheDir = File(context.cacheDir, "webtoon_sources").apply { mkdirs() }
        val key = sha256(source)
        val destination = File(cacheDir, "$key.source")
        val lock = sourceLocks.getOrPut(key) { Mutex() }
        return lock.withLock {
            if (isImage(destination) && destination.absolutePath !in poisoned) {
                destination.setLastModified(System.currentTimeMillis())
                return@withLock destination
            }
            poisoned.remove(destination.absolutePath)
            destination.delete()
            downloads.withPermit {
                if (isImage(destination) && destination.absolutePath !in poisoned) {
                    return@withPermit destination
                }
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
                        if (failure is CancellationException) throw failure
                        lastError = failure
                        part.delete()
                    }
                }
                throw lastError ?: IllegalStateException("Не удалось загрузить страницу")
            }
        }
    }

    private fun client(context: Context): OkHttpClient = mediaClient ?: synchronized(this) {
        mediaClient ?: NetworkModule.createMediaClient(context).also { mediaClient = it }
    }

    private fun isLocalSource(source: String): Boolean =
        source.startsWith("file:") || source.startsWith("content:") || source.startsWith("/")

    private fun localFile(source: String): File? = runCatching {
        when {
            source.startsWith("file:") -> File(requireNotNull(Uri.parse(source).path))
            source.startsWith("/") -> File(source)
            else -> null
        }
    }.getOrNull()

    private fun cacheFile(context: Context, source: String): File {
        val cacheDir = File(context.cacheDir, "webtoon_sources")
        return File(cacheDir, sha256(source) + ".source")
    }

    private fun isCacheFile(context: Context, file: File): Boolean {
        val root = File(context.cacheDir, "webtoon_sources").absolutePath
        return file.absolutePath.startsWith(root)
    }

    private fun isImage(file: File): Boolean {
        if (!file.isFile || file.length() < 64L) return false
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        return options.outWidth > 0 && options.outHeight > 0
    }

    private fun acquire(file: File) {
        activeDecodes.compute(file.absolutePath) { _, current ->
            val counter = current ?: AtomicInteger(0)
            counter.incrementAndGet()
            counter
        }
    }

    private fun release(file: File) {
        activeDecodes.compute(file.absolutePath) { _, current ->
            if (current == null) return@compute null
            val left = current.decrementAndGet()
            if (left <= 0) null else current
        }
    }

    /**
     * Битый кэш не отдаём плиткам повторно, но файл оставляем:
     * Coil читает его с диска, без второго HTTP. Удаление — в [evict]
     * или когда та же страница качается заново.
     */
    private fun poison(context: Context, file: File) {
        if (isCacheFile(context, file)) poisoned.add(file.absolutePath)
    }

    private fun trimCache(directory: File, keep: File) {
        val files = directory.listFiles()?.filter { it.isFile && it.extension == "source" }.orEmpty()
        var total = files.sumOf { it.length() }
        if (total <= CACHE_TRIM_AT) return
        files.sortedBy { it.lastModified() }.forEach { file ->
            if (total <= CACHE_TARGET) return
            if (file == keep) return@forEach
            if (file.absolutePath in poisoned) return@forEach
            if ((activeDecodes[file.absolutePath]?.get() ?: 0) > 0) return@forEach
            val size = file.length()
            if (file.delete()) total -= size
        }
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray())
        .joinToString("") { "%02x".format(it) }
}

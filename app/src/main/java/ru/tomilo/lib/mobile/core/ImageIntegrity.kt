package ru.tomilo.lib.mobile.core

import android.graphics.BitmapFactory
import java.io.File
import java.io.RandomAccessFile

/**
 * Проверяет, что файл — целая картинка, а не обрезанный ответ CDN / HTML-заглушка.
 * Обрезанные JPEG/PNG часто декодируются Coil с «ломаной» нижней частью.
 */
object ImageIntegrity {
    private const val MIN_BYTES = 64

    fun isValidFile(file: File): Boolean {
        if (!file.isFile || file.length() < MIN_BYTES) return false
        val header = file.readHead(16)
        val tail = file.readTail(64)
        if (!hasValidContainer(header, tail, file.length())) return false
        return canDecodeBounds(file)
    }

    fun isValidBytes(bytes: ByteArray): Boolean {
        if (bytes.size < MIN_BYTES) return false
        val header = bytes.copyOfRange(0, 16.coerceAtMost(bytes.size))
        val tailFrom = (bytes.size - 64).coerceAtLeast(0)
        val tail = bytes.copyOfRange(tailFrom, bytes.size)
        if (!hasValidContainer(header, tail, bytes.size.toLong())) return false
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        return decodeLooksUsable(opts, header)
    }

    private fun hasValidContainer(header: ByteArray, tail: ByteArray, size: Long): Boolean {
        return when {
            isJpeg(header) -> containsMarker(tail, 0xFF, 0xD9)
            isPng(header) -> containsPngIend(tail)
            isWebp(header) -> size >= 16L
            isGif(header) -> size >= 32L && (tail.lastOrNull() == 0x3B.toByte() || tail.size >= 2)
            isAvif(header) -> size >= 32L
            else -> false
        }
    }

    private fun canDecodeBounds(file: File): Boolean {
        val header = file.readHead(16)
        if (isAvif(header)) return true
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, opts)
        return decodeLooksUsable(opts, header)
    }

    private fun decodeLooksUsable(opts: BitmapFactory.Options, header: ByteArray): Boolean {
        if (opts.outWidth >= 8 && opts.outHeight >= 8) return true
        // AVIF / редкие форматы: bounds может не сработать на старых API
        return isAvif(header) || isWebp(header)
    }

    private fun isJpeg(h: ByteArray) = h.size >= 3 &&
        h[0] == 0xFF.toByte() && h[1] == 0xD8.toByte() && h[2] == 0xFF.toByte()

    private fun isPng(h: ByteArray) = h.size >= 8 &&
        h[0] == 0x89.toByte() && h[1] == 0x50.toByte() && h[2] == 0x4E.toByte() && h[3] == 0x47.toByte()

    private fun isWebp(h: ByteArray) = h.size >= 12 &&
        h[0] == 'R'.code.toByte() && h[1] == 'I'.code.toByte() &&
        h[2] == 'F'.code.toByte() && h[3] == 'F'.code.toByte() &&
        h[8] == 'W'.code.toByte() && h[9] == 'E'.code.toByte() &&
        h[10] == 'B'.code.toByte() && h[11] == 'P'.code.toByte()

    private fun isGif(h: ByteArray) = h.size >= 6 &&
        h[0] == 'G'.code.toByte() && h[1] == 'I'.code.toByte() && h[2] == 'F'.code.toByte()

    private fun isAvif(h: ByteArray): Boolean {
        if (h.size < 12) return false
        val brand = String(h, 4, 8, Charsets.US_ASCII)
        return brand.startsWith("ftyp") &&
            (brand.contains("avif") || brand.contains("avis") || brand.contains("mif1"))
    }

    private fun containsMarker(bytes: ByteArray, a: Int, b: Int): Boolean {
        if (bytes.size < 2) return false
        for (i in 0 until bytes.size - 1) {
            if (bytes[i] == a.toByte() && bytes[i + 1] == b.toByte()) return true
        }
        return false
    }

    private fun containsPngIend(tail: ByteArray): Boolean {
        val marker = byteArrayOf(0x49, 0x45, 0x4E, 0x44)
        if (tail.size < 8) return false
        outer@ for (i in 0..tail.size - 4) {
            for (j in 0..3) {
                if (tail[i + j] != marker[j]) continue@outer
            }
            return true
        }
        return false
    }

    private fun File.readHead(n: Int): ByteArray {
        val size = n.coerceAtMost(length().toInt())
        return inputStream().use { it.readNBytesCompat(size) }
    }

    private fun File.readTail(n: Int): ByteArray {
        val len = length()
        if (len <= 0L) return ByteArray(0)
        val size = n.toLong().coerceAtMost(len).toInt()
        RandomAccessFile(this, "r").use { raf ->
            raf.seek(len - size)
            val buf = ByteArray(size)
            raf.readFully(buf)
            return buf
        }
    }

    private fun java.io.InputStream.readNBytesCompat(n: Int): ByteArray {
        val buf = ByteArray(n)
        var off = 0
        while (off < n) {
            val read = read(buf, off, n - off)
            if (read <= 0) break
            off += read
        }
        return if (off == n) buf else buf.copyOf(off)
    }
}

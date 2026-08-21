package ru.tomilo.lib.mobile.core

import ru.tomilo.lib.mobile.BuildConfig

object MediaUrl {
    private val cdn = BuildConfig.CDN_BASE_URL.trimEnd('/')
    private val s3 = BuildConfig.S3_BASE_URL.trimEnd('/')
    private val site = BuildConfig.SITE_URL.trimEnd('/')

    fun resolve(pathOrUrl: String?): String {
        if (pathOrUrl.isNullOrBlank()) return ""
        val raw = pathOrUrl.trim()
        if (raw.startsWith("http://") || raw.startsWith("https://")) {
            return rewriteToCdn(raw)
        }
        val key = normalizeKey(raw)
        return "$cdn$key"
    }

    /**
     * Источники одной картинки в порядке приоритета. Для страниц главы прямой
     * S3 надёжнее: CDN может быстро ответить заголовками, но зависнуть на теле.
     */
    fun candidates(pathOrUrl: String?): List<String> {
        if (pathOrUrl.isNullOrBlank()) return emptyList()
        val raw = pathOrUrl.trim()
        if (raw.startsWith("file:") || raw.startsWith("content:")) {
            return listOf(raw)
        }
        if (raw.startsWith("http://") || raw.startsWith("https://")) {
            return try {
                val uri = java.net.URI(raw)
                if (!isTomiloMediaHost(uri.host.orEmpty())) return listOf(raw)
                val key = normalizeKey(uri.path.orEmpty())
                listOf("$s3$key", "$cdn$key", raw).distinct()
            } catch (_: Exception) {
                listOf(raw)
            }
        }
        val key = normalizeKey(raw)
        return listOf("$s3$key", "$cdn$key").distinct()
    }

    fun candidate(pathOrUrl: String?, attempt: Int): String {
        val urls = candidates(pathOrUrl)
        if (urls.isEmpty()) return ""
        return urls[attempt.coerceAtLeast(0) % urls.size]
    }

    private fun rewriteToCdn(url: String): String {
        return try {
            val u = java.net.URI(url)
            val host = u.host.orEmpty()
            val path = u.path.orEmpty()
            if (isTomiloMediaHost(host)) {
                val key = normalizeKey(path)
                if (key.isNotBlank()) "$cdn$key" else url
            } else {
                url
            }
        } catch (_: Exception) {
            url
        }
    }

    private fun isTomiloMediaHost(host: String): Boolean =
        host.contains("s3.regru.cloud") ||
            host.contains("s3.regcloud.ru") ||
            host.contains("tomilo-lib.ru") ||
            host == "localhost" ||
            host == "127.0.0.1"

    private fun normalizeKey(p: String): String {
        var path = if (p.startsWith("/")) p else "/$p"
        path = path
            .removePrefix("/api")
            .removePrefix("/uploads")
            .removePrefix("/tomilolib")
        if (!path.startsWith("/")) path = "/$path"
        return path
    }

    fun siteOrigin(): String = site
}

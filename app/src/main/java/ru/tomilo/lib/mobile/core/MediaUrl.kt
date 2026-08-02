package ru.tomilo.lib.mobile.core

import ru.tomilo.lib.mobile.BuildConfig

object MediaUrl {
    private val cdn = BuildConfig.CDN_BASE_URL.trimEnd('/')
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

    private fun rewriteToCdn(url: String): String {
        return try {
            val u = java.net.URI(url)
            val host = u.host.orEmpty()
            val path = u.path.orEmpty()
            val isS3 = host.contains("s3.regru.cloud") || host.contains("s3.regcloud.ru")
            val isSite = host.contains("tomilo-lib.ru") || host == "localhost"
            if (isS3 || isSite) {
                val key = normalizeKey(path)
                if (key.isNotBlank()) "$cdn$key" else url
            } else {
                url
            }
        } catch (_: Exception) {
            url
        }
    }

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

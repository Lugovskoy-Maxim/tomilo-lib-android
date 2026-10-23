package ru.tomilo.lib.mobile.core

import ru.tomilo.lib.mobile.BuildConfig

object MediaUrl {
    private val cdn = BuildConfig.CDN_BASE_URL.trimEnd('/')
    private val s3 = BuildConfig.S3_BASE_URL.trimEnd('/')
    private val site = BuildConfig.SITE_URL.trimEnd('/')
    private val signedQueryKeys = setOf(
        "signature",
        "sig",
        "token",
        "access_token",
        "awsaccesskeyid",
        "key-pair-id",
        "expires",
        "x-amz-signature",
        "x-goog-signature",
    )
    private val decorationPreviewWidths = intArrayOf(96, 192, 320, 640, 1280)
    private val safeDecorationRevisionParams = setOf("v", "version", "t", "updated", "w", "h", "width", "height", "quality")

    fun resolve(pathOrUrl: String?): String {
        if (pathOrUrl.isNullOrBlank()) return ""
        val raw = pathOrUrl.trim()
        if (raw.startsWith("file:") || raw.startsWith("content:")) return raw
        if (raw.startsWith("http://") || raw.startsWith("https://")) {
            val uri = runCatching { java.net.URI(raw) }.getOrNull()
            uri?.takeIf { isTomiloMediaHost(it.host.orEmpty()) && !hasSignedQuery(it) }
                ?.let { siteAssetUrl(it.rawPath.orEmpty(), uriSuffix(it)) }
                ?.let { return it }
            uri?.takeIf { isTomiloMediaHost(it.host.orEmpty()) }
                ?.let {
                    shopImageApiUrl(
                        it.rawPath.orEmpty(),
                        uriSuffix(it),
                        shopImageApiOrigin(it),
                    )
                }
                ?.let { return it }
            return rewriteToCdn(raw)
        }
        val uri = runCatching { java.net.URI(raw) }.getOrNull()
        val path = uri?.rawPath?.takeIf(String::isNotBlank) ?: raw.substringBefore('?')
        val suffix = uri?.let(::uriSuffix).orEmpty()
        siteAssetUrl(path, suffix)?.let { return it }
        shopImageApiUrl(path, suffix)?.let { return it }
        val key = normalizeKey(uri?.rawPath?.takeIf(String::isNotBlank) ?: raw)
        return "$cdn$key$suffix"
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
                if (!hasSignedQuery(uri)) {
                    siteAssetUrl(uri.rawPath.orEmpty(), uriSuffix(uri))
                        ?.let { return listOf(it) }
                }
                val shopImageApi = shopImageApiUrl(
                    uri.rawPath.orEmpty(),
                    uriSuffix(uri),
                    shopImageApiOrigin(uri),
                )
                if (shopImageApi != null) {
                    return shopImageCandidates(uri.rawPath.orEmpty(), uriSuffix(uri), shopImageApi)
                }
                if (hasSignedQuery(uri)) return listOf(raw)
                val key = normalizeKey(uri.rawPath.orEmpty())
                val suffix = uriSuffix(uri)
                listOf("$s3$key$suffix", "$cdn$key$suffix", raw).distinct()
            } catch (_: Exception) {
                listOf(raw)
            }
        }
        val uri = runCatching { java.net.URI(raw) }.getOrNull()
        val path = uri?.rawPath?.takeIf(String::isNotBlank) ?: raw.substringBefore('?')
        val suffix = uri?.let(::uriSuffix).orEmpty()
        siteAssetUrl(path, suffix)?.let { return listOf(it) }
        shopImageApiUrl(path, suffix)?.let { return shopImageCandidates(path, suffix, it) }
        val key = normalizeKey(uri?.path?.takeIf(String::isNotBlank) ?: raw)
        return listOf("$s3$key$suffix", "$cdn$key$suffix").distinct()
    }

    private fun shopImageCandidates(path: String, suffix: String, apiUrl: String): List<String> {
        val storageUrl = shopCardArtStorageUrl(path, suffix)
        return if (storageUrl != null) listOf(storageUrl, apiUrl).distinct() else listOf(apiUrl)
    }

    /**
     * Card-art API responses can stall after their headers. The same public object
     * is available from S3, so cards try storage first and retain the API route as fallback.
     */
    private fun shopCardArtStorageUrl(path: String, suffix: String): String? {
        if (suffix.isNotEmpty()) return null
        val assetToken = Regex("^/(?:api/)?shop/images/asset/([A-Za-z0-9_-]+)$")
            .matchEntire(path)
            ?.groupValues
            ?.getOrNull(1)
            ?: return null
        val key = runCatching {
            String(java.util.Base64.getUrlDecoder().decode(assetToken), Charsets.UTF_8)
        }.getOrNull() ?: return null
        if (!Regex("^game-items/card-art/[A-Za-z0-9._-]+$").matches(key)) return null
        return "$s3/$key"
    }

    /** Prefer the server-side media cache for small catalogue images. */
    fun coverCandidates(pathOrUrl: String?): List<String> {
        val urls = candidates(pathOrUrl)
        if (urls.size < 2) return urls
        val cdnHost = runCatching { java.net.URI(cdn).host.orEmpty() }.getOrDefault("")
        if (cdnHost.isBlank()) return urls
        val cdnIndex = urls.indexOfFirst { candidate ->
            runCatching { java.net.URI(candidate).host.equals(cdnHost, ignoreCase = true) }
                .getOrDefault(false)
        }
        if (cdnIndex <= 0) return urls
        return buildList(urls.size) {
            add(urls[cdnIndex])
            urls.forEachIndexed { index, url -> if (index != cdnIndex) add(url) }
        }
    }

    /** Serve small avatar decorations as API-generated WebP previews when available. */
    fun decorationPreviewUrl(pathOrUrl: String?, requestedWidthPx: Int): String {
        if (pathOrUrl.isNullOrBlank()) return ""
        val raw = pathOrUrl.trim()
        if (raw.startsWith("file:") || raw.startsWith("content:")) return raw
        val uri = runCatching { java.net.URI(raw) }.getOrNull() ?: return raw
        val host = uri.host.orEmpty()
        if (host.isNotBlank() && !isTomiloMediaHost(host)) return raw
        if (!uri.rawFragment.isNullOrBlank()) return raw
        val originalQuery = uri.rawQuery.orEmpty()
        if (originalQuery.isNotBlank() && hasSignedQuery(uri)) return raw
        val revisionParams = originalQuery.split('&').filter(String::isNotBlank).map { parameter ->
            val key = parameter.substringBefore('=').lowercase()
            val value = parameter.substringAfter('=', "")
            if (key !in safeDecorationRevisionParams || !Regex("^[A-Za-z0-9._-]{1,64}$").matches(value)) return raw
            key to value
        }

        val path = uri.path.orEmpty()
        val decorationPath = when {
            path.startsWith("/decorations/") -> path
            path.startsWith("/tomilolib/decorations/") -> path.removePrefix("/tomilolib")
            else -> return raw
        }
        val filename = decorationPath.removePrefix("/decorations/")
        if (!Regex("^[A-Za-z0-9][A-Za-z0-9_.-]{0,200}\\.(?:png|jpe?g|webp|gif|avif)$", RegexOption.IGNORE_CASE)
                .matches(filename) || filename.contains("..")) {
            return raw
        }

        val width = decorationPreviewWidths.firstOrNull { it >= requestedWidthPx.coerceAtLeast(1) }
            ?: decorationPreviewWidths.last()
        val revisionSeed = buildString {
            append("decorations/").append(filename)
            revisionParams.sortedBy { it.first }.forEach { (key, value) -> append('|').append(key).append('=').append(value) }
        }
        val revision = stableRevision(revisionSeed)
        return "$site/api/shop/images/$filename?w=$width&v=$revision"
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
            val path = u.rawPath.orEmpty()
            if (isTomiloMediaHost(host)) {
                if (hasSignedQuery(u)) return url
                val key = normalizeKey(path)
                if (key.isNotBlank()) "$cdn$key${uriSuffix(u)}" else url
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

    /** `/assets/...` files are deployed with the website, not in the S3 media bucket. */
    private fun siteAssetUrl(path: String, suffix: String): String? {
        val normalizedPath = if (path.startsWith('/')) path else "/$path"
        return if (normalizedPath.startsWith("/assets/")) {
            "$site$normalizedPath$suffix"
        } else {
            null
        }
    }

    /** Shop images are served by the API's Redis-backed route, not as raw CDN objects. */
    private fun shopImageApiUrl(
        path: String,
        suffix: String,
        apiOrigin: String = site,
    ): String? {
        val normalizedPath = if (path.startsWith('/')) path else "/$path"
        if (!normalizedPath.startsWith("/shop/images/") &&
            !normalizedPath.startsWith("/api/shop/images/")
        ) {
            return null
        }
        val apiPath = if (normalizedPath.startsWith("/api/")) normalizedPath else "/api$normalizedPath"
        return "$apiOrigin$apiPath$suffix"
    }

    private fun shopImageApiOrigin(uri: java.net.URI): String {
        val host = uri.host.orEmpty()
        return if (
            host.equals("localhost", ignoreCase = true) ||
            host.equals("127.0.0.1", ignoreCase = true) ||
            host.equals(runCatching { java.net.URI(site).host }.getOrNull(), ignoreCase = true)
        ) {
            "${uri.scheme}://${uri.rawAuthority}"
        } else {
            site
        }
    }

    private fun uriSuffix(uri: java.net.URI): String = buildString {
        uri.rawQuery?.let { append('?').append(it) }
        uri.rawFragment?.let { append('#').append(it) }
    }

    private fun hasSignedQuery(uri: java.net.URI): Boolean {
        return uri.rawQuery.orEmpty().split('&').any { parameter ->
            val key = parameter.substringBefore('=').lowercase()
            key in signedQueryKeys || key.startsWith("x-amz-") || key.startsWith("x-goog-")
        }
    }

    private fun stableRevision(value: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
        val digits = "0123456789abcdef"
        return buildString(16) {
            for (index in 0 until 8) {
                val byte = digest[index].toInt() and 0xff
                append(digits[byte ushr 4])
                append(digits[byte and 0x0f])
            }
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

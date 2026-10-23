package ru.tomilo.lib.mobile.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaUrlTest {
    @Test
    fun cdnUrlHasDirectS3Fallback() {
        val path = "/titles/title-id/chapters/chapter-id/001.webp"
        val urls = MediaUrl.candidates("https://cdn.tomilo-lib.ru$path")

        assertEquals("https://s3.regru.cloud/tomilolib$path", urls[0])
        assertEquals("https://cdn.tomilo-lib.ru$path", urls[1])
    }

    @Test
    fun absoluteCdnCoverPrefersCacheAndKeepsS3Fallback() {
        val source = "https://cdn.tomilo-lib.ru/titles/6ac6c1f38fdcd24936185d02/cover./api/wamanga/cover-proxy"
        val urls = MediaUrl.coverCandidates(source)

        assertEquals(source, urls[0])
        assertEquals(
            "https://s3.regru.cloud/tomilolib/titles/6ac6c1f38fdcd24936185d02/cover./api/wamanga/cover-proxy",
            urls[1],
        )
    }

    @Test
    fun legacyRelativePathIsNormalizedForBothOrigins() {
        val urls = MediaUrl.candidates("/uploads/titles/a/chapters/b/001.jpeg")

        assertEquals("https://s3.regru.cloud/tomilolib/titles/a/chapters/b/001.jpeg", urls[0])
        assertEquals("https://cdn.tomilo-lib.ru/titles/a/chapters/b/001.jpeg", urls[1])
    }

    @Test
    fun localFileNeverGetsRewritten() {
        val local = "file:/data/user/0/ru.tomilo.lib.mobile/files/offline/001.webp"
        assertEquals(listOf(local), MediaUrl.candidates(local))
        assertTrue(MediaUrl.candidate(local, 2).startsWith("file:"))
    }

    @Test
    fun cardArtworkApiUrlStaysOnSite() {
        val url = "https://tomilo-lib.ru/api/shop/images/asset/example"
        assertEquals(url, MediaUrl.resolve(url))
        assertEquals(listOf(url), MediaUrl.candidates(url))
    }

    @Test
    fun profileBadgeAssetUsesSiteInsteadOfCdn() {
        val path = "/assets/shop-badges/reader-spark.webp"
        val url = "https://tomilo-lib.ru$path"
        assertEquals(url, MediaUrl.resolve(path))
        assertEquals(url, MediaUrl.resolve(url))
        assertEquals(listOf(url), MediaUrl.candidates(path))
        val cdnUrl = "https://cdn.tomilo-lib.ru$path"
        assertEquals(url, MediaUrl.resolve(cdnUrl))
        assertEquals(listOf(url), MediaUrl.candidates(cdnUrl))
        val legacyPath = path.removePrefix("/")
        assertEquals(url, MediaUrl.resolve(legacyPath))
        assertEquals(listOf(url), MediaUrl.candidates(legacyPath))
    }

    @Test
    fun decorationImagesUseServerWebpPreviewsAndKeepSignedOrExternalUrls() {
        val source = "https://cdn.tomilo-lib.ru/decorations/decoration-123.webp?v=42"
        val preview = MediaUrl.decorationPreviewUrl(source, requestedWidthPx = 150)

        assertTrue(preview.matches(Regex("https://tomilo-lib\\.ru/api/shop/images/decoration-123\\.webp\\?w=192&v=[a-f0-9]{16}")))
        assertEquals(preview, MediaUrl.decorationPreviewUrl(source, requestedWidthPx = 150))
        assertEquals(preview, MediaUrl.resolve(preview))
        assertEquals(listOf(preview), MediaUrl.candidates(preview))

        val signed = "https://s3.regru.cloud/tomilolib/decorations/decoration-123.webp?X-Amz-Signature=abc"
        assertEquals(signed, MediaUrl.decorationPreviewUrl(signed, requestedWidthPx = 96))
        val external = "https://images.example.org/decorations/decoration-123.webp"
        assertEquals(external, MediaUrl.decorationPreviewUrl(external, requestedWidthPx = 96))
    }

    @Test
    fun signedMediaUrlKeepsOriginalHostAndQuery() {
        val url = "https://s3.regru.cloud/tomilolib/page.webp?X-Amz-Signature=abc&X-Amz-Expires=60"

        assertEquals(url, MediaUrl.resolve(url))
        assertEquals(listOf(url), MediaUrl.candidates(url))
    }
}

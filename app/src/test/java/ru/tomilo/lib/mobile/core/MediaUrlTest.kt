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
    }

    @Test
    fun signedMediaUrlKeepsOriginalHostAndQuery() {
        val url = "https://s3.regru.cloud/tomilolib/page.webp?X-Amz-Signature=abc&X-Amz-Expires=60"

        assertEquals(url, MediaUrl.resolve(url))
        assertEquals(listOf(url), MediaUrl.candidates(url))
    }
}

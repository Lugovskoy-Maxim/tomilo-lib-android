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
}

package ru.tomilo.lib.mobile.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PageImagesTest {
    @Test
    fun signedS3UrlIsUnchangedAcrossRetries() {
        val signedUrl = "https://s3.regru.cloud/tomilolib/page.webp?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=reader%2Fscope&X-Amz-Expires=60&X-Amz-Signature=abc123"

        assertEquals(signedUrl, PageImages.retrySource(signedUrl, attempt = 0))
        assertEquals(signedUrl, PageImages.retrySource(signedUrl, attempt = 1))
        assertEquals(signedUrl, PageImages.retrySource(signedUrl, attempt = 2))
    }

    @Test
    fun retriesBypassBothImageAndHttpCachesWithoutChangingTheUrl() {
        assertNull(PageImages.retryCacheControl(attempt = 0))
        assertEquals("no-cache, no-store", PageImages.retryCacheControl(attempt = 1))
        assertEquals("no-cache, no-store", PageImages.retryCacheControl(attempt = 2))
    }
}

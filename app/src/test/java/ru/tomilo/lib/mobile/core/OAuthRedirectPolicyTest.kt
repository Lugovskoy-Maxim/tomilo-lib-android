package ru.tomilo.lib.mobile.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OAuthRedirectPolicyTest {
    private val redirect = "https://tomilo-lib.ru/auth/vk"

    @Test fun acceptsOnlyExactCallbackOriginAndPath() {
        assertTrue(OAuthRedirectPolicy.matches("$redirect?code=test&state=test", redirect))
        assertFalse(OAuthRedirectPolicy.matches("https://example.com/auth/vk?code=test", redirect))
        assertFalse(OAuthRedirectPolicy.matches("https://tomilo-lib.ru.evil.test/auth/vk", redirect))
        assertFalse(OAuthRedirectPolicy.matches("${redirect}other?code=test", redirect))
        assertFalse(OAuthRedirectPolicy.matches("http://tomilo-lib.ru/auth/vk", redirect))
        assertFalse(OAuthRedirectPolicy.matches("https://tomilo-lib.ru:444/auth/vk", redirect))
        assertFalse(OAuthRedirectPolicy.matches("not a url", redirect))
    }

    @Test fun requiresOriginalStateAndCompletePkceResponse() {
        assertTrue(OAuthRedirectPolicy.validVkResponse("verifier", "original", "original", "device"))
        assertFalse(OAuthRedirectPolicy.validVkResponse("verifier", "original", "", "device"))
        assertFalse(OAuthRedirectPolicy.validVkResponse("verifier", "original", "other", "device"))
        assertFalse(OAuthRedirectPolicy.validVkResponse("", "original", "original", "device"))
        assertFalse(OAuthRedirectPolicy.validVkResponse("verifier", "", "", "device"))
        assertFalse(OAuthRedirectPolicy.validVkResponse("verifier", "original", "original", ""))
    }
}

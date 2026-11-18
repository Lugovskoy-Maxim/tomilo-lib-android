package ru.tomilo.lib.mobile.core

import java.net.URI

internal object OAuthRedirectPolicy {
    fun matches(url: String, redirect: String): Boolean = runCatching {
        val actual = URI(url)
        val expected = URI(redirect)
        actual.scheme == expected.scheme && actual.host == expected.host &&
            actual.port == expected.port && actual.path == expected.path && actual.userInfo == null
    }.getOrDefault(false)

    fun validVkResponse(verifier: String, expectedState: String, state: String, deviceId: String): Boolean =
        verifier.isNotBlank() && expectedState.isNotBlank() && state == expectedState && deviceId.isNotBlank()
}

package ru.tomilo.lib.mobile.core

import org.junit.Assert.*
import org.junit.Test

class OAuthPkceTest {
    @Test fun challengeMatchesRfc7636Vector() {
        assertEquals(
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            OAuthPkce.challenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"),
        )
    }

    @Test fun transactionsHaveFreshUrlSafeVerifiers() {
        val values = List(100) { OAuthPkce.randomValue() }
        assertEquals(values.size, values.toSet().size)
        assertTrue(values.all { it.length == 43 && it.matches(Regex("[A-Za-z0-9_-]+")) })
    }
}

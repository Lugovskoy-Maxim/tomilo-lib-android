package ru.tomilo.lib.mobile.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdsConsentTest {

    @Test
    fun `parse maps stored values`() {
        assertEquals(AdsConsent.DENIED, AdsConsent.parse("denied"))
        assertEquals(AdsConsent.CONTEXTUAL, AdsConsent.parse("contextual"))
        assertEquals(AdsConsent.GRANTED, AdsConsent.parse("granted"))
        assertEquals(AdsConsent.GRANTED, AdsConsent.parse("personalized"))
    }

    @Test
    fun `parse falls back to unknown`() {
        assertEquals(AdsConsent.UNKNOWN, AdsConsent.parse(null))
        assertEquals(AdsConsent.UNKNOWN, AdsConsent.parse(""))
        assertEquals(AdsConsent.UNKNOWN, AdsConsent.parse("unexpected-value"))
    }

    @Test
    fun `allows ads only for contextual and granted`() {
        assertFalse(AdsConsent.UNKNOWN.allowsAds)
        assertFalse(AdsConsent.DENIED.allowsAds)
        assertTrue(AdsConsent.CONTEXTUAL.allowsAds)
        assertTrue(AdsConsent.GRANTED.allowsAds)
    }

    @Test
    fun `personalized only for granted`() {
        assertFalse(AdsConsent.UNKNOWN.personalized)
        assertFalse(AdsConsent.DENIED.personalized)
        assertFalse(AdsConsent.CONTEXTUAL.personalized)
        assertTrue(AdsConsent.GRANTED.personalized)
    }

    @Test
    fun `stored representation round-trips through parse`() {
        listOf(AdsConsent.DENIED, AdsConsent.CONTEXTUAL, AdsConsent.GRANTED).forEach { consent ->
            assertEquals(consent, AdsConsent.parse(consent.name.lowercase()))
        }
        assertEquals(AdsConsent.UNKNOWN, AdsConsent.parse(AdsConsent.UNKNOWN.name.lowercase()))
    }
}

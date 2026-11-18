package io.github.mrcoder20.ioclens.security

import io.github.mrcoder20.ioclens.domain.parser.IOCParser
import io.github.mrcoder20.ioclens.domain.parser.PrivacyGuardValidator
import io.github.mrcoder20.ioclens.domain.vision.QuishingAnalyzer
import io.github.mrcoder20.ioclens.domain.vision.QuishingRisk
import io.github.mrcoder20.ioclens.presentation.home.formatUtcTimestamp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdversarialHardeningVerificationTest {

    @Test
    fun testMultiAtAuthorityUserinfoBypassIsCaught() {
        val raw = "https://microsoft.com@login.microsoftonline.com@attacker.example/auth"
        val payload = QuishingAnalyzer.analyze(raw)
        assertEquals(QuishingRisk.HIGH_RISK, payload.quishingRisk)
        assertTrue(payload.riskReason?.contains("attacker.example") == true)
        assertTrue(payload.defangedText.contains("[@]"))
    }

    @Test
    fun testPlainAndDoubleEncodedOpenRedirectsAreExtracted() {
        val plain = "https://redirector.example/out?redirect=https://evil.example/steal"
        val plainPayload = QuishingAnalyzer.analyze(plain)
        assertEquals(QuishingRisk.HIGH_RISK, plainPayload.quishingRisk)
        assertEquals("https://evil.example/steal", plainPayload.unmaskedTargetUrl)

        val doubleEncoded = "https://redirector.example/out?next=https%253A%252F%252Fevil.example%252Fdrop"
        val doublePayload = QuishingAnalyzer.analyze(doubleEncoded)
        assertEquals(QuishingRisk.HIGH_RISK, doublePayload.quishingRisk)
        assertEquals("https://evil.example/drop", doublePayload.unmaskedTargetUrl)
    }

    @Test
    fun testRawIpv6LiteralAndControlCharSchemeAreFlagged() {
        val ipv6Payload = QuishingAnalyzer.analyze("http://[2001:4860:4860::8888]/stager.bin")
        assertEquals(QuishingRisk.HIGH_RISK, ipv6Payload.quishingRisk)
        assertTrue(ipv6Payload.riskReason?.contains("IPv6") == true)

        val tabSmuggledJs = QuishingAnalyzer.analyze("java\tscript:alert(1)")
        assertEquals(QuishingRisk.HIGH_RISK, tabSmuggledJs.quishingRisk)
        assertTrue(tabSmuggledJs.riskReason?.contains("javascript") == true)
    }

    @Test
    fun testPrivacyGuardCustomRulesAndRfc5737PublicInvariant() {
        val parser = IOCParser()
        val testNet = parser.parse("203.0.113.42").toIndicator()
        assertFalse(PrivacyGuardValidator.isPrivateOrInternal(testNet))

        val corpDomain = parser.parse("vault.corp.example").toIndicator()
        assertFalse(PrivacyGuardValidator.isPrivateOrInternal(corpDomain))
        assertTrue(PrivacyGuardValidator.isPrivateOrInternal(corpDomain, listOf(".corp.example")))
        assertTrue(PrivacyGuardValidator.isPrivateOrInternal(testNet, listOf("203.0.113.0/24")))
    }

    @Test
    fun testFormatUtcTimestampBoundaries() {
        assertEquals("", formatUtcTimestamp(0L))
        assertEquals("1970-01-01 00:00 UTC", formatUtcTimestamp(1_000L))
        assertEquals("2023-11-14 22:13 UTC", formatUtcTimestamp(1_700_000_000_000L))
    }
}

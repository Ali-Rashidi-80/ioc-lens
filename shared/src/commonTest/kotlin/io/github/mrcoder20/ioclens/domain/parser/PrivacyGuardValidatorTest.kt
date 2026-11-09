package io.github.mrcoder20.ioclens.domain.parser

import io.github.mrcoder20.ioclens.domain.model.Indicator
import io.github.mrcoder20.ioclens.domain.model.IndicatorType
import io.github.mrcoder20.ioclens.domain.model.VerdictLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PrivacyGuardValidatorTest {

    @Test
    fun testRfc1918IPv4Ranges() {
        // 10.0.0.0/8
        assertTrue(PrivacyGuardValidator.isPrivateIp("10.0.0.1"))
        assertTrue(PrivacyGuardValidator.isPrivateIp("10.255.255.254"))
        assertTrue(PrivacyGuardValidator.isPrivateIp("10.10.10.10:8080"))

        // 172.16.0.0/12
        assertTrue(PrivacyGuardValidator.isPrivateIp("172.16.0.0"))
        assertTrue(PrivacyGuardValidator.isPrivateIp("172.16.0.1"))
        assertTrue(PrivacyGuardValidator.isPrivateIp("172.31.255.255"))
        assertFalse(PrivacyGuardValidator.isPrivateIp("172.15.255.255"))
        assertFalse(PrivacyGuardValidator.isPrivateIp("172.32.0.1"))

        // 192.168.0.0/16
        assertTrue(PrivacyGuardValidator.isPrivateIp("192.168.0.1"))
        assertTrue(PrivacyGuardValidator.isPrivateIp("192.168.1.1"))
        assertTrue(PrivacyGuardValidator.isPrivateIp("192.168.255.255"))
        assertFalse(PrivacyGuardValidator.isPrivateIp("192.169.0.1"))
    }

    @Test
    fun testLoopbackAndLinkLocal() {
        // 127.0.0.0/8 Loopback
        assertTrue(PrivacyGuardValidator.isPrivateIp("127.0.0.1"))
        assertTrue(PrivacyGuardValidator.isPrivateIp("127.255.255.255"))
        assertTrue(PrivacyGuardValidator.isPrivateIp("127.0.0.1:8080"))

        // 169.254.0.0/16 Link-Local
        assertTrue(PrivacyGuardValidator.isPrivateIp("169.254.0.1"))
        assertTrue(PrivacyGuardValidator.isPrivateIp("169.254.255.254"))
        assertFalse(PrivacyGuardValidator.isPrivateIp("169.253.255.255"))
        assertFalse(PrivacyGuardValidator.isPrivateIp("169.255.0.1"))
    }

    @Test
    fun testCgnatAndReserved() {
        // 100.64.0.0/10 Carrier-grade NAT
        assertTrue(PrivacyGuardValidator.isPrivateIp("100.64.0.1"))
        assertTrue(PrivacyGuardValidator.isPrivateIp("100.127.255.254"))
        assertFalse(PrivacyGuardValidator.isPrivateIp("100.63.255.255"))
        assertFalse(PrivacyGuardValidator.isPrivateIp("100.128.0.1"))

        // 0.0.0.0/8 Current network
        assertTrue(PrivacyGuardValidator.isPrivateIp("0.0.0.0"))
        assertTrue(PrivacyGuardValidator.isPrivateIp("0.255.255.255"))

        // 224.0.0.0/4 Multicast & 240.0.0.0/4 Reserved
        assertTrue(PrivacyGuardValidator.isPrivateIp("224.0.0.1"))
        assertTrue(PrivacyGuardValidator.isPrivateIp("239.255.255.255"))
        assertTrue(PrivacyGuardValidator.isPrivateIp("240.0.0.1"))
        assertTrue(PrivacyGuardValidator.isPrivateIp("255.255.255.255"))
    }

    @Test
    fun testPublicIpsReturnFalse() {
        assertFalse(PrivacyGuardValidator.isPrivateIp("8.8.8.8"))
        assertFalse(PrivacyGuardValidator.isPrivateIp("1.1.1.1"))
        assertFalse(PrivacyGuardValidator.isPrivateIp("93.184.216.34"))
        assertFalse(PrivacyGuardValidator.isPrivateIp("185.220.101.5"))
    }

    @Test
    fun testIPv6PrivateAndLoopback() {
        // Loopback & Unspecified
        assertTrue(PrivacyGuardValidator.isPrivateIp("::1"))
        assertTrue(PrivacyGuardValidator.isPrivateIp("::"))
        assertTrue(PrivacyGuardValidator.isPrivateIp("0:0:0:0:0:0:0:1"))
        assertTrue(PrivacyGuardValidator.isPrivateIp("0000:0000:0000:0000:0000:0000:0000:0001"))
        assertTrue(PrivacyGuardValidator.isPrivateIp("[::1]:8080"))

        // Link-Local (fe80::/10)
        assertTrue(PrivacyGuardValidator.isPrivateIp("fe80::1"))
        assertTrue(PrivacyGuardValidator.isPrivateIp("fe80::dead:beef"))

        // Unique-Local / ULA (fc00::/7)
        assertTrue(PrivacyGuardValidator.isPrivateIp("fc00::1"))
        assertTrue(PrivacyGuardValidator.isPrivateIp("fd12:3456:789a::1"))

        // IPv4-mapped IPv6
        assertTrue(PrivacyGuardValidator.isPrivateIp("::ffff:192.168.1.1"))
        assertTrue(PrivacyGuardValidator.isPrivateIp("::ffff:127.0.0.1"))
        assertTrue(PrivacyGuardValidator.isPrivateIp("::ffff:10.0.0.1"))
        assertTrue(PrivacyGuardValidator.isPrivateIp("[::ffff:10.0.0.1]:8080"))
        assertTrue(PrivacyGuardValidator.isPrivateIp("[::ffff:192.168.1.50]:443"))
        assertFalse(PrivacyGuardValidator.isPrivateIp("::ffff:8.8.8.8"))
        assertFalse(PrivacyGuardValidator.isPrivateIp("[::ffff:8.8.8.8]:8080"))
    }

    @Test
    fun testPublicIPv6ReturnsFalse() {
        assertFalse(PrivacyGuardValidator.isPrivateIp("2001:4860:4860::8888"))
        assertFalse(PrivacyGuardValidator.isPrivateIp("2606:4700:4700::1111"))
    }

    @Test
    fun testInternalDomainsAndTlds() {
        assertTrue(PrivacyGuardValidator.isInternalDomain("localhost"))
        assertTrue(PrivacyGuardValidator.isInternalDomain("test.localhost"))
        assertTrue(PrivacyGuardValidator.isInternalDomain("api.dev.localhost"))
        assertTrue(PrivacyGuardValidator.isInternalDomain("router.local"))
        assertTrue(PrivacyGuardValidator.isInternalDomain("service.internal"))
        assertTrue(PrivacyGuardValidator.isInternalDomain("nas.lan"))
        assertTrue(PrivacyGuardValidator.isInternalDomain("ad.home"))
        assertTrue(PrivacyGuardValidator.isInternalDomain("portal.corp"))
    }

    @Test
    fun testPublicDomainsReturnFalse() {
        assertFalse(PrivacyGuardValidator.isInternalDomain("google.com"))
        assertFalse(PrivacyGuardValidator.isInternalDomain("github.com"))
        assertFalse(PrivacyGuardValidator.isInternalDomain("abuse.ch"))
        // Domain bypass defense: domain starting with numbers must not be treated as private IP
        assertFalse(PrivacyGuardValidator.isInternalDomain("10.malware.com"))
        assertFalse(PrivacyGuardValidator.isInternalDomain("192.168.evil.ru"))
        assertFalse(PrivacyGuardValidator.isInternalDomain("127.badactor.org"))
    }

    @Test
    fun testInternalUrls() {
        assertTrue(PrivacyGuardValidator.isInternalUrl("http://192.168.1.1:8080/admin"))
        assertTrue(PrivacyGuardValidator.isInternalUrl("https://localhost:3000/api"))
        assertTrue(PrivacyGuardValidator.isInternalUrl("http://gateway.lan/setup"))
        assertTrue(PrivacyGuardValidator.isInternalUrl("hxxps[://][::1][:]8443/admin"))
        assertTrue(PrivacyGuardValidator.isInternalUrl("http://admin:secret@192.168.1.1/setup"))
        assertFalse(PrivacyGuardValidator.isInternalUrl("https://google.com/search"))
        assertFalse(PrivacyGuardValidator.isInternalUrl("http://8.8.8.8/dns"))
    }

    @Test
    fun testIsPrivateOrInternalWithRawInput() {
        assertTrue(PrivacyGuardValidator.isPrivateOrInternal("10.0.0.5"))
        assertTrue(PrivacyGuardValidator.isPrivateOrInternal("192.168.1.254"))
        assertTrue(PrivacyGuardValidator.isPrivateOrInternal("127.0.0.1:3000"))
        assertTrue(PrivacyGuardValidator.isPrivateOrInternal("localhost"))
        assertTrue(PrivacyGuardValidator.isPrivateOrInternal("http://10.0.0.1/"))
        assertFalse(PrivacyGuardValidator.isPrivateOrInternal("10.malware.com"))
        assertFalse(PrivacyGuardValidator.isPrivateOrInternal("8.8.8.8"))
        assertFalse(PrivacyGuardValidator.isPrivateOrInternal("https://example.com/"))
    }

    @Test
    fun testIsPrivateOrInternalWithIndicator() {
        val privateIp = Indicator("192.168.1.1", "192.168.1.1", IndicatorType.IP_ADDRESS)
        val publicIp = Indicator("8.8.8.8", "8.8.8.8", IndicatorType.IP_ADDRESS)
        val internalDomain = Indicator("nas.lan", "nas.lan", IndicatorType.DOMAIN)
        val publicDomain = Indicator("example.com", "example.com", IndicatorType.DOMAIN)
        val hash = Indicator("d41d8cd98f00b204e9800998ecf8427e", "d41d8cd98f00b204e9800998ecf8427e", IndicatorType.FILE_HASH)

        assertTrue(PrivacyGuardValidator.isPrivateOrInternal(privateIp))
        assertFalse(PrivacyGuardValidator.isPrivateOrInternal(publicIp))
        assertTrue(PrivacyGuardValidator.isPrivateOrInternal(internalDomain))
        assertFalse(PrivacyGuardValidator.isPrivateOrInternal(publicDomain))
        assertFalse(PrivacyGuardValidator.isPrivateOrInternal(hash))
    }

    @Test
    fun testCreatePrivacyGuardVerdictGuaranteesCleanVerdict() {
        val indicator = Indicator("192.168.1.1", "192.168.1.1", IndicatorType.IP_ADDRESS, isInternalOrPrivate = false)
        val verdict = PrivacyGuardValidator.createPrivacyGuardVerdict(indicator)

        assertEquals(VerdictLevel.CLEAN, verdict.overallVerdict)
        assertEquals(0, verdict.confidenceScore)
        assertTrue(verdict.indicator.isInternalOrPrivate)
        assertEquals(1, verdict.sourceResults.size)

        val result = verdict.sourceResults.first()
        assertEquals("Privacy Guard Local", result.providerName)
        assertEquals(VerdictLevel.CLEAN, result.verdict)
        assertEquals(0, result.detectionsCount)
        assertEquals(1, result.totalEngineCount)
        assertFalse(result.isRateLimited)
        assertTrue(result.summary.contains("safely blocked locally"))
    }
}

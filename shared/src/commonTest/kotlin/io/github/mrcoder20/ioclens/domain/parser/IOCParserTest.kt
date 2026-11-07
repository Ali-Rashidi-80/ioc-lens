package io.github.mrcoder20.ioclens.domain.parser

import io.github.mrcoder20.ioclens.domain.model.HashAlgorithm
import io.github.mrcoder20.ioclens.domain.model.ParsedIoc
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class IOCParserTest {

    private val parser = IOCParser()

    @Test
    fun testPublicIpv4() {
        val result = parser.parse("8.8.8.8")
        val ip = assertIs<ParsedIoc.IpAddress>(result)
        assertEquals("8.8.8.8", ip.normalizedValue)
        assertFalse(ip.isIpv6)
        assertFalse(ip.isPrivate)
    }

    @Test
    fun testPrivateIpv4Ranges() {
        val privateIps = listOf(
            "10.0.0.1",
            "10.255.255.254",
            "172.16.0.1",
            "172.31.255.255",
            "192.168.0.1",
            "192.168.100.254",
            "127.0.0.1",
            "169.254.1.1",
            "100.64.0.1",
            "100.127.255.255"
        )
        for (raw in privateIps) {
            val result = parser.parse(raw)
            val ip = assertIs<ParsedIoc.IpAddress>(result, "Expected $raw to be IpAddress")
            assertTrue(ip.isPrivate, "Expected $raw to be identified as private IP")
        }

        // Boundary: 172.32.0.1 is public!
        val public172 = parser.parse("172.32.0.1")
        val ip172 = assertIs<ParsedIoc.IpAddress>(public172)
        assertFalse(ip172.isPrivate)

        // Boundary: 100.128.0.1 is public!
        val public100 = parser.parse("100.128.0.1")
        val ip100 = assertIs<ParsedIoc.IpAddress>(public100)
        assertFalse(ip100.isPrivate)
    }

    @Test
    fun testInvalidIpv4Octets() {
        // Octets > 255 should not be parsed as valid IP
        val invalid = parser.parse("999.999.999.999")
        assertFalse(invalid is ParsedIoc.IpAddress)

        val invalid2 = parser.parse("256.1.1.1")
        assertFalse(invalid2 is ParsedIoc.IpAddress)
    }

    @Test
    fun testDomainBypassBugFix() {
        // Critical: In legacy code, "10.malware.com" matched startsWith("10.") and was marked internal!
        val result = parser.parse("10.malware.com")
        val domain = assertIs<ParsedIoc.Domain>(result)
        assertEquals("10.malware.com", domain.normalizedValue)
        assertFalse(domain.isInternal, "10.malware.com must NOT be marked internal!")

        val result2 = parser.parse("192.168.evil.ru")
        val domain2 = assertIs<ParsedIoc.Domain>(result2)
        assertFalse(domain2.isInternal, "192.168.evil.ru must NOT be marked internal!")

        val result3 = parser.parse("127.badactor.org")
        val domain3 = assertIs<ParsedIoc.Domain>(result3)
        assertFalse(domain3.isInternal, "127.badactor.org must NOT be marked internal!")
    }

    @Test
    fun testInternalDomains() {
        val internalDomains = listOf(
            "intranet.local",
            "db-master.internal",
            "router.lan",
            "nas.home",
            "localhost"
        )
        for (raw in internalDomains) {
            val result = parser.parse(raw)
            val domain = assertIs<ParsedIoc.Domain>(result, "Expected $raw to be Domain")
            assertTrue(domain.isInternal, "Expected $raw to be internal domain")
        }
    }

    @Test
    fun testDefanging() {
        // URL defanging
        val url = parser.parse("hxxp://evil[.]com/path")
        val parsedUrl = assertIs<ParsedIoc.Url>(url)
        assertEquals("http://evil.com/path", parsedUrl.normalizedValue)
        assertEquals("evil.com", parsedUrl.host)

        val urlHttps = parser.parse("hxxps://malicious(.)net[:]8080/c2")
        val parsedHttps = assertIs<ParsedIoc.Url>(urlHttps)
        assertEquals("https://malicious.net:8080/c2", parsedHttps.normalizedValue)

        // Protocol defanging variants (colon inside brackets)
        val urlBracketColon = parser.parse("hxxps[:]//evil.com/payload")
        val parsedBracketColon = assertIs<ParsedIoc.Url>(urlBracketColon)
        assertEquals("https://evil.com/payload", parsedBracketColon.normalizedValue)

        val urlFullBracket = parser.parse("hxxps[://]threat-domain.net/worm")
        val parsedFullBracket = assertIs<ParsedIoc.Url>(urlFullBracket)
        assertEquals("https://threat-domain.net/worm", parsedFullBracket.normalizedValue)

        // IP defanging
        val ip = parser.parse("1[.]1[.]1[.]1")
        val parsedIp = assertIs<ParsedIoc.IpAddress>(ip)
        assertEquals("1.1.1.1", parsedIp.normalizedValue)
    }

    @Test
    fun testFileExtensionsNotMisclassifiedAsDomains() {
        val filenames = listOf(
            "malware.apk",
            "trojan.exe",
            "ransomware.bin",
            "invoice.pdf",
            "setup.msi",
            "payload.dll",
            "archive.zip",
            "doc.docx"
        )
        for (file in filenames) {
            val result = parser.parse(file)
            assertFalse(result is ParsedIoc.Domain, "Filename '$file' must not be classified as a Domain!")
        }
    }

    @Test
    fun testIpv4WithPort() {
        val socket = parser.parse("185.220.101.5:9001")
        val ip = assertIs<ParsedIoc.IpAddress>(socket)
        assertEquals("185.220.101.5", ip.normalizedValue)
        assertEquals("185.220.101.5:9001", ip.rawValue)
        assertFalse(ip.isPrivate)

        val internalSocket = parser.parse("192.168.1.10:8080")
        val internalIp = assertIs<ParsedIoc.IpAddress>(internalSocket)
        assertEquals("192.168.1.10", internalIp.normalizedValue)
        assertTrue(internalIp.isPrivate)
    }

    @Test
    fun testBracketedIpv6() {
        val bracketed = parser.parse("[2001:4860:4860::8888]")
        val parsed = assertIs<ParsedIoc.IpAddress>(bracketed)
        assertTrue(parsed.isIpv6)
        assertEquals("2001:4860:4860::8888", parsed.normalizedValue)
        assertFalse(parsed.isPrivate)

        val loopback = parser.parse("[::1]")
        val parsedLoopback = assertIs<ParsedIoc.IpAddress>(loopback)
        assertTrue(parsedLoopback.isIpv6)
        assertTrue(parsedLoopback.isPrivate)
    }

    @Test
    fun testCryptographicHashes() {
        // MD5 (32 hex)
        val md5 = parser.parse("44d88612fea8a8f36de82e1278abb02f")
        val parsedMd5 = assertIs<ParsedIoc.Hash>(md5)
        assertEquals(HashAlgorithm.MD5, parsedMd5.algorithm)
        assertEquals("44d88612fea8a8f36de82e1278abb02f", parsedMd5.normalizedValue)

        // SHA1 (40 hex)
        val sha1 = parser.parse("3395856CE81F2B7382DEE72602F798B642F14140")
        val parsedSha1 = assertIs<ParsedIoc.Hash>(sha1)
        assertEquals(HashAlgorithm.SHA1, parsedSha1.algorithm)
        assertEquals("3395856ce81f2b7382dee72602f798b642f14140", parsedSha1.normalizedValue)

        // SHA256 (64 hex)
        val sha256 = parser.parse("275a021bbfb6489e54d471899f7db9d1663fc695ec2fe2a2c4538aabf651fd0f")
        val parsedSha256 = assertIs<ParsedIoc.Hash>(sha256)
        assertEquals(HashAlgorithm.SHA256, parsedSha256.algorithm)
    }

    @Test
    fun testIpv6() {
        val pubIpv6 = parser.parse("2001:4860:4860::8888")
        val parsedPub = assertIs<ParsedIoc.IpAddress>(pubIpv6)
        assertTrue(parsedPub.isIpv6)
        assertFalse(parsedPub.isPrivate)

        val loopback = parser.parse("::1")
        val parsedLoopback = assertIs<ParsedIoc.IpAddress>(loopback)
        assertTrue(parsedLoopback.isIpv6)
        assertTrue(parsedLoopback.isPrivate)

        val linkLocal = parser.parse("fe80::1ff:fe23:4567:890a")
        val parsedLinkLocal = assertIs<ParsedIoc.IpAddress>(linkLocal)
        assertTrue(parsedLinkLocal.isIpv6)
        assertTrue(parsedLinkLocal.isPrivate)

        val ula = parser.parse("fc00::1")
        val parsedUla = assertIs<ParsedIoc.IpAddress>(ula)
        assertTrue(parsedUla.isIpv6)
        assertTrue(parsedUla.isPrivate)
    }

    @Test
    fun testUnknownInput() {
        val unknown = parser.parse("this is a random sentence from user")
        assertIs<ParsedIoc.Unknown>(unknown)
    }
}

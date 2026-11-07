package io.github.mrcoder20.ioclens.domain.parser

import io.github.mrcoder20.ioclens.domain.model.HashAlgorithm
import io.github.mrcoder20.ioclens.domain.model.ParsedIoc
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class IOCParserAdversarialTest {

    private val parser = IOCParser()

    @Test
    fun testXssInjectionStringsSafelyClassifiedAsUnknown() {
        val xssPayloads = listOf(
            "<script>alert(1)</script>",
            "<img src=x onerror=alert(1)>",
            "<svg/onload=alert('XSS')>",
            "javascript:alert(document.cookie)",
            "\"><script>document.location='http://evil.com'</script>",
            "https://<script>alert(1)</script>",
            "http://\" onclick=\"alert(1)"
        )
        for (payload in xssPayloads) {
            val result = parser.parse(payload)
            assertIs<ParsedIoc.Unknown>(result, "Expected XSS payload '$payload' to safely classify as Unknown")
        }
    }

    @Test
    fun testSqlInjectionStringsSafelyClassifiedAsUnknown() {
        val sqliPayloads = listOf(
            "' OR '1'='1",
            "admin' --",
            "1; DROP TABLE users; --",
            "' UNION SELECT NULL, NULL, NULL --",
            "1' or 1=1; --",
            "benchmark(10000000,MD5(1))"
        )
        for (payload in sqliPayloads) {
            val result = parser.parse(payload)
            assertIs<ParsedIoc.Unknown>(result, "Expected SQLi payload '$payload' to safely classify as Unknown")
        }
    }

    @Test
    fun testDirectoryTraversalStringsSafelyClassifiedAsUnknown() {
        val traversalPayloads = listOf(
            "../../etc/shadow",
            "../../../etc/passwd",
            "..\\..\\windows\\system32\\cmd.exe",
            "/etc/passwd",
            "C:\\Windows\\System32\\drivers\\etc\\hosts",
            "..\\..\\boot.ini"
        )
        for (payload in traversalPayloads) {
            val result = parser.parse(payload)
            assertIs<ParsedIoc.Unknown>(result, "Expected traversal payload '$payload' to safely classify as Unknown")
        }
    }

    @Test
    fun testUnicodeHomographAttackRejectedFromAsciiDomain() {
        // Cyrillic 'о' (U+043E) and 'е' (U+0435) spoofing google.com
        val cyrillicGoogle = "g\u043E\u043Egl\u0435.com"
        val result = parser.parse(cyrillicGoogle)
        assertIs<ParsedIoc.Unknown>(result, "Cyrillic homograph domain must NOT be classified as valid ASCII domain")

        // Cyrillic paypal: 'а' (U+0430), 'у' (U+0443), 'р' (U+0440)
        val cyrillicPaypal = "\u0440\u0430\u0443\u0440\u0430l.com"
        val resultPaypal = parser.parse(cyrillicPaypal)
        assertIs<ParsedIoc.Unknown>(resultPaypal, "Cyrillic homograph Paypal must NOT be classified as valid ASCII domain")
    }

    @Test
    fun testInvisibleZeroWidthCharactersSanitization() {
        // Zero-width space inside IP: 8.8.\u200B8.8 -> should sanitize and parse as 8.8.8.8
        val zeroWidthIp = "8.8.\u200B8.8"
        val parsedIp = assertIs<ParsedIoc.IpAddress>(parser.parse(zeroWidthIp))
        assertEquals("8.8.8.8", parsedIp.normalizedValue)

        // BOM (\uFEFF) prefixed hash -> should sanitize and parse as MD5
        val bomHash = "\uFEFF44d88612fea8a8f36de82e1278abb02f"
        val parsedHash = assertIs<ParsedIoc.Hash>(parser.parse(bomHash))
        assertEquals(HashAlgorithm.MD5, parsedHash.algorithm)
        assertEquals("44d88612fea8a8f36de82e1278abb02f", parsedHash.normalizedValue)

        // Non-breaking space (\u00A0) padded URL -> sanitized to clean URL
        val nbspUrl = "hxxps://evil[.]com/payload\u00A0"
        val parsedUrl = assertIs<ParsedIoc.Url>(parser.parse(nbspUrl))
        assertEquals("https://evil.com/payload", parsedUrl.normalizedValue)
    }

    @Test
    fun testMixedCasingAndPaddedWhitespaces() {
        // Mixed-casing URL
        val mixedUrl = "  \t\r\n HxTpS://ExAmPlE.CoM/PaTh?q=1#HaSh \n  "
        val parsedUrl = assertIs<ParsedIoc.Url>(parser.parse(mixedUrl))
        assertEquals("https://ExAmPlE.CoM/PaTh?q=1#HaSh", parsedUrl.normalizedValue)
        assertEquals("example.com", parsedUrl.host)

        // Mixed-casing SHA256
        val mixedSha = "  \n E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855 \t\r\n"
        val parsedSha = assertIs<ParsedIoc.Hash>(parser.parse(mixedSha))
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", parsedSha.normalizedValue)
        assertEquals(HashAlgorithm.SHA256, parsedSha.algorithm)

        // Padded Private IPv4
        val paddedIp = " \r\n\t 192.168.1.1:8080 \n "
        val parsedIp = assertIs<ParsedIoc.IpAddress>(parser.parse(paddedIp))
        assertEquals("192.168.1.1", parsedIp.normalizedValue)
        assertTrue(parsedIp.isPrivate)
    }

    @Test
    fun testTruncatedHashesSafelyClassifiedAsUnknown() {
        // 31 hex chars (truncated MD5)
        val truncMd5 = "44d88612fea8a8f36de82e1278abb02"
        assertIs<ParsedIoc.Unknown>(parser.parse(truncMd5))

        // 39 hex chars (truncated SHA1)
        val truncSha1 = "3395856ce81f2b7382dee72602f798b642f1414"
        assertIs<ParsedIoc.Unknown>(parser.parse(truncSha1))

        // 63 hex chars (truncated SHA256)
        val truncSha256 = "275a021bbfb6489e54d471899f7db9d1663fc695ec2fe2a2c4538aabf651fd0"
        assertIs<ParsedIoc.Unknown>(parser.parse(truncSha256))

        // 65 hex chars (overlong hash)
        val overlongSha = "275a021bbfb6489e54d471899f7db9d1663fc695ec2fe2a2c4538aabf651fd0faa"
        assertIs<ParsedIoc.Unknown>(parser.parse(overlongSha))
    }

    @Test
    fun testExtremelyLongUrlsAndInputLimits() {
        // Valid 4096-char URL (ReDoS resistance test)
        val longPath = "a".repeat(4050)
        val longUrl = "https://example.com/$longPath"
        val parsedLong = assertIs<ParsedIoc.Url>(parser.parse(longUrl))
        assertEquals("example.com", parsedLong.host)

        // Exceedingly long string > 8192 chars -> rejected safely without StackOverflow/ReDoS
        val hugeUrl = "https://example.com/" + "a".repeat(9000)
        assertIs<ParsedIoc.Unknown>(parser.parse(hugeUrl))

        // Exceedingly long non-URL string > 512 chars
        val hugeRandom = "random_gibberish_".repeat(50)
        assertIs<ParsedIoc.Unknown>(parser.parse(hugeRandom))
    }

    @Test
    fun testMalformedUrlsWithoutHost() {
        val badUrls = listOf(
            "http://",
            "https://",
            "http://:8080",
            "https://?param=value",
            "https://#target",
            "http:///path/to/resource"
        )
        for (bad in badUrls) {
            val result = parser.parse(bad)
            assertIs<ParsedIoc.Unknown>(result, "Expected malformed URL '$bad' to be classified as Unknown")
        }
    }

    @Test
    fun testHostileIpv6MalformedSyntax() {
        val invalidIpv6 = listOf(
            "1::2::3", // Multiple ::
            ":::1",    // Triple colon
            "a:b:c",   // 3 groups, no ::
            "2001:xyz::1", // Non-hex characters
            "fe80::1ff:fe23:4567:890a:bcde:f012:3456", // 9 groups
            "1:2:3:4:5:6:7" // 7 groups without ::
        )
        for (invalid in invalidIpv6) {
            val result = parser.parse(invalid)
            assertIs<ParsedIoc.Unknown>(result, "Expected invalid IPv6 '$invalid' to be classified as Unknown")
        }
    }

    @Test
    fun testHostileIpv4MalformedSyntax() {
        val invalidIpv4 = listOf(
            "256.1.1.1",
            "1.256.1.1",
            "1.1.256.1",
            "1.1.1.256",
            "1.1.1.1:70000", // Port > 65535
            "1.1.1.1:0",     // Port 0 invalid
            "1.1.1.1:abc",   // Non-numeric port
            "1.2.3.4.5",     // 5 octets
            "1.2.3"          // 3 octets
        )
        for (invalid in invalidIpv4) {
            val result = parser.parse(invalid)
            assertFalse(result is ParsedIoc.IpAddress, "Expected invalid IPv4 '$invalid' NOT to be IpAddress")
        }
    }

    @Test
    fun testUrlsWithBracketedIpv6HostExtraction() {
        // IPv6 URL with port
        val urlWithPort = parser.parse("http://[::1]:8080/path/to/resource")
        val parsed1 = assertIs<ParsedIoc.Url>(urlWithPort)
        assertEquals("::1", parsed1.host)
        assertTrue(parsed1.isInternal, "Loopback IPv6 URL must be marked internal")

        // IPv6 URL without port
        val urlWithoutPort = parser.parse("https://[2001:db8::1]/index.html")
        val parsed2 = assertIs<ParsedIoc.Url>(urlWithoutPort)
        assertEquals("2001:db8::1", parsed2.host)
        assertFalse(parsed2.isInternal)

        // Defanged IPv6 URL
        val defanged = parser.parse("hxxps[://][::1][:]8443/admin")
        val parsed3 = assertIs<ParsedIoc.Url>(defanged)
        assertEquals("::1", parsed3.host)
        assertTrue(parsed3.isInternal)
    }

    @Test
    fun testBracketedIpv6WithPortParsing() {
        val bracketedLoopback = parser.parse("[::1]:8080")
        val parsed1 = assertIs<ParsedIoc.IpAddress>(bracketedLoopback)
        assertEquals("::1", parsed1.normalizedValue)
        assertTrue(parsed1.isIpv6)
        assertTrue(parsed1.isPrivate)

        val bracketedPublic = parser.parse("[2001:db8::1]:443")
        val parsed2 = assertIs<ParsedIoc.IpAddress>(bracketedPublic)
        assertEquals("2001:db8::1", parsed2.normalizedValue)
        assertTrue(parsed2.isIpv6)
        assertFalse(parsed2.isPrivate)

        // Invalid port > 65535
        val invalidPort = parser.parse("[::1]:70000")
        assertIs<ParsedIoc.Unknown>(invalidPort)
    }

    @Test
    fun testUncompressedAndIpv4MappedLoopbackIpv6() {
        // Full uncompressed 8-group loopback
        val uncompressed = parser.parse("0:0:0:0:0:0:0:1")
        val parsed1 = assertIs<ParsedIoc.IpAddress>(uncompressed)
        assertTrue(parsed1.isPrivate, "0:0:0:0:0:0:0:1 must be marked as private loopback")

        // Leading zeroes padded
        val padded = parser.parse("0000:0000:0000:0000:0000:0000:0000:0001")
        val parsed2 = assertIs<ParsedIoc.IpAddress>(padded)
        assertTrue(parsed2.isPrivate, "0000:...:0001 must be marked as private loopback")

        // IPv4-mapped loopback ::ffff:127.0.0.1
        val mappedLoopback = parser.parse("::ffff:127.0.0.1")
        val parsed3 = assertIs<ParsedIoc.IpAddress>(mappedLoopback)
        assertTrue(parsed3.isPrivate, "::ffff:127.0.0.1 must be identified as private IPv4-mapped IPv6")
    }

    @Test
    fun testLocalhostSubdomainsMarkedAsInternal() {
        val subLocalhost = parser.parse("api.localhost")
        val parsed = assertIs<ParsedIoc.Domain>(subLocalhost)
        assertTrue(parsed.isInternal, "api.localhost should be identified as internal domain")

        val deepSubLocalhost = parser.parse("dev.service.localhost")
        val parsedDeep = assertIs<ParsedIoc.Domain>(deepSubLocalhost)
        assertTrue(parsedDeep.isInternal, "dev.service.localhost should be identified as internal domain")
    }

    @Test
    fun testFtpUrlsProperlyClassified() {
        val ftpUrl = parser.parse("ftp://evil.com/payload.exe")
        val parsed = assertIs<ParsedIoc.Url>(ftpUrl)
        assertEquals("evil.com", parsed.host)
        assertEquals("ftp://evil.com/payload.exe", parsed.normalizedValue)

        // Refanged fxp://
        val fxpUrl = parser.parse("fxp://threat.net/sample")
        val parsedFxp = assertIs<ParsedIoc.Url>(fxpUrl)
        assertEquals("threat.net", parsedFxp.host)
        assertEquals("ftp://threat.net/sample", parsedFxp.normalizedValue)
    }
}

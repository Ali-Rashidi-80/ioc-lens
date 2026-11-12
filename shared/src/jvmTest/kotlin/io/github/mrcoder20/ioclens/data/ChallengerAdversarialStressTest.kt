package io.github.mrcoder20.ioclens.data

import io.github.mrcoder20.ioclens.data.remote.ThreatIntelAggregator
import io.github.mrcoder20.ioclens.data.repository.ZeroKeyThreatIntelRepositoryImpl
import io.github.mrcoder20.ioclens.domain.model.Indicator
import io.github.mrcoder20.ioclens.domain.model.IndicatorType
import io.github.mrcoder20.ioclens.domain.model.ParsedIoc
import io.github.mrcoder20.ioclens.domain.model.VerdictLevel
import io.github.mrcoder20.ioclens.domain.parser.IOCParser
import io.github.mrcoder20.ioclens.domain.parser.PrivacyGuardValidator
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ChallengerAdversarialStressTest {

    private val parser = IOCParser()

    /**
     * Objective 1: Bracketed scoped IPv6 addresses with ports.
     * Input vectors: [fe80::1%eth0]:8080, [fe80::1%1]:8080, [::1]:8080, [::ffff:10.0.0.1]:8080
     * Ensures all are parsed, recognized as private, and intercepted with 0 network calls!
     */
    @Test
    fun testBracketedScopedIpv6WithPortsZeroNetworkInterception() = runTest {
        val vectors = listOf(
            "[fe80::1%eth0]:8080",
            "[fe80::1%1]:8080",
            "[::1]:8080",
            "[::ffff:10.0.0.1]:8080"
        )

        var networkCalls = 0
        val throwingEngine = MockEngine { request ->
            networkCalls++
            throw AssertionError("SECURITY VIOLATION: Network call leaked to ${request.url}")
        }
        val client = HttpClient(throwingEngine)
        val repository = ZeroKeyThreatIntelRepositoryImpl(client = client)

        for (v in vectors) {
            // 1. PrivacyGuardValidator directly
            assertTrue(PrivacyGuardValidator.isPrivateIp(v), "PrivacyGuardValidator.isPrivateIp must be true for $v")
            assertTrue(PrivacyGuardValidator.isPrivateOrInternal(v), "PrivacyGuardValidator.isPrivateOrInternal must be true for $v")

            // 2. Parser verification
            val parsed = parser.parseExact(v)
            assertIs<ParsedIoc.IpAddress>(parsed, "Expected ParsedIoc.IpAddress for $v")
            assertTrue(parsed.isIpv6, "Expected isIpv6=true for $v")
            assertTrue(parsed.isPrivate, "Expected isPrivate=true for $v")

            // 3. Indicator with isInternalOrPrivate = false (sneaky / un-sanitized)
            val sneakyIndicator = Indicator(
                rawValue = v,
                normalizedValue = parsed.normalizedValue,
                type = IndicatorType.IP_ADDRESS,
                isInternalOrPrivate = false
            )
            val verdict = repository.analyze(sneakyIndicator)
            assertEquals(0, networkCalls, "Network call leaked for $v")
            assertEquals(VerdictLevel.CLEAN, verdict.overallVerdict)
            assertEquals(0, verdict.confidenceScore)
            assertTrue(verdict.indicator.isInternalOrPrivate)
            assertEquals("Privacy Guard Local", verdict.sourceResults.first().providerName)

            // 4. Indicator from parser
            val parsedIndicator = parsed.toIndicator()
            val parsedVerdict = repository.analyze(parsedIndicator)
            assertEquals(0, networkCalls, "Network call leaked for parsed indicator $v")
            assertEquals(VerdictLevel.CLEAN, parsedVerdict.overallVerdict)
            assertEquals(0, parsedVerdict.confidenceScore)
            assertTrue(parsedVerdict.indicator.isInternalOrPrivate)
        }
    }

    /**
     * Objective 2: Uncompressed & hex-mapped IPv4-mapped IPv6.
     * Inputs: 0:0:0:0:0:ffff:10.0.0.1, ::ffff:0a00:0001, 0:0:0:0:0:ffff:0a00:0001
     * Contrast with public mapped: ::ffff:0808:0808 / ::ffff:8.8.8.8 (must NOT be marked private).
     */
    @Test
    fun testUncompressedAndHexMappedIpv4MappedIpv6() = runTest {
        val privateVectors = listOf(
            "0:0:0:0:0:ffff:10.0.0.1",
            "::ffff:0a00:0001",
            "0:0:0:0:0:ffff:0a00:0001"
        )

        var networkCalls = 0
        val throwingEngine = MockEngine { request ->
            networkCalls++
            throw AssertionError("SECURITY VIOLATION: Network call leaked to ${request.url}")
        }
        val client = HttpClient(throwingEngine)
        val repository = ZeroKeyThreatIntelRepositoryImpl(client = client)

        for (v in privateVectors) {
            assertTrue(PrivacyGuardValidator.isPrivateIp(v), "Expected isPrivateIp=true for $v")
            assertTrue(PrivacyGuardValidator.isPrivateOrInternal(v), "Expected isPrivateOrInternal=true for $v")

            val parsed = parser.parseExact(v)
            assertIs<ParsedIoc.IpAddress>(parsed, "Expected ParsedIoc.IpAddress for $v")
            assertTrue(parsed.isIpv6, "Expected isIpv6=true for $v")
            assertTrue(parsed.isPrivate, "Expected isPrivate=true for $v")

            // Zero network calls verification
            val sneaky = Indicator(v, parsed.normalizedValue, IndicatorType.IP_ADDRESS, isInternalOrPrivate = false)
            val verdict = repository.analyze(sneaky)
            assertEquals(0, networkCalls, "Network call leaked for private mapped IPv6 $v")
            assertEquals(VerdictLevel.CLEAN, verdict.overallVerdict)
            assertEquals(0, verdict.confidenceScore)
            assertTrue(verdict.indicator.isInternalOrPrivate)
        }

        val publicVectors = listOf(
            "::ffff:0808:0808",
            "::ffff:8.8.8.8",
            "0:0:0:0:0:ffff:8.8.8.8",
            "::ffff:0101:0101"
        )

        for (v in publicVectors) {
            assertFalse(PrivacyGuardValidator.isPrivateIp(v), "Public mapped IPv6 $v must NOT be private")
            assertFalse(PrivacyGuardValidator.isPrivateOrInternal(v), "Public mapped IPv6 $v must NOT be privateOrInternal")

            val parsed = parser.parseExact(v)
            assertIs<ParsedIoc.IpAddress>(parsed, "Expected ParsedIoc.IpAddress for $v")
            assertTrue(parsed.isIpv6, "Expected isIpv6=true for $v")
            assertFalse(parsed.isPrivate, "Expected isPrivate=false for public mapped IPv6 $v")

            // Negative harness proof: public mapped IPv6 MUST trigger network calls
            val indicator = parsed.toIndicator()
            try {
                repository.analyze(indicator)
            } catch (e: AssertionError) {
                // Expected to attempt network calls
            }
            assertTrue(networkCalls > 0, "Public mapped IPv6 $v must dispatch network calls")
            networkCalls = 0 // reset for next iteration
        }
    }

    /**
     * Objective 3: Defanged variants via PrivacyGuardValidator.isPrivateOrInternal(rawInput).
     * Inputs: 10[dot]0[dot]0[dot]1, 192{.}168{.}1{.}1, 172(.)16(.)0(.)1, hxxp://10.0.0.1:8080/admin
     */
    @Test
    fun testDefangedVariantsViaIsPrivateOrInternal() = runTest {
        val defangedPrivateInputs = listOf(
            "10[dot]0[dot]0[dot]1",
            "192{.}168{.}1{.}1",
            "172(.)16(.)0(.)1",
            "hxxp://10.0.0.1:8080/admin",
            "10(dot)0(dot)0(dot)1",
            "10{dot}0{dot}0{dot}1",
            "hxxps://192{.}168{.}1{.}1:8443/login",
            "h[tt]ps://10[dot]0[dot]0[dot]1:443/api"
        )

        var networkCalls = 0
        val throwingEngine = MockEngine { request ->
            networkCalls++
            throw AssertionError("SECURITY VIOLATION: Network call leaked to ${request.url}")
        }
        val client = HttpClient(throwingEngine)
        val repository = ZeroKeyThreatIntelRepositoryImpl(client = client)

        for (raw in defangedPrivateInputs) {
            assertTrue(
                PrivacyGuardValidator.isPrivateOrInternal(raw),
                "isPrivateOrInternal must return true for defanged input $raw"
            )

            // Test parsing and zero-network interception
            val parsed = parser.parse(raw)
            val indicator = parsed.toIndicator()
            assertTrue(indicator.isInternalOrPrivate, "Indicator for $raw must have isInternalOrPrivate=true")

            val verdict = repository.analyze(indicator)
            assertEquals(0, networkCalls, "Zero network calls expected for defanged input $raw")
            assertEquals(VerdictLevel.CLEAN, verdict.overallVerdict)
            assertEquals(0, verdict.confidenceScore)
            assertTrue(verdict.indicator.isInternalOrPrivate)
        }
    }

    /**
     * Objective 4: Scheme-less internal URLs.
     * Inputs: 192.168.1.1/admin, [::1]:8080/path, localhost/api
     */
    @Test
    fun testSchemeLessInternalUrls() = runTest {
        val schemeLessInternalUrls = listOf(
            "192.168.1.1/admin",
            "[::1]:8080/path",
            "localhost/api",
            "10.0.0.1:8080/dashboard",
            "[fe80::1%eth0]:8080/config",
            "[::ffff:10.0.0.1]:8080/api",
            "admin:secret@192.168.1.1:8080/setup",
            "user:pass@[::1]:8080/secure",
            "internal.corp/intranet"
        )

        var networkCalls = 0
        val throwingEngine = MockEngine { request ->
            networkCalls++
            throw AssertionError("SECURITY VIOLATION: Network call leaked to ${request.url}")
        }
        val client = HttpClient(throwingEngine)
        val repository = ZeroKeyThreatIntelRepositoryImpl(client = client)

        for (url in schemeLessInternalUrls) {
            assertTrue(
                PrivacyGuardValidator.isPrivateOrInternal(url),
                "PrivacyGuardValidator.isPrivateOrInternal must return true for $url"
            )

            val parsed = parser.parseExact(url)
            assertIs<ParsedIoc.Url>(parsed, "Expected ParsedIoc.Url for scheme-less url $url")
            assertTrue(parsed.isInternal, "Expected isInternal=true for $url")

            val indicator = parsed.toIndicator()
            assertTrue(indicator.isInternalOrPrivate, "Indicator must be internal for $url")

            // Zero network dispatch
            val verdict = repository.analyze(indicator)
            assertEquals(0, networkCalls, "Network call leaked for scheme-less url $url")
            assertEquals(VerdictLevel.CLEAN, verdict.overallVerdict)
            assertEquals(0, verdict.confidenceScore)
            assertTrue(verdict.indicator.isInternalOrPrivate)
        }

        // Contrast with public scheme-less URLs
        val publicSchemeLessUrls = listOf(
            "google.com/search?q=test",
            "8.8.8.8:53/dns-query",
            "[2001:4860:4860::8888]:443/test"
        )
        for (pub in publicSchemeLessUrls) {
            assertFalse(
                PrivacyGuardValidator.isPrivateOrInternal(pub),
                "Public scheme-less url $pub must NOT be marked privateOrInternal"
            )
            val parsed = parser.parseExact(pub)
            assertIs<ParsedIoc.Url>(parsed, "Expected ParsedIoc.Url for $pub")
            assertFalse(parsed.isInternal, "Public url $pub must have isInternal=false")
        }
    }

    /**
     * Additional Adversarial Test: Freeform noise wrapping internal indicators.
     */
    @Test
    fun testFreeformNoiseWrappingInternalIndicators() {
        val freeformSamples = listOf(
            "Firewall drop from [fe80::1%eth0]:8080 to destination" to true,
            "C2 server observed at 0:0:0:0:0:ffff:10.0.0.1 in logs" to true,
            "Suspicious connection to ::ffff:0a00:0001 confirmed" to true,
            "Check proxy at 192[dot]168[dot]1[dot]1" to true,
            "Admin console at 192.168.1.1/admin was accessed" to true
        )

        for ((text, expectedPrivate) in freeformSamples) {
            val extracted = parser.extractFirstIoc(text)
            assertTrue(extracted != null, "IOC should be extracted from '$text'")
            val ind = extracted.toIndicator()
            assertEquals(expectedPrivate, ind.isInternalOrPrivate, "Extracted IOC from '$text' private flag mismatch")
        }
    }
}

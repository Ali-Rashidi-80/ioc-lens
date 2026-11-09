package io.github.mrcoder20.ioclens.data

import io.github.mrcoder20.ioclens.data.remote.ThreatIntelAggregator
import io.github.mrcoder20.ioclens.data.repository.ZeroKeyThreatIntelRepositoryImpl
import io.github.mrcoder20.ioclens.domain.model.Indicator
import io.github.mrcoder20.ioclens.domain.model.IndicatorType
import io.github.mrcoder20.ioclens.domain.model.VerdictLevel
import io.github.mrcoder20.ioclens.domain.parser.IOCParser
import io.github.mrcoder20.ioclens.domain.parser.PrivacyGuardValidator
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdversarialPrivacyGuardStressTest {

    private val parser = IOCParser()

    /**
     * Requirement 1: CIDR boundary stress-testing.
     * Evaluates exact boundaries, inside, and just outside for all RFC/reserved IPv4 blocks.
     */
    @Test
    fun testCidrBoundaryAdversarialMatrix() {
        val testCases = listOf(
            // 10.0.0.0/8 (RFC 1918)
            "10.0.0.0" to true,
            "10.0.0.1" to true,
            "10.255.255.255" to true,
            "9.255.255.255" to false,
            "11.0.0.0" to false,

            // 172.16.0.0/12 (RFC 1918)
            "172.16.0.0" to true,
            "172.16.0.1" to true,
            "172.31.255.255" to true,
            "172.15.255.255" to false,
            "172.32.0.0" to false,
            "172.32.0.1" to false,

            // 192.168.0.0/16 (RFC 1918)
            "192.168.0.0" to true,
            "192.168.0.1" to true,
            "192.168.255.255" to true,
            "192.167.255.255" to false,
            "192.169.0.0" to false,
            "192.169.0.1" to false,

            // 127.0.0.0/8 (Loopback RFC 1122)
            "127.0.0.0" to true,
            "127.0.0.1" to true,
            "127.255.255.255" to true,
            "126.255.255.255" to false,
            "128.0.0.0" to false,

            // 169.254.0.0/16 (Link-Local RFC 3927)
            "169.254.0.0" to true,
            "169.254.0.1" to true,
            "169.254.255.255" to true,
            "169.253.255.255" to false,
            "169.255.0.0" to false,

            // 100.64.0.0/10 (CGNAT RFC 6598)
            "100.64.0.0" to true,
            "100.64.0.1" to true,
            "100.127.255.255" to true,
            "100.63.255.255" to false,
            "100.128.0.0" to false,

            // 0.0.0.0/8 (Current network)
            "0.0.0.0" to true,
            "0.255.255.255" to true,

            // 224.0.0.0/4 Multicast & 240.0.0.0/4 Reserved
            "224.0.0.0" to true,
            "224.0.0.1" to true,
            "239.255.255.255" to true,
            "240.0.0.0" to true,
            "240.0.0.1" to true,
            "255.255.255.255" to true,
            "223.255.255.255" to false
        )

        for ((ip, expectedPrivate) in testCases) {
            val actual = PrivacyGuardValidator.isPrivateIp(ip)
            assertEquals(
                expectedPrivate,
                actual,
                "CIDR boundary failure for IP '$ip': expected isPrivate=$expectedPrivate, but was $actual"
            )
            val rawCheck = PrivacyGuardValidator.isPrivateOrInternal(ip)
            assertEquals(
                expectedPrivate,
                rawCheck,
                "isPrivateOrInternal raw failure for IP '$ip': expected isPrivate=$expectedPrivate, but was $rawCheck"
            )
        }
    }

    /**
     * Requirement 2: Obfuscated and defanged IPs and URLs.
     */
    @Test
    fun testObfuscatedAndDefangedVectors() {
        // Defanged IPv4
        assertTrue(PrivacyGuardValidator.isPrivateIp("10[.]0[.]0[.]1"))
        assertTrue(PrivacyGuardValidator.isPrivateIp("10(.)0(.)0(.)1"))
        assertTrue(PrivacyGuardValidator.isPrivateIp("192[.]168[.]1[.]1"))
        assertTrue(PrivacyGuardValidator.isPrivateIp("172[.]16[.]0[.]1"))
        assertFalse(PrivacyGuardValidator.isPrivateIp("8[.]8[.]8[.]8"))

        // IPv4 with ports (standard and defanged)
        assertTrue(PrivacyGuardValidator.isPrivateIp("192.168.1.1:8080"))
        assertTrue(PrivacyGuardValidator.isPrivateIp("10.0.0.1:443"))
        assertTrue(PrivacyGuardValidator.isPrivateIp("127.0.0.1:9000"))
        assertTrue(PrivacyGuardValidator.isPrivateIp("172.16.1.1:22"))
        assertTrue(PrivacyGuardValidator.isPrivateIp("192[.]168[.]1[.]1[:]8080"))
        assertFalse(PrivacyGuardValidator.isPrivateIp("8.8.8.8:53"))

        // Defanged and credential-bearing URLs
        assertTrue(PrivacyGuardValidator.isPrivateOrInternal("hxxp://127.0.0.1/admin"))
        assertTrue(PrivacyGuardValidator.isPrivateOrInternal("hxxps://10.0.0.1/api/v1"))
        assertTrue(PrivacyGuardValidator.isPrivateOrInternal("http://admin:secret@192.168.1.1:8080/setup"))
        assertTrue(PrivacyGuardValidator.isPrivateOrInternal("hxxp[://]127[.]0[.]0[.]1/admin"))
        assertTrue(PrivacyGuardValidator.isPrivateOrInternal("hxxps://user:pass@172.16.0.1:8443/"))

        // Public URLs must return false
        assertFalse(PrivacyGuardValidator.isPrivateOrInternal("https://admin:password@8.8.8.8:443/"))
        assertFalse(PrivacyGuardValidator.isPrivateOrInternal("http://10.malware.com/payload"))
        assertFalse(PrivacyGuardValidator.isPrivateOrInternal("hxxp://10.malware.com/c2"))
    }

    /**
     * Requirement 3: IPv6 variations (loopback, IPv4-mapped, ULA, link-local, bracketed, ports).
     */
    @Test
    fun testIpv6VariationsAdversarialMatrix() {
        val privateV6 = listOf(
            "0:0:0:0:0:0:0:1",
            "::1",
            "[::1]:9000",
            "::",
            "0000:0000:0000:0000:0000:0000:0000:0001",
            "[0:0:0:0:0:0:0:1]:8080",

            // IPv4-mapped IPv6 (private targets)
            "::ffff:10.0.0.1",
            "::ffff:192.168.1.1",
            "::ffff:172.16.0.1",
            "::ffff:127.0.0.1",
            "[::ffff:10.0.0.1]",

            // Link-Local (fe80::/10)
            "fe80::1",
            "fe80::dead:beef:1",
            "febf:ffff:ffff:ffff:ffff:ffff:ffff:ffff",
            "fe80::1%eth0",

            // Unique Local Addresses ULA (fc00::/7)
            "fc00::1",
            "fd00::1",
            "fd12:3456:789a::1",
            "fdff:ffff:ffff:ffff:ffff:ffff:ffff:ffff"
        )

        for (v6 in privateV6) {
            val isPriv = PrivacyGuardValidator.isPrivateIp(v6)
            assertTrue(isPriv, "IPv6 vector '$v6' was NOT identified as private")
            val isPrivOrInt = PrivacyGuardValidator.isPrivateOrInternal(v6)
            assertTrue(isPrivOrInt, "IPv6 vector '$v6' was NOT identified as privateOrInternal")
        }

        val publicV6 = listOf(
            "2001:4860:4860::8888",
            "2606:4700:4700::1111",
            "2a00:1450:4009:820::200e",
            "::ffff:8.8.8.8",
            "::ffff:172.32.0.1",
            "::ffff:192.169.1.1"
        )

        for (v6 in publicV6) {
            val isPriv = PrivacyGuardValidator.isPrivateIp(v6)
            assertFalse(isPriv, "Public IPv6 '$v6' was falsely identified as private")
            val isPrivOrInt = PrivacyGuardValidator.isPrivateOrInternal(v6)
            assertFalse(isPrivOrInt, "Public IPv6 '$v6' was falsely identified as privateOrInternal")
        }
    }

    /**
     * Requirement 4: Tricky domains and bypass vectors.
     */
    @Test
    fun testTrickyDomainAndBypassVectors() {
        // Domains starting with numbers or resembling IP octets must NOT be flagged as private
        val trickyPublicDomains = listOf(
            "10.malware.com",
            "172.16.attacker.org",
            "192.168.evil.ru",
            "127.badactor.org",
            "localhost.evil.com",
            "evilcorp.com",
            "corporate.org",
            "local-domain.com",
            "internal-network.net",
            "home-network.org",
            "lan-party.io"
        )

        for (domain in trickyPublicDomains) {
            assertFalse(
                PrivacyGuardValidator.isInternalDomain(domain),
                "Public domain '$domain' must NOT be classified as internal domain"
            )
            assertFalse(
                PrivacyGuardValidator.isPrivateOrInternal(domain),
                "Public domain '$domain' must NOT be classified as private or internal"
            )
            val indicator = Indicator(domain, domain, IndicatorType.DOMAIN)
            assertFalse(
                PrivacyGuardValidator.isPrivateOrInternal(indicator),
                "Public domain indicator '$domain' must NOT be classified as private or internal"
            )
        }

        // True internal domains and TLDs
        val internalDomains = listOf(
            "localhost",
            "sub.domain.local",
            "test.corp",
            "service.internal",
            "router.lan",
            "ad.home",
            "api.dev.localhost",
            "app.local." // Trailing dot handling
        )

        for (domain in internalDomains) {
            assertTrue(
                PrivacyGuardValidator.isInternalDomain(domain),
                "Internal domain '$domain' must be classified as internal domain"
            )
            assertTrue(
                PrivacyGuardValidator.isPrivateOrInternal(domain),
                "Internal domain '$domain' must be classified as private or internal"
            )
            val indicator = Indicator(domain, domain, IndicatorType.DOMAIN)
            assertTrue(
                PrivacyGuardValidator.isPrivateOrInternal(indicator),
                "Internal domain indicator '$domain' must be classified as private or internal"
            )
        }
    }

    /**
     * Requirement 5: Active Throwing MockEngine verification.
     * Ensures ZERO outbound network requests are dispatched for ANY private indicator.
     */
    @Test
    fun testThrowingMockEngineZeroNetworkDispatchForPrivateVectors() = runTest {
        var networkDispatchedCount = 0
        val throwingMockEngine = MockEngine { request ->
            networkDispatchedCount++
            throw AssertionError("SECURITY VIOLATION: Outbound HTTP network request dispatched to ${request.url}")
        }
        val client = HttpClient(throwingMockEngine)
        val aggregator = ThreatIntelAggregator(client = client)
        val repository = ZeroKeyThreatIntelRepositoryImpl(client = client, aggregator = aggregator)

        val privateIndicators = listOf(
            // CIDR boundaries
            Indicator("10.0.0.0", "10.0.0.0", IndicatorType.IP_ADDRESS),
            Indicator("10.255.255.255", "10.255.255.255", IndicatorType.IP_ADDRESS),
            Indicator("172.16.0.0", "172.16.0.0", IndicatorType.IP_ADDRESS),
            Indicator("172.31.255.255", "172.31.255.255", IndicatorType.IP_ADDRESS),
            Indicator("192.168.0.0", "192.168.0.0", IndicatorType.IP_ADDRESS),
            Indicator("192.168.255.255", "192.168.255.255", IndicatorType.IP_ADDRESS),
            Indicator("127.0.0.1", "127.0.0.1", IndicatorType.IP_ADDRESS),
            Indicator("169.254.1.1", "169.254.1.1", IndicatorType.IP_ADDRESS),
            Indicator("100.64.0.1", "100.64.0.1", IndicatorType.IP_ADDRESS),

            // Defanged & ports
            Indicator("10[.]0[.]0[.]1", "10.0.0.1", IndicatorType.IP_ADDRESS),
            Indicator("192.168.1.1:8080", "192.168.1.1", IndicatorType.IP_ADDRESS),
            Indicator("hxxp://127.0.0.1/admin", "http://127.0.0.1/admin", IndicatorType.URL),

            // IPv6 variations
            Indicator("0:0:0:0:0:0:0:1", "0:0:0:0:0:0:0:1", IndicatorType.IP_ADDRESS),
            Indicator("::1", "::1", IndicatorType.IP_ADDRESS),
            Indicator("[::1]:9000", "::1", IndicatorType.IP_ADDRESS),
            Indicator("::ffff:10.0.0.1", "::ffff:10.0.0.1", IndicatorType.IP_ADDRESS),
            Indicator("fe80::1", "fe80::1", IndicatorType.IP_ADDRESS),
            Indicator("fc00::1", "fc00::1", IndicatorType.IP_ADDRESS),

            // Internal domains
            Indicator("localhost", "localhost", IndicatorType.DOMAIN),
            Indicator("sub.domain.local", "sub.domain.local", IndicatorType.DOMAIN),
            Indicator("test.corp", "test.corp", IndicatorType.DOMAIN)
        )

        for (indicator in privateIndicators) {
            // Test Aggregator
            val aggregatorVerdict = aggregator.aggregate(indicator)
            assertEquals(0, networkDispatchedCount, "Network dispatched during aggregator.aggregate for $indicator")
            assertEquals(VerdictLevel.CLEAN, aggregatorVerdict.overallVerdict)
            assertEquals(0, aggregatorVerdict.confidenceScore)
            assertTrue(aggregatorVerdict.indicator.isInternalOrPrivate)
            assertEquals("Privacy Guard Local", aggregatorVerdict.sourceResults.first().providerName)

            // Test Repository
            val repoVerdict = repository.analyze(indicator)
            assertEquals(0, networkDispatchedCount, "Network dispatched during repository.analyze for $indicator")
            assertEquals(VerdictLevel.CLEAN, repoVerdict.overallVerdict)
            assertEquals(0, repoVerdict.confidenceScore)
            assertTrue(repoVerdict.indicator.isInternalOrPrivate)
            assertEquals("Privacy Guard Local", repoVerdict.sourceResults.first().providerName)
        }
    }

    /**
     * Requirement 5.1: Negative harness proof.
     * Proves that the throwing MockEngine DOES actively intercept and throw when a public indicator is queried.
     */
    @Test
    fun testThrowingMockEngineFailsIfPublicIndicatorDispatched() = runTest {
        var networkDispatchedCount = 0
        val throwingMockEngine = MockEngine { request ->
            networkDispatchedCount++
            throw AssertionError("EXPECTED ASSERTION: Outbound HTTP network request dispatched to ${request.url}")
        }
        val client = HttpClient(throwingMockEngine)
        val aggregator = ThreatIntelAggregator(client = client)

        val publicIndicator = Indicator("8.8.8.8", "8.8.8.8", IndicatorType.IP_ADDRESS, isInternalOrPrivate = false)

        try {
            aggregator.aggregate(publicIndicator)
        } catch (e: AssertionError) {
            // Expected: MockEngine threw AssertionError which is not caught by catch (e: Exception)
            assertTrue(networkDispatchedCount > 0, "Network calls should have been attempted for public IP")
            return@runTest
        }

        // If not caught at top level, verify that at least one network call was recorded
        assertTrue(networkDispatchedCount > 0, "Expected throwing MockEngine to be invoked for public IP")
    }

    /**
     * Requirement 3 (from Dispatch): Verify that createPrivacyGuardVerdict strictly returns
     * VerdictLevel.CLEAN and confidenceScore == 0.
     */
    @Test
    fun testCreatePrivacyGuardVerdictStrictContract() {
        val indicators = listOf(
            Indicator("10.0.0.1", "10.0.0.1", IndicatorType.IP_ADDRESS),
            Indicator("localhost", "localhost", IndicatorType.DOMAIN),
            Indicator("http://192.168.1.1/admin", "http://192.168.1.1/admin", IndicatorType.URL)
        )

        for (ind in indicators) {
            val verdict = PrivacyGuardValidator.createPrivacyGuardVerdict(ind)
            assertEquals(
                VerdictLevel.CLEAN,
                verdict.overallVerdict,
                "VerdictLevel must strictly be CLEAN for PrivacyGuardVerdict"
            )
            assertEquals(
                0,
                verdict.confidenceScore,
                "confidenceScore must strictly be 0 for PrivacyGuardVerdict"
            )
            assertTrue(verdict.indicator.isInternalOrPrivate)
            assertTrue(verdict.summaryDescription.contains("Privacy Guard Activated"))
            assertEquals(1, verdict.sourceResults.size)
            assertEquals("Privacy Guard Local", verdict.sourceResults.first().providerName)
            assertEquals(VerdictLevel.CLEAN, verdict.sourceResults.first().verdict)
            assertEquals(0, verdict.sourceResults.first().detectionsCount)
        }
    }

    /**
     * Test sneaky indicator: isInternalOrPrivate explicitly set to false, but value is private.
     * The validator and aggregator must still catch it.
     */
    @Test
    fun testSneakyIndicatorWithInternalFlagFalseIsBlocked() = runTest {
        var networkDispatchedCount = 0
        val throwingMockEngine = MockEngine { request ->
            networkDispatchedCount++
            throw AssertionError("SECURITY VIOLATION: Outbound HTTP network request dispatched to ${request.url}")
        }
        val client = HttpClient(throwingMockEngine)
        val aggregator = ThreatIntelAggregator(client = client)

        val sneakyIndicators = listOf(
            Indicator("10.0.0.1", "10.0.0.1", IndicatorType.IP_ADDRESS, isInternalOrPrivate = false),
            Indicator("192.168.1.1", "192.168.1.1", IndicatorType.IP_ADDRESS, isInternalOrPrivate = false),
            Indicator("172.16.0.1", "172.16.0.1", IndicatorType.IP_ADDRESS, isInternalOrPrivate = false),
            Indicator("localhost", "localhost", IndicatorType.DOMAIN, isInternalOrPrivate = false),
            Indicator("test.corp", "test.corp", IndicatorType.DOMAIN, isInternalOrPrivate = false),
            Indicator("http://127.0.0.1/admin", "http://127.0.0.1/admin", IndicatorType.URL, isInternalOrPrivate = false)
        )

        for (sneaky in sneakyIndicators) {
            val verdict = aggregator.aggregate(sneaky)
            assertEquals(0, networkDispatchedCount, "Sneaky indicator $sneaky leaked out network request!")
            assertEquals(VerdictLevel.CLEAN, verdict.overallVerdict)
            assertEquals(0, verdict.confidenceScore)
            assertTrue(verdict.indicator.isInternalOrPrivate)
        }
    }

    /**
     * Test IOCParser delegation consistency.
     */
    @Test
    fun testIOCParserDelegationConsistency() {
        val parsed1 = parser.parse("10.0.0.1")
        assertTrue(parsed1.toIndicator().isInternalOrPrivate)

        val parsed2 = parser.parse("172.16.0.1")
        assertTrue(parsed2.toIndicator().isInternalOrPrivate)

        val parsed3 = parser.parse("192.168.1.1")
        assertTrue(parsed3.toIndicator().isInternalOrPrivate)

        val parsed4 = parser.parse("10.malware.com")
        assertFalse(parsed4.toIndicator().isInternalOrPrivate)

        val parsed5 = parser.parse("localhost")
        assertTrue(parsed5.toIndicator().isInternalOrPrivate)

        val parsed6 = parser.parse("sub.domain.local")
        assertTrue(parsed6.toIndicator().isInternalOrPrivate)

        val parsed7 = parser.parse("test.corp")
        assertTrue(parsed7.toIndicator().isInternalOrPrivate)
    }

    /**
     * REGRESSION TEST:
     * Bracketed IPv4-mapped IPv6 with port (e.g. `[::ffff:10.0.0.1]:8080`) intercepted by PrivacyGuardValidator.
     * Root Cause: BRACKETED_IPV6_REGEX now uses [0-9a-fA-F:.] which includes '.' for IPv4-mapped addresses.
     * Verified invariants:
     * 1. isPrivateIp("[::ffff:10.0.0.1]:8080") returns true.
     * 2. ThreatIntelAggregator intercepts locally with ZERO network dispatches.
     */
    @Test
    fun testEmpiricalVulnerabilityBracketedIpv4MappedIpv6WithPortBypass() = runTest {
        val target = "[::ffff:10.0.0.1]:8080"

        // Demonstrating fix in PrivacyGuardValidator:
        val isDetectedByValidator = PrivacyGuardValidator.isPrivateIp(target)
        assertTrue(isDetectedByValidator, "Bracketed IPv4-mapped IPv6 with port must be detected by isPrivateIp")

        // Demonstrating that ThreatIntelAggregator intercepts locally with zero outbound network calls:
        var networkDispatched = 0
        val throwingMockEngine = MockEngine { request ->
            networkDispatched++
            throw AssertionError("LEAK CONFIRMED: Network request dispatched to ${request.url} for private indicator $target")
        }
        val client = HttpClient(throwingMockEngine)
        val aggregator = ThreatIntelAggregator(client = client)

        val indicator = Indicator(target, target, IndicatorType.IP_ADDRESS, isInternalOrPrivate = false)
        val verdict = aggregator.aggregate(indicator)
        assertEquals(0, networkDispatched, "Zero network calls must be dispatched for private indicator $target")
        assertTrue(verdict.indicator.isInternalOrPrivate, "Indicator must be marked internal or private")
    }
}

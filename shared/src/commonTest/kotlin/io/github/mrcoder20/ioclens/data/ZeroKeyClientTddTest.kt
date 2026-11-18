package io.github.mrcoder20.ioclens.data

import io.github.mrcoder20.ioclens.data.remote.AlienVaultOtxProvider
import io.github.mrcoder20.ioclens.data.remote.CloudflareDnsProvider
import io.github.mrcoder20.ioclens.data.remote.CrtShProvider
import io.github.mrcoder20.ioclens.data.remote.IpWhoIsProvider
import io.github.mrcoder20.ioclens.data.remote.MalwareBazaarProvider
import io.github.mrcoder20.ioclens.data.remote.RdapProvider
import io.github.mrcoder20.ioclens.data.remote.RipeStatProvider
import io.github.mrcoder20.ioclens.data.remote.ShodanInternetDbProvider
import io.github.mrcoder20.ioclens.data.remote.ThreatFoxProvider
import io.github.mrcoder20.ioclens.data.remote.ThreatIntelAggregator
import io.github.mrcoder20.ioclens.data.remote.UrlhausProvider
import io.github.mrcoder20.ioclens.domain.model.AppSettings
import io.github.mrcoder20.ioclens.domain.model.Indicator
import io.github.mrcoder20.ioclens.domain.model.IndicatorType
import io.github.mrcoder20.ioclens.domain.model.VerdictLevel
import io.github.mrcoder20.ioclens.domain.parser.IOCParser
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TDD Automated Test Suite verifying:
 * 1. 100% Zero-Key Direct Architecture: IP, Domain, URL, and Hash indicators execute
 *    across all 10 Zero-Key engines with 0 API keys configured (AppSettings()).
 * 2. Client-Side RFC 1918 Privacy Guard: Scanning 192.168.1.1 or any internal indicator
 *    immediately returns a local Privacy Guard notice with EXACTLY ZERO outbound network calls.
 * 3. Individual Zero-Key engines (Shodan InternetDB, AlienVault OTX, RIPE Stat, crt.sh, ipwho.is)
 *    parse telemetry without authentication.
 */
class ZeroKeyClientTddTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private val parser = IOCParser()

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun TestScope.safeMockEngine(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
    ): MockEngine {
        return MockEngine(
            MockEngineConfig().apply {
                dispatcher = StandardTestDispatcher(testScheduler)
                addHandler(handler)
            }
        )
    }

    @Test
    fun testAllTenCoreEnginesAreZeroKey() {
        val zeroKeyProviders = listOf(
            UrlhausProvider(),
            MalwareBazaarProvider(),
            ThreatFoxProvider(),
            RdapProvider(),
            CloudflareDnsProvider(),
            ShodanInternetDbProvider(),
            AlienVaultOtxProvider(),
            RipeStatProvider(),
            CrtShProvider(),
            IpWhoIsProvider()
        )

        assertEquals(10, zeroKeyProviders.size)
        for (provider in zeroKeyProviders) {
            assertTrue(
                provider.isZeroKey,
                "Provider '${provider.name}' must be configured as Zero-Key (isZeroKey = true)"
            )
        }
    }

    @Test
    fun testRfc1918PrivacyGuardBlocks19216811WithZeroNetworkCalls() = runTest {
        var networkCallCount = 0
        val mockEngine = safeMockEngine { request ->
            networkCallCount++
            error("Privacy Guard VIOLATION: Network request dispatched to ${request.url}")
        }

        val client = HttpClient(mockEngine)
        val aggregator = ThreatIntelAggregator(client, timeoutMs = 1500L)
        val indicator = parser.parse("192.168.1.1").toIndicator()

        assertTrue(indicator.isInternalOrPrivate, "192.168.1.1 must be recognized as internal/private RFC 1918")

        val settings = AppSettings() // 0 keys configured
        val verdict = aggregator.aggregate(indicator, settings)

        assertEquals(0, networkCallCount, "Expected exactly 0 network calls for RFC 1918 indicator")
        assertEquals(VerdictLevel.CLEAN, verdict.overallVerdict)
        assertEquals(0, verdict.confidenceScore)
        assertTrue(verdict.summaryDescription.contains("Privacy Guard Activated"))
        assertTrue(verdict.sourceResults.any { it.providerName == "Privacy Guard Local" })
    }

    @Test
    fun testRfc1918PrivacyGuardBlocksAllPrivateRanges() = runTest {
        val privateSamples = listOf(
            "10.0.0.1",
            "10.254.12.8",
            "172.16.0.1",
            "172.31.255.254",
            "192.168.0.1",
            "192.168.100.50",
            "127.0.0.1",
            "169.254.1.1",
            "::1",
            "fc00::1",
            "company.local",
            "test.internal",
            "router.lan"
        )

        var outboundCalls = 0
        val mockEngine = safeMockEngine {
            outboundCalls++
            respond("{}", HttpStatusCode.OK, jsonHeaders)
        }
        val client = HttpClient(mockEngine)
        val aggregator = ThreatIntelAggregator(client, timeoutMs = 1000L)

        for (sample in privateSamples) {
            val indicator = parser.parse(sample).toIndicator()
            assertTrue(indicator.isInternalOrPrivate, "Sample '$sample' should be flagged as internal/private")

            val verdict = aggregator.aggregate(indicator, AppSettings())
            assertEquals(0, outboundCalls, "Dispatched network call for internal indicator '$sample'")
            assertTrue(verdict.summaryDescription.contains("Privacy Guard Activated"))
        }
    }

    @Test
    fun testZeroKeyIpScanWithZeroApiKeys() = runTest {
        val mockEngine = safeMockEngine { request ->
            val url = request.url.toString()
            when {
                url.contains("threatfox-api.abuse.ch") -> {
                    respond("""{"query_status": "ok", "data": [{"threat_type": "botnet_c2", "confidence_level": 95}]}""", HttpStatusCode.OK, jsonHeaders)
                }
                url.contains("rdap.org") -> {
                    respond("""{"handle": "NET-8-8-8-0", "name": "GOOGLE"}""", HttpStatusCode.OK, jsonHeaders)
                }
                url.contains("internetdb.shodan.io") -> {
                    respond("""{"vulns": ["CVE-2023-1234"], "ports": [53, 443], "hostnames": ["dns.google"]}""", HttpStatusCode.OK, jsonHeaders)
                }
                url.contains("otx.alienvault.com") -> {
                    respond("""{"pulse_info": {"count": 2}}""", HttpStatusCode.OK, jsonHeaders)
                }
                url.contains("stat.ripe.net") -> {
                    respond("""{"data": {"asns": ["15169"], "prefix": "8.8.8.0/24"}}""", HttpStatusCode.OK, jsonHeaders)
                }
                url.contains("ipwho.is") -> {
                    respond("""{"success": true, "connection": {"isp": "Google LLC"}, "country": "US", "security": {"tor": false, "proxy": false, "vpn": false, "anonymous": false}}""", HttpStatusCode.OK, jsonHeaders)
                }
                else -> respond("{}", HttpStatusCode.OK, jsonHeaders)
            }
        }

        val client = HttpClient(mockEngine)
        val aggregator = ThreatIntelAggregator(client, timeoutMs = 2000L)
        val indicator = parser.parse("8.8.8.8").toIndicator()
        assertFalse(indicator.isInternalOrPrivate)
        assertEquals(IndicatorType.IP_ADDRESS, indicator.type)

        val settings = AppSettings() // 0 keys configured
        val verdict = aggregator.aggregate(indicator, settings)

        // Verify that applicable Zero-Key providers ran
        val names = verdict.sourceResults.map { it.providerName }
        assertTrue(names.contains("ThreatFox (abuse.ch)"), "Missing ThreatFox in $names")
        assertTrue(names.contains("RDAP / Registry"), "Missing RDAP in $names")
        assertTrue(names.contains("Shodan InternetDB"), "Missing Shodan in $names")
        assertTrue(names.contains("AlienVault OTX"), "Missing AlienVault OTX in $names")
        assertTrue(names.contains("RIPE Stat"), "Missing RIPE Stat in $names")
        assertTrue(names.contains("ipwho.is"), "Missing ipwho.is in $names")

        // Verify no authenticated-only provider ran without key
        assertFalse(names.contains("VirusTotal"), "VirusTotal must not run with 0 keys")
        assertFalse(names.contains("AbuseIPDB"), "AbuseIPDB must not run with 0 keys")
        assertFalse(names.contains("IPQualityScore"), "IPQualityScore must not run with 0 keys")

        // ThreatFox flagged botnet_c2 -> overall verdict MALICIOUS
        assertEquals(VerdictLevel.MALICIOUS, verdict.overallVerdict)
    }

    @Test
    fun testZeroKeyDomainScanWithZeroApiKeys() = runTest {
        val mockEngine = safeMockEngine { request ->
            val url = request.url.toString()
            when {
                url.contains("urlhaus-api.abuse.ch") -> {
                    respond("""{"query_status": "ok", "threat": "malware_download", "url_status": "online"}""", HttpStatusCode.OK, jsonHeaders)
                }
                url.contains("threatfox-api.abuse.ch") -> {
                    respond("""{"query_status": "no_result"}""", HttpStatusCode.OK, jsonHeaders)
                }
                url.contains("rdap.org") -> {
                    respond("""{"handle": "DOM-123", "name": "MALICIOUS.COM"}""", HttpStatusCode.OK, jsonHeaders)
                }
                url.contains("cloudflare-dns.com") -> {
                    respond("""{"Status": 0}""", HttpStatusCode.OK, jsonHeaders)
                }
                url.contains("otx.alienvault.com") -> {
                    respond("""{"pulse_info": {"count": 4}}""", HttpStatusCode.OK, jsonHeaders)
                }
                url.contains("crt.sh") -> {
                    respond("""[{"issuer_name": "C=US, O=Let's Encrypt"}, {"issuer_name": "C=US, O=DigiCert"}]""", HttpStatusCode.OK, jsonHeaders)
                }
                else -> respond("{}", HttpStatusCode.OK, jsonHeaders)
            }
        }

        val client = HttpClient(mockEngine)
        val aggregator = ThreatIntelAggregator(client, timeoutMs = 2000L)
        val indicator = parser.parse("malicious-c2.com").toIndicator()
        assertEquals(IndicatorType.DOMAIN, indicator.type)

        val settings = AppSettings() // 0 keys configured
        val verdict = aggregator.aggregate(indicator, settings)

        val names = verdict.sourceResults.map { it.providerName }
        assertTrue(names.contains("URLhaus (abuse.ch)"))
        assertTrue(names.contains("ThreatFox (abuse.ch)"))
        assertTrue(names.contains("RDAP / Registry"))
        assertTrue(names.contains("Cloudflare DoH"))
        assertTrue(names.contains("AlienVault OTX"))
        assertTrue(names.contains("crt.sh"))

        // Malicious from URLhaus & AlienVault (pulseCount >= 3)
        assertEquals(VerdictLevel.MALICIOUS, verdict.overallVerdict)
    }

    @Test
    fun testZeroKeyUrlScanWithZeroApiKeys() = runTest {
        val mockEngine = safeMockEngine { request ->
            val url = request.url.toString()
            when {
                url.contains("urlhaus-api.abuse.ch") -> {
                    respond("""{"query_status": "no_results"}""", HttpStatusCode.OK, jsonHeaders)
                }
                url.contains("threatfox-api.abuse.ch") -> {
                    respond("""{"query_status": "no_result"}""", HttpStatusCode.OK, jsonHeaders)
                }
                url.contains("otx.alienvault.com") -> {
                    respond("""{"pulse_info": {"count": 0}}""", HttpStatusCode.OK, jsonHeaders)
                }
                else -> respond("{}", HttpStatusCode.OK, jsonHeaders)
            }
        }

        val client = HttpClient(mockEngine)
        val aggregator = ThreatIntelAggregator(client, timeoutMs = 5000L)
        val indicator = parser.parse("https://safe-example.org/downloads/doc.pdf").toIndicator()
        assertEquals(IndicatorType.URL, indicator.type)

        val verdict = aggregator.aggregate(indicator, AppSettings())
        val names = verdict.sourceResults.map { it.providerName }
        assertTrue(names.contains("URLhaus (abuse.ch)"))
        assertTrue(names.contains("ThreatFox (abuse.ch)"))
        assertTrue(names.contains("AlienVault OTX"))
        assertEquals(VerdictLevel.CLEAN, verdict.overallVerdict)
    }

    @Test
    fun testMalwareBazaarProviderDirect() = runTest {
        val mockEngine = safeMockEngine { request ->
            respond("""{"query_status": "ok", "signature": "WannaCry", "file_type": "exe"}""", HttpStatusCode.OK, jsonHeaders)
        }
        val client = HttpClient(mockEngine)
        val provider = MalwareBazaarProvider()
        val indicator = Indicator("hash123", "hash123", IndicatorType.FILE_HASH)
        val res = provider.query(client, indicator, AppSettings())
        assertEquals(VerdictLevel.MALICIOUS, res.verdict)
    }

    @Test
    fun testThreatFoxProviderDirect() = runTest {
        val mockEngine = safeMockEngine { request ->
            respond("""{"query_status": "ok", "data": [{"threat_type": "payload", "malware": "win.wannacry", "confidence_level": 100}]}""", HttpStatusCode.OK, jsonHeaders)
        }
        val client = HttpClient(mockEngine)
        val provider = ThreatFoxProvider()
        val indicator = Indicator("hash123", "hash123", IndicatorType.FILE_HASH)
        val res = provider.query(client, indicator, AppSettings())
        assertEquals(VerdictLevel.MALICIOUS, res.verdict)
    }

    @Test
    fun testAlienVaultOtxProviderDirect() = runTest {
        val mockEngine = safeMockEngine { request ->
            respond("""{"pulse_info": {"count": 5}}""", HttpStatusCode.OK, jsonHeaders)
        }
        val client = HttpClient(mockEngine)
        val provider = AlienVaultOtxProvider()
        val indicator = Indicator("hash123", "hash123", IndicatorType.FILE_HASH)
        val res = provider.query(client, indicator, AppSettings())
        assertEquals(VerdictLevel.MALICIOUS, res.verdict)
    }

    @Test
    fun testZeroKeyHashScanWithZeroApiKeys() = runTest {
        val mockEngine = safeMockEngine { request ->
            val url = request.url.toString()
            when {
                url.contains("mb-api.abuse.ch") -> {
                    respond("""{"query_status": "ok", "signature": "WannaCry.Ransomware", "file_type": "exe"}""", HttpStatusCode.OK, jsonHeaders)
                }
                url.contains("threatfox-api.abuse.ch") -> {
                    respond("""{"query_status": "no_result"}""", HttpStatusCode.OK, jsonHeaders)
                }
                url.contains("otx.alienvault.com") -> {
                    respond("""{"pulse_info": {"count": 10}}""", HttpStatusCode.OK, jsonHeaders)
                }
                else -> respond("{}", HttpStatusCode.OK, jsonHeaders)
            }
        }

        val client = HttpClient(mockEngine)
        val aggregator = ThreatIntelAggregator(client, timeoutMs = 5000L)
        val indicator = parser.parse("84c82835a5d21bbcf75a61706d8ab549").toIndicator()
        assertEquals(IndicatorType.FILE_HASH, indicator.type)

        val verdict = aggregator.aggregate(indicator, AppSettings())
        val names = verdict.sourceResults.map { it.providerName }
        assertTrue(names.contains("MalwareBazaar (abuse.ch)"))
        assertTrue(names.contains("ThreatFox (abuse.ch)"))
        assertTrue(names.contains("AlienVault OTX"))
        assertEquals(VerdictLevel.MALICIOUS, verdict.overallVerdict)
    }

    @Test
    fun testShodanInternetDbParsesOpenPortsAndCves() = runTest {
        val mockEngine = safeMockEngine {
            respond(
                content = """{"vulns": ["CVE-2021-44228", "CVE-2022-22965"], "ports": [80, 443, 8080], "hostnames": ["vulnerable-server.org"], "tags": ["vpn"]}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders
            )
        }

        val provider = ShodanInternetDbProvider()
        val client = HttpClient(mockEngine)
        val indicator = Indicator("198.51.100.22", "198.51.100.22", IndicatorType.IP_ADDRESS)
        val result = provider.query(client, indicator, AppSettings())

        assertEquals(VerdictLevel.MALICIOUS, result.verdict)
        assertEquals(2, result.detectionsCount)
        assertTrue(result.summary.contains("2 CVEs"))
        assertTrue(result.summary.contains("3 open ports"))
    }

    @Test
    fun testIpWhoIsDetectsTorExitNodeAndVpn() = runTest {
        // TOR Exit Node -> MALICIOUS
        val mockTor = safeMockEngine {
            respond(
                content = """{"success": true, "connection": {"isp": "Tor Relay"}, "country": "DE", "security": {"tor": true, "proxy": true, "vpn": false, "anonymous": true}}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders
            )
        }
        val provider = IpWhoIsProvider()
        val clientTor = HttpClient(mockTor)
        val resTor = provider.query(clientTor, Indicator("185.220.101.5", "185.220.101.5", IndicatorType.IP_ADDRESS), AppSettings())
        assertEquals(VerdictLevel.MALICIOUS, resTor.verdict)
        assertTrue(resTor.summary.contains("Active TOR exit node"))

        // VPN / Proxy -> SUSPICIOUS
        val mockVpn = safeMockEngine {
            respond(
                content = """{"success": true, "connection": {"isp": "Mullvad"}, "country": "SE", "security": {"tor": false, "proxy": false, "vpn": true, "anonymous": true}}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders
            )
        }
        val clientVpn = HttpClient(mockVpn)
        val resVpn = provider.query(clientVpn, Indicator("193.138.218.74", "193.138.218.74", IndicatorType.IP_ADDRESS), AppSettings())
        assertEquals(VerdictLevel.SUSPICIOUS, resVpn.verdict)
        assertTrue(resVpn.summary.contains("Proxy/VPN anonymous endpoint"))
    }

    @Test
    fun testCrtShAndRipeStatEnrichment() = runTest {
        val mockCrt = safeMockEngine {
            respond(
                content = """[{"issuer_name": "Let's Encrypt"}, {"issuer_name": "DigiCert"}, {"issuer_name": "Sectigo"}]""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders
            )
        }
        val crtProvider = CrtShProvider()
        val crtResult = crtProvider.query(HttpClient(mockCrt), Indicator("github.com", "github.com", IndicatorType.DOMAIN), AppSettings())
        assertEquals(VerdictLevel.NO_DATA, crtResult.verdict)
        assertTrue(crtResult.summary.contains("3 logged certificates discovered"))

        val mockRipe = safeMockEngine {
            respond(
                content = """{"data": {"asns": ["13335"], "prefix": "1.1.1.0/24"}}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders
            )
        }
        val ripeProvider = RipeStatProvider()
        val ripeResult = ripeProvider.query(HttpClient(mockRipe), Indicator("1.1.1.1", "1.1.1.1", IndicatorType.IP_ADDRESS), AppSettings())
        assertEquals(VerdictLevel.NO_DATA, ripeResult.verdict)
        assertTrue(ripeResult.summary.contains("AS13335"))
        assertTrue(ripeResult.summary.contains("1.1.1.0/24"))
    }
}

package io.github.mrcoder20.ioclens.data

import io.github.mrcoder20.ioclens.data.remote.ThreatIntelAggregator
import io.github.mrcoder20.ioclens.domain.model.AppSettings
import io.github.mrcoder20.ioclens.domain.model.Indicator
import io.github.mrcoder20.ioclens.domain.model.IndicatorType
import io.github.mrcoder20.ioclens.domain.model.VerdictLevel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ThreatIntelAggregatorTest {

    @Test
    fun testZeroKeyPublicMaliciousDetection() = runTest {
        val mockEngine = MockEngine { request ->
            val url = request.url.toString()
            when {
                url.contains("urlhaus-api.abuse.ch") -> {
                    respond(
                        content = """{"query_status": "ok", "url_status": "online", "threat": "malware_download"}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                url.contains("threatfox-api.abuse.ch") -> {
                    respond(
                        content = """{"query_status": "ok", "data": [{"threat_type": "botnet_c2", "confidence_level": 90}]}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                url.contains("rdap.org") -> {
                    respond(
                        content = """{"handle": "NET-8-8-8-0-1", "name": "LVLT-GOGL-8-8-8"}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                else -> {
                    respond(
                        content = "{}",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            }
        }

        val client = HttpClient(mockEngine)
        val aggregator = ThreatIntelAggregator(client, timeoutMs = 2000L)
        val indicator = Indicator(
            rawValue = "http://evil-malware.com/payload.exe",
            normalizedValue = "http://evil-malware.com/payload.exe",
            type = IndicatorType.URL,
            isInternalOrPrivate = false
        )
        val settings = AppSettings()

        val verdict = aggregator.aggregate(indicator, settings)

        assertEquals(VerdictLevel.MALICIOUS, verdict.overallVerdict)
        assertTrue(verdict.confidenceScore >= 70)
        assertTrue(verdict.sourceResults.isNotEmpty())
    }

    @Test
    fun testProviderTimeoutResilience() = runTest {
        val mockEngine = MockEngine { request ->
            val url = request.url.toString()
            if (url.contains("threatfox-api.abuse.ch")) {
                // Simulate slow provider exceeding timeout
                delay(1500)
                respond("{}", HttpStatusCode.OK)
            } else {
                respond(
                    content = """{"query_status": "no_results"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        }

        val client = HttpClient(mockEngine)
        // Set strict 500ms timeout
        val aggregator = ThreatIntelAggregator(client, timeoutMs = 500L)
        val indicator = Indicator(
            rawValue = "1.1.1.1",
            normalizedValue = "1.1.1.1",
            type = IndicatorType.IP_ADDRESS,
            isInternalOrPrivate = false
        )

        val verdict = aggregator.aggregate(indicator, AppSettings())

        // App must NOT crash or hang; slow provider is skipped/marked NO_DATA
        assertTrue(verdict.sourceResults.any { it.providerName.contains("ThreatFox") && it.verdict == VerdictLevel.NO_DATA })
    }

    @Test
    fun testHttpErrorResilience() = runTest {
        val mockEngine = MockEngine { request ->
            // Simulate 503 Service Unavailable / network error on one service
            if (request.url.toString().contains("urlhaus-api.abuse.ch")) {
                respond("Service Unavailable", HttpStatusCode.ServiceUnavailable)
            } else {
                respond(
                    content = """{"query_status": "ok", "url_status": "offline"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        }

        val client = HttpClient(mockEngine)
        val aggregator = ThreatIntelAggregator(client, timeoutMs = 1000L)
        val indicator = Indicator(
            rawValue = "https://example.com",
            normalizedValue = "https://example.com",
            type = IndicatorType.URL,
            isInternalOrPrivate = false
        )

        val verdict = aggregator.aggregate(indicator, AppSettings())

        // Surviving providers returned results without failure
        assertTrue(verdict.sourceResults.isNotEmpty())
    }

    @Test
    fun testAuthenticatedProviderWithKey() = runTest {
        var vtKeyReceived: String? = null
        val mockEngine = MockEngine { request ->
            if (request.url.toString().contains("virustotal.com")) {
                vtKeyReceived = request.headers["x-apikey"]
                respond(
                    content = """{"data": {"attributes": {"last_analysis_stats": {"malicious": 5, "suspicious": 1, "harmless": 80}}}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            } else {
                respond("{}", HttpStatusCode.OK)
            }
        }

        val client = HttpClient(mockEngine)
        val aggregator = ThreatIntelAggregator(client, timeoutMs = 1000L)
        val indicator = Indicator(
            rawValue = "275a021bbfb6489e54d471899f7db9d1663fc695ec2fe2a2c4538aabf651fd0f",
            normalizedValue = "275a021bbfb6489e54d471899f7db9d1663fc695ec2fe2a2c4538aabf651fd0f",
            type = IndicatorType.FILE_HASH,
            isInternalOrPrivate = false
        )
        val settings = AppSettings(virusTotalApiKey = "test-secret-key-12345")

        val verdict = aggregator.aggregate(indicator, settings)

        assertEquals("test-secret-key-12345", vtKeyReceived)
        assertTrue(verdict.sourceResults.any { it.providerName.contains("VirusTotal") && it.verdict == VerdictLevel.MALICIOUS })
    }

    @Test
    fun testVirusTotalUrlIdentifierLookup() = runTest {
        val mutex = Mutex()
        val requestedEndpoints = mutableListOf<String>()
        val mockEngine = MockEngine { request ->
            val url = request.url.toString()
            mutex.withLock {
                requestedEndpoints.add(url)
            }
            respond(
                content = """{"data": {"attributes": {"last_analysis_stats": {"malicious": 8, "suspicious": 2, "harmless": 60}}}}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = HttpClient(mockEngine)
        val aggregator = ThreatIntelAggregator(client, timeoutMs = 1000L)
        val indicator = Indicator(
            rawValue = "https://evil.com/phish",
            normalizedValue = "https://evil.com/phish",
            type = IndicatorType.URL,
            isInternalOrPrivate = false
        )
        val settings = AppSettings(virusTotalApiKey = "my-vt-key")

        val verdict = aggregator.aggregate(indicator, settings)

        assertTrue(requestedEndpoints.any { it.contains("/api/v3/urls/") }, "Expected VT URL endpoint to be queried in $requestedEndpoints")
        assertTrue(verdict.sourceResults.any { it.providerName.contains("VirusTotal") && it.verdict == VerdictLevel.MALICIOUS })
    }

    @Test
    fun testIpQualityScoreWithKey() = runTest {
        val mockEngine = MockEngine { request ->
            if (request.url.toString().contains("ipqualityscore.com")) {
                respond(
                    content = """{"success": true, "fraud_score": 85, "recent_abuse": true, "bot_status": false}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            } else {
                respond("{}", HttpStatusCode.OK)
            }
        }

        val client = HttpClient(mockEngine)
        val aggregator = ThreatIntelAggregator(client, timeoutMs = 1000L)
        val indicator = Indicator(
            rawValue = "185.220.101.5",
            normalizedValue = "185.220.101.5",
            type = IndicatorType.IP_ADDRESS,
            isInternalOrPrivate = false
        )
        val settings = AppSettings(ipQualityScoreApiKey = "ipqs-secret-key")

        val verdict = aggregator.aggregate(indicator, settings)

        assertTrue(verdict.sourceResults.any { it.providerName.contains("IPQualityScore") && it.verdict == VerdictLevel.MALICIOUS })
    }

    @Test
    fun testRdapAndDnsDoNotFalselyAssertCleanForUnknownDomain() = runTest {
        val mockEngine = MockEngine { request ->
            val url = request.url.toString()
            when {
                url.contains("rdap.org") -> {
                    respond(
                        content = """{"handle": "DOMAIN-1", "name": "MALICIOUS-UNKNOWN.COM"}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                url.contains("cloudflare-dns.com") -> {
                    respond(
                        content = """{"Status": 0}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                else -> {
                    // Threat engines have no record
                    respond(
                        content = """{"query_status": "no_results"}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            }
        }

        val client = HttpClient(mockEngine)
        val aggregator = ThreatIntelAggregator(client, timeoutMs = 1000L)
        val indicator = Indicator(
            rawValue = "malicious-unknown.com",
            normalizedValue = "malicious-unknown.com",
            type = IndicatorType.DOMAIN,
            isInternalOrPrivate = false
        )

        val verdict = aggregator.aggregate(indicator, AppSettings())

        // RDAP and DoH must be NO_DATA (enrichment/resolution), NOT CLEAN
        val rdapResult = verdict.sourceResults.find { it.providerName.contains("RDAP") }
        val dnsResult = verdict.sourceResults.find { it.providerName.contains("Cloudflare") }
        assertEquals(VerdictLevel.NO_DATA, rdapResult?.verdict)
        assertEquals(VerdictLevel.NO_DATA, dnsResult?.verdict)
    }
}

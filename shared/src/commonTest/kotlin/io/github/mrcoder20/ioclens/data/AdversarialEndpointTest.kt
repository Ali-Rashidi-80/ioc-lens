package io.github.mrcoder20.ioclens.data

import io.github.mrcoder20.ioclens.core.storage.KeyValueStorage
import io.github.mrcoder20.ioclens.data.remote.CloudflareDnsProvider
import io.github.mrcoder20.ioclens.data.remote.IpQualityScoreProvider
import io.github.mrcoder20.ioclens.data.remote.PulsediveProvider
import io.github.mrcoder20.ioclens.data.remote.RdapProvider
import io.github.mrcoder20.ioclens.data.remote.ThreatIntelAggregator
import io.github.mrcoder20.ioclens.data.remote.VirusTotalProvider
import io.github.mrcoder20.ioclens.data.repository.SettingsRepositoryImpl
import io.github.mrcoder20.ioclens.domain.model.AppSettings
import io.github.mrcoder20.ioclens.domain.model.Indicator
import io.github.mrcoder20.ioclens.domain.model.IndicatorType
import io.github.mrcoder20.ioclens.domain.model.SourceResult
import io.github.mrcoder20.ioclens.domain.model.TriageVerdict
import io.github.mrcoder20.ioclens.domain.model.VerdictLevel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdversarialEndpointTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    @Test
    fun testVirusTotalHttp429RateLimitProducesRateLimitedStatus() = runTest {
        val mockEngine = MockEngine { request ->
            if (request.url.toString().contains("virustotal.com")) {
                respond(
                    content = """{"error": {"code": "QuotaExceededError", "message": "Quota exceeded"}}""",
                    status = HttpStatusCode.TooManyRequests,
                    headers = jsonHeaders
                )
            } else {
                respond("{}", HttpStatusCode.OK, jsonHeaders)
            }
        }

        val client = HttpClient(mockEngine)
        val provider = VirusTotalProvider()
        val indicator = Indicator("8.8.8.8", "8.8.8.8", IndicatorType.IP_ADDRESS)
        val settings = AppSettings(virusTotalApiKey = "dummy_vt_key")

        val result = provider.query(client, indicator, settings)

        assertTrue(result.isRateLimited, "VirusTotal result must flag isRateLimited = true on HTTP 429")
        assertTrue(result.summary.contains("Rate limit exceeded", ignoreCase = true))
        assertTrue(result.summary.contains("429"))
        assertEquals(VerdictLevel.NO_DATA, result.verdict)
    }

    @Test
    fun testCloudflareDohUnderDnsNxdomainStatus3() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = """{"Status": 3, "TC": false, "RD": true, "RA": true, "AD": false, "CD": false, "Question": [{"name": "nonexistent.example.com.", "type": 1}]}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders
            )
        }

        val client = HttpClient(mockEngine)
        val provider = CloudflareDnsProvider()
        val indicator = Indicator("nonexistent.example.com", "nonexistent.example.com", IndicatorType.DOMAIN)
        val settings = AppSettings()

        val result = provider.query(client, indicator, settings)

        assertEquals(VerdictLevel.LOW_RISK, result.verdict, "NXDOMAIN should be classified as LOW_RISK")
        assertTrue(result.summary.contains("NXDOMAIN"))
        assertTrue(result.summary.contains("Status 3"))
    }

    @Test
    fun testCloudflareDohUnderDnsServfailStatus2() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = """{"Status": 2, "TC": false, "RD": true, "RA": true, "AD": false, "CD": false}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders
            )
        }

        val client = HttpClient(mockEngine)
        val provider = CloudflareDnsProvider()
        val indicator = Indicator("servfail.broken-dns.org", "servfail.broken-dns.org", IndicatorType.DOMAIN)
        val settings = AppSettings()

        val result = provider.query(client, indicator, settings)

        assertEquals(VerdictLevel.NO_DATA, result.verdict)
        assertTrue(result.summary.contains("SERVFAIL"))
        assertTrue(result.summary.contains("Status 2"))
    }

    @Test
    fun testCloudflareDohUnderNoErrorStatus0() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = """{"Status": 0, "TC": false, "RD": true, "RA": true, "AD": true, "Answer": [{"name": "cloudflare.com", "type": 1, "data": "104.16.132.229"}]}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders
            )
        }

        val client = HttpClient(mockEngine)
        val provider = CloudflareDnsProvider()
        val indicator = Indicator("cloudflare.com", "cloudflare.com", IndicatorType.DOMAIN)
        val settings = AppSettings()

        val result = provider.query(client, indicator, settings)

        assertEquals(VerdictLevel.NO_DATA, result.verdict)
        assertTrue(result.summary.contains("NOERROR (Status 0)"))
    }

    @Test
    fun testRdapProviderUnder404NotFound() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = "Not Found",
                status = HttpStatusCode.NotFound
            )
        }

        val client = HttpClient(mockEngine)
        val provider = RdapProvider()
        val indicator = Indicator("unregistered-xyz987.org", "unregistered-xyz987.org", IndicatorType.DOMAIN)
        val settings = AppSettings()

        val result = provider.query(client, indicator, settings)

        assertEquals(VerdictLevel.NO_DATA, result.verdict)
        assertTrue(result.summary.contains("Unregistered") || result.summary.contains("HTTP 404"))
    }

    @Test
    fun testRdapProviderWithMissingRegistrarFieldsGracefulFallback() = runTest {
        val mockEngine = MockEngine {
            // Missing handle, missing name, missing country
            respond(
                content = """{"objectClassName": "entity"}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders
            )
        }

        val client = HttpClient(mockEngine)
        val provider = RdapProvider()
        val indicator = Indicator("198.51.100.1", "198.51.100.1", IndicatorType.IP_ADDRESS)
        val settings = AppSettings()

        val result = provider.query(client, indicator, settings)

        assertEquals(VerdictLevel.NO_DATA, result.verdict)
        assertTrue(result.summary.contains("Allocated Network"))
    }

    @Test
    fun testAggregatorResilienceToHttp500AndCorruptedJson() = runTest {
        val mockEngine = MockEngine { request ->
            val url = request.url.toString()
            when {
                url.contains("urlhaus-api.abuse.ch") -> {
                    respond("Internal Server Error", HttpStatusCode.InternalServerError)
                }
                url.contains("threatfox-api.abuse.ch") -> {
                    // Truncated / malformed JSON
                    respond("""{"query_status": "ok", "data": [ {"threat_type": """, HttpStatusCode.OK, jsonHeaders)
                }
                url.contains("rdap.org") -> {
                    // Empty body
                    respond("", HttpStatusCode.OK)
                }
                else -> {
                    respond("{}", HttpStatusCode.OK, jsonHeaders)
                }
            }
        }

        val client = HttpClient(mockEngine)
        val aggregator = ThreatIntelAggregator(client, timeoutMs = 2000L)
        val indicator = Indicator("example.com", "example.com", IndicatorType.DOMAIN)
        val settings = AppSettings()

        val verdict = aggregator.aggregate(indicator, settings)

        // Aggregator must not crash or throw unhandled exceptions
        assertEquals(VerdictLevel.NO_DATA, verdict.overallVerdict)
        val results = verdict.sourceResults

        val urlhausResult = results.firstOrNull { it.providerName.contains("URLhaus") }
        assertTrue(urlhausResult != null && urlhausResult.summary.contains("Server error"))

        val threatFoxResult = results.firstOrNull { it.providerName.contains("ThreatFox") }
        assertTrue(threatFoxResult != null && threatFoxResult.summary.contains("Invalid or corrupted"))

        val rdapResult = results.firstOrNull { it.providerName.contains("RDAP") }
        assertTrue(rdapResult != null && rdapResult.summary.contains("Invalid or corrupted"))
    }

    @Test
    fun testAggregatorResilienceToNetworkDropsMidStream() = runTest {
        val mockEngine = MockEngine {
            respondError(HttpStatusCode.BadGateway, "Bad Gateway 502")
        }

        val client = HttpClient(mockEngine)
        val aggregator = ThreatIntelAggregator(client, timeoutMs = 2000L)
        val indicator = Indicator("8.8.8.8", "8.8.8.8", IndicatorType.IP_ADDRESS)
        val settings = AppSettings()

        val verdict = aggregator.aggregate(indicator, settings)

        assertEquals(VerdictLevel.NO_DATA, verdict.overallVerdict)
        assertTrue(verdict.sourceResults.all { it.verdict == VerdictLevel.NO_DATA })
    }

    @Test
    fun testUnknownIndicatorNeverInvokesRemoteEngines() = runTest {
        var remoteCalls = 0
        val mockEngine = MockEngine {
            remoteCalls++
            respond("{}", HttpStatusCode.OK, jsonHeaders)
        }

        val client = HttpClient(mockEngine)
        val aggregator = ThreatIntelAggregator(client, timeoutMs = 2000L)
        val unknownIndicator = Indicator("invalid_payload_input", "invalid_payload_input", IndicatorType.UNKNOWN)
        val settings = AppSettings()

        val verdict = aggregator.aggregate(unknownIndicator, settings)

        assertEquals(0, remoteCalls, "Remote engines must NEVER be invoked for UNKNOWN indicators")
        assertEquals(VerdictLevel.NO_DATA, verdict.overallVerdict)
        assertEquals(0, verdict.confidenceScore)
        assertTrue(verdict.sourceResults.isEmpty())
    }

    @Test
    fun testScanCancellationMidFlight() = runTest {
        val mockEngine = MockEngine {
            delay(5000)
            respond("{}", HttpStatusCode.OK)
        }

        val client = HttpClient(mockEngine)
        val aggregator = ThreatIntelAggregator(client, timeoutMs = 10000L)
        val indicator = Indicator("8.8.8.8", "8.8.8.8", IndicatorType.IP_ADDRESS)
        val settings = AppSettings()

        val job = async {
            aggregator.aggregate(indicator, settings)
        }

        delay(100)
        job.cancelAndJoin()
        assertTrue(job.isCancelled, "Scan job must cancel cleanly on cancellation signal")
    }

    @Test
    fun testHistoryPersistenceRoundTripAndCorruptionRecovery() {
        class InMemoryStorage : KeyValueStorage {
            val map = mutableMapOf<String, Any>()
            override fun getBoolean(key: String, defaultValue: Boolean): Boolean = (map[key] as? Boolean) ?: defaultValue
            override fun setBoolean(key: String, value: Boolean) { map[key] = value }
            override fun getString(key: String, defaultValue: String): String = (map[key] as? String) ?: defaultValue
            override fun setString(key: String, value: String) { map[key] = value }
        }

        val storage = InMemoryStorage()
        val repo = SettingsRepositoryImpl(storage)

        // 1. Initial history is empty
        assertTrue(repo.getScanHistory().isEmpty())

        // 2. Save history items
        val indicator = Indicator("1.1.1.1", "1.1.1.1", IndicatorType.IP_ADDRESS)
        val verdict = TriageVerdict(
            indicator = indicator,
            overallVerdict = VerdictLevel.CLEAN,
            confidenceScore = 5,
            summaryDescription = "Clean IP",
            sourceResults = listOf(
                SourceResult("Cloudflare", VerdictLevel.CLEAN, 0, 1, "Clean", isRateLimited = false)
            )
        )
        repo.saveScanHistory(listOf(verdict))

        // 3. Read back
        val loaded = repo.getScanHistory()
        assertEquals(1, loaded.size)
        assertEquals("1.1.1.1", loaded[0].indicator.normalizedValue)
        assertEquals(VerdictLevel.CLEAN, loaded[0].overallVerdict)

        // 4. Corrupted storage recovery: If storage has bad JSON, it returns empty list instead of crashing
        storage.setString("key_scan_history_json", "{ broken: invalid json [}")
        val recovered = repo.getScanHistory()
        assertTrue(recovered.isEmpty(), "Corrupted JSON in storage must gracefully return emptyList()")

        // 5. Clear history
        repo.saveScanHistory(listOf(verdict))
        assertEquals(1, repo.getScanHistory().size)
        repo.clearScanHistory()
        assertTrue(repo.getScanHistory().isEmpty())
    }

    @Test
    fun testIpQualityScoreQuotaExceededFlagsRateLimited() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = """{"success": false, "message": "You have exceeded your request quota."}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders
            )
        }

        val client = HttpClient(mockEngine)
        val provider = IpQualityScoreProvider()
        val indicator = Indicator("8.8.8.8", "8.8.8.8", IndicatorType.IP_ADDRESS)
        val settings = AppSettings(ipQualityScoreApiKey = "test_key")

        val result = provider.query(client, indicator, settings)

        assertTrue(result.isRateLimited, "IPQS quota exceeded must set isRateLimited = true")
        assertTrue(result.summary.contains("Rate limit exceeded") || result.summary.contains("quota"))
        assertEquals(VerdictLevel.NO_DATA, result.verdict)
    }

    @Test
    fun testPulsediveRateLimitErrorFlagsRateLimited() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = """{"error": "API rate limit reached"}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders
            )
        }

        val client = HttpClient(mockEngine)
        val provider = PulsediveProvider()
        val indicator = Indicator("evil.com", "evil.com", IndicatorType.DOMAIN)
        val settings = AppSettings(pulsediveApiKey = "test_pulse_key")

        val result = provider.query(client, indicator, settings)

        assertTrue(result.isRateLimited, "Pulsedive rate limit response must set isRateLimited = true")
        assertTrue(result.summary.contains("Rate limit exceeded") || result.summary.contains("rate limit"))
        assertEquals(VerdictLevel.NO_DATA, result.verdict)
    }

    @Test
    fun testPrivacyGuardThreatConfidenceScoreIsZero() = runTest {
        val mockEngine = MockEngine {
            respond("{}", HttpStatusCode.OK, jsonHeaders)
        }

        val client = HttpClient(mockEngine)
        val aggregator = ThreatIntelAggregator(client)
        val localIndicator = Indicator("127.0.0.1", "127.0.0.1", IndicatorType.IP_ADDRESS, isInternalOrPrivate = true)
        val settings = AppSettings(privacyGuardEnabled = true)

        val verdict = aggregator.aggregate(localIndicator, settings)

        assertEquals(VerdictLevel.CLEAN, verdict.overallVerdict)
        assertEquals(0, verdict.confidenceScore, "Safe private IP must have 0% threat confidence score, NOT 100%")
        assertTrue(verdict.summaryDescription.contains("Privacy Guard Activated"))
    }
}

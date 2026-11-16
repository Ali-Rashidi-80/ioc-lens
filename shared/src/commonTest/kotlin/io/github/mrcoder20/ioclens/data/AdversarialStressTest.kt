package io.github.mrcoder20.ioclens.data

import io.github.mrcoder20.ioclens.data.remote.AlienVaultOtxProvider
import io.github.mrcoder20.ioclens.data.remote.CloudflareDnsProvider
import io.github.mrcoder20.ioclens.data.remote.CrtShProvider
import io.github.mrcoder20.ioclens.data.remote.DEFAULT_ZERO_KEY_PROVIDERS
import io.github.mrcoder20.ioclens.data.remote.FeodoTrackerProvider
import io.github.mrcoder20.ioclens.data.remote.MalwareBazaarProvider
import io.github.mrcoder20.ioclens.data.remote.RdapProvider
import io.github.mrcoder20.ioclens.data.remote.RipeStatProvider
import io.github.mrcoder20.ioclens.data.remote.ShodanInternetDbProvider
import io.github.mrcoder20.ioclens.data.remote.ThreatFoxProvider
import io.github.mrcoder20.ioclens.data.remote.ThreatIntelAggregator
import io.github.mrcoder20.ioclens.data.remote.ThreatIntelProvider
import io.github.mrcoder20.ioclens.data.remote.UrlhausProvider
import io.github.mrcoder20.ioclens.data.repository.ZeroKeyThreatIntelRepositoryImpl
import io.github.mrcoder20.ioclens.domain.model.AppSettings
import io.github.mrcoder20.ioclens.domain.model.Indicator
import io.github.mrcoder20.ioclens.domain.model.IndicatorType
import io.github.mrcoder20.ioclens.domain.model.SourceResult
import io.github.mrcoder20.ioclens.domain.model.VerdictLevel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Adversarial Empirical Stress Suite for Milestone 1:
 * Validates Zero-Key engines resilience against HTTP 429 rate limiting,
 * hung providers with strict timeouts, and corrupted/HTML FeodoTracker payloads.
 */
class AdversarialStressTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private val htmlHeaders = headersOf(HttpHeaders.ContentType, "text/html")

    // =========================================================================
    // 1. Rate-Limiting Resilience: HTTP 429 on all 10 Zero-Key Engines
    // =========================================================================

    private val rateLimitedClient = HttpClient(MockEngine { _ ->
        respond(
            content = """{"error": "Too Many Requests", "message": "Rate limit exceeded. Try again in 60 seconds."}""",
            status = HttpStatusCode.TooManyRequests,
            headers = jsonHeaders
        )
    })

    @Test
    fun testAllTenZeroKeyEnginesGracefullyHandleHttp429() = runTest {
        val testIndicators = mapOf(
            IndicatorType.URL to Indicator("https://suspicious.example.com/login", "https://suspicious.example.com/login", IndicatorType.URL),
            IndicatorType.DOMAIN to Indicator("suspicious.example.com", "suspicious.example.com", IndicatorType.DOMAIN),
            IndicatorType.IP_ADDRESS to Indicator("198.51.100.25", "198.51.100.25", IndicatorType.IP_ADDRESS),
            IndicatorType.FILE_HASH to Indicator("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", IndicatorType.FILE_HASH)
        )

        for (provider in DEFAULT_ZERO_KEY_PROVIDERS) {
            val matchingIndicator = testIndicators.values.firstOrNull { provider.isApplicable(it) }
            assertNotNull(matchingIndicator, "Provider ${provider.name} should have at least one applicable indicator type")

            val result = provider.query(rateLimitedClient, matchingIndicator, AppSettings())

            assertEquals(
                VerdictLevel.NO_DATA,
                result.verdict,
                "Provider ${provider.name} must return NO_DATA when HTTP 429 rate-limited"
            )
            assertTrue(
                result.isRateLimited,
                "Provider ${provider.name} must set isRateLimited = true on HTTP 429"
            )
            assertTrue(
                result.summary.contains("429") || result.summary.contains("Rate limit", ignoreCase = true),
                "Provider ${provider.name} summary should mention rate limiting: ${result.summary}"
            )
            assertEquals(
                0,
                result.detectionsCount,
                "Provider ${provider.name} detectionsCount must be 0 on rate limit"
            )
        }
    }

    @Test
    fun testUrlhausApplicationLevelRateLimitPayload() = runTest {
        val client = HttpClient(MockEngine { _ ->
            respond(
                content = """{"query_status": "rate_limited", "message": "Your IP has exceeded the allowed query limit"}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders
            )
        })

        val provider = UrlhausProvider()
        val indicator = Indicator("https://example.com/file.exe", "https://example.com/file.exe", IndicatorType.URL)
        val result = provider.query(client, indicator, AppSettings())

        assertEquals(VerdictLevel.NO_DATA, result.verdict)
        assertTrue(result.isRateLimited)
        assertTrue(result.summary.contains("Rate limit", ignoreCase = true))
    }

    @Test
    fun testRepositoryAnalyzeUnderTotalRateLimiting() = runTest {
        val repository = ZeroKeyThreatIntelRepositoryImpl(
            client = rateLimitedClient,
            aggregator = ThreatIntelAggregator(
                client = rateLimitedClient,
                providers = DEFAULT_ZERO_KEY_PROVIDERS
            )
        )

        val progressUpdates = mutableListOf<Pair<Int, Int>>()
        val indicator = Indicator("198.51.100.50", "198.51.100.50", IndicatorType.IP_ADDRESS)

        val verdict = repository.analyze(indicator) { completed, total ->
            progressUpdates.add(completed to total)
        }

        // Triage verdict under total rate-limiting
        assertEquals(VerdictLevel.NO_DATA, verdict.overallVerdict)
        assertEquals(0, verdict.confidenceScore)
        assertTrue(verdict.sourceResults.isNotEmpty())
        assertTrue(
            verdict.sourceResults.all { it.isRateLimited || it.summary.contains("timed out", ignoreCase = true) },
            "All queried providers under HTTP 429 must report isRateLimited = true or timed out"
        )
        assertTrue(
            verdict.sourceResults.all { it.verdict == VerdictLevel.NO_DATA },
            "All queried providers under HTTP 429 must report verdict == NO_DATA"
        )
        // Progress should cleanly reach completion
        assertTrue(progressUpdates.toList().any { it.first == it.second && it.second > 0 })
    }

    // =========================================================================
    // 2. Network Timeout & Hung Provider Handling
    // =========================================================================

    @Test
    fun testHungProviderTimesOutAndRemainingProvidersCompleteTriage() = runTest {
        val hungProvider = object : ThreatIntelProvider {
            override val name = "Hung ZeroKey Provider"
            override val isZeroKey = true
            override fun isApplicable(indicator: Indicator): Boolean = true
            override suspend fun query(client: HttpClient, indicator: Indicator, settings: AppSettings): SourceResult {
                delay(20_000L) // Exceeds timeoutMs by 20x
                return SourceResult(name, VerdictLevel.MALICIOUS, 1, 1, "Should never be reached")
            }
        }

        val maliciousProvider = object : ThreatIntelProvider {
            override val name = "Fast Malicious Provider"
            override val isZeroKey = true
            override fun isApplicable(indicator: Indicator): Boolean = true
            override suspend fun query(client: HttpClient, indicator: Indicator, settings: AppSettings): SourceResult {
                delay(50L) // Completes quickly
                return SourceResult(name, VerdictLevel.MALICIOUS, 1, 1, "Confirmed malicious C2")
            }
        }

        val cleanProvider = object : ThreatIntelProvider {
            override val name = "Fast Clean Provider"
            override val isZeroKey = true
            override fun isApplicable(indicator: Indicator): Boolean = true
            override suspend fun query(client: HttpClient, indicator: Indicator, settings: AppSettings): SourceResult {
                delay(30L) // Completes quickly
                return SourceResult(name, VerdictLevel.CLEAN, 0, 1, "Clean telemetry")
            }
        }

        val aggregator = ThreatIntelAggregator(
            client = HttpClient(MockEngine { respond("OK") }),
            timeoutMs = 1_000L,
            providers = listOf(hungProvider, maliciousProvider, cleanProvider)
        )

        val indicator = Indicator("203.0.113.10", "203.0.113.10", IndicatorType.IP_ADDRESS)
        val progress = mutableListOf<Pair<Int, Int>>()

        val verdict = aggregator.aggregate(indicator, AppSettings()) { completed, total ->
            progress.add(completed to total)
        }

        // 1. Hung provider is safely skipped with NO_DATA
        val hungResult = verdict.sourceResults.find { it.providerName == "Hung ZeroKey Provider" }
        assertNotNull(hungResult)
        assertEquals(VerdictLevel.NO_DATA, hungResult.verdict)
        assertTrue(hungResult.summary.contains("Connection timed out after 1000ms (Provider skipped)"))

        // 2. Fast providers successfully executed and were synthesized
        val maliciousResult = verdict.sourceResults.find { it.providerName == "Fast Malicious Provider" }
        assertNotNull(maliciousResult)
        assertEquals(VerdictLevel.MALICIOUS, maliciousResult.verdict)

        val cleanResult = verdict.sourceResults.find { it.providerName == "Fast Clean Provider" }
        assertNotNull(cleanResult)
        assertEquals(VerdictLevel.CLEAN, cleanResult.verdict)

        // 3. Overall verdict correctly synthesized despite the timeout
        assertEquals(VerdictLevel.MALICIOUS, verdict.overallVerdict)
        assertTrue(verdict.confidenceScore >= 70)
        assertTrue(verdict.summaryDescription.contains("Fast Malicious Provider"))

        // 4. Progress cleanly finished 3/3
        assertEquals(3 to 3, progress.last())
    }

    @Test
    fun testRepositoryWithHungZeroKeyEngineDoesNotBlockTriage() = runTest {
        val hungEngine = object : ThreatIntelProvider {
            override val name = "Lagging Threat Engine"
            override val isZeroKey = true
            override fun isApplicable(indicator: Indicator): Boolean = true
            override suspend fun query(client: HttpClient, indicator: Indicator, settings: AppSettings): SourceResult {
                delay(20_000L)
                return SourceResult(name, VerdictLevel.MALICIOUS, 1, 1, "Late result")
            }
        }

        val fastEngine = object : ThreatIntelProvider {
            override val name = "Swift Clean Engine"
            override val isZeroKey = true
            override fun isApplicable(indicator: Indicator): Boolean = true
            override suspend fun query(client: HttpClient, indicator: Indicator, settings: AppSettings): SourceResult {
                return SourceResult(name, VerdictLevel.CLEAN, 0, 1, "Clean record")
            }
        }

        val aggregator = ThreatIntelAggregator(
            client = HttpClient(MockEngine { respond("OK") }),
            timeoutMs = 500L,
            providers = listOf(hungEngine, fastEngine)
        )
        val repository = ZeroKeyThreatIntelRepositoryImpl(
            client = HttpClient(MockEngine { respond("OK") }),
            aggregator = aggregator
        )

        val indicator = Indicator("198.51.100.99", "198.51.100.99", IndicatorType.IP_ADDRESS)
        val verdict = repository.analyze(indicator)

        assertEquals(VerdictLevel.CLEAN, verdict.overallVerdict)
        assertEquals(2, verdict.sourceResults.size)
        val timedOutSource = verdict.sourceResults.first { it.providerName == "Lagging Threat Engine" }
        assertEquals(VerdictLevel.NO_DATA, timedOutSource.verdict)
        assertTrue(timedOutSource.summary.contains("timed out"))
    }

    // =========================================================================
    // 3. FeodoTrackerProvider: Corrupt, Truncated, & HTML Payloads
    // =========================================================================

    @Test
    fun testFeodoTrackerHttp502BadGatewayHtmlFallback() = runTest {
        val client = HttpClient(MockEngine { _ ->
            respond(
                content = """
                    <html>
                    <head><title>502 Bad Gateway</title></head>
                    <body bgcolor="white">
                    <center><h1>502 Bad Gateway</h1></center>
                    <hr><center>nginx/1.18.0 (Ubuntu)</center>
                    </body>
                    </html>
                """.trimIndent(),
                status = HttpStatusCode.BadGateway,
                headers = htmlHeaders
            )
        })

        val provider = FeodoTrackerProvider()
        val indicator = Indicator("198.51.100.1", "198.51.100.1", IndicatorType.IP_ADDRESS)
        val result = provider.query(client, indicator, AppSettings())

        assertEquals(VerdictLevel.NO_DATA, result.verdict)
        assertEquals(0, result.detectionsCount)
        assertTrue(result.summary.contains("502") || result.summary.contains("Bad Gateway"))
        assertFalse(result.isRateLimited)
    }

    @Test
    fun testFeodoTrackerHttp200WithDdosHtmlInterstitialPage() = runTest {
        val client = HttpClient(MockEngine { _ ->
            respond(
                content = """
                    <!DOCTYPE html>
                    <html lang="en">
                    <head><title>Attention Required! | Cloudflare</title></head>
                    <body>
                    <h2>Please verify you are a human</h2>
                    <div id="challenge-running">Checking browser...</div>
                    </body>
                    </html>
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = htmlHeaders
            )
        })

        val provider = FeodoTrackerProvider()
        val indicator = Indicator("198.51.100.1", "198.51.100.1", IndicatorType.IP_ADDRESS)
        val result = provider.query(client, indicator, AppSettings())

        assertEquals(VerdictLevel.NO_DATA, result.verdict)
        assertEquals(0, result.detectionsCount)
        assertTrue(result.summary.contains("Invalid or corrupted Feodo Tracker response payload"))
    }

    @Test
    fun testFeodoTrackerTruncatedJsonPayload() = runTest {
        val client = HttpClient(MockEngine { _ ->
            respond(
                content = """[{"ip_address": "198.51.100.1", "malware": "Qakb""", // Truncated mid-stream
                status = HttpStatusCode.OK,
                headers = jsonHeaders
            )
        })

        val provider = FeodoTrackerProvider()
        val indicator = Indicator("198.51.100.1", "198.51.100.1", IndicatorType.IP_ADDRESS)
        val result = provider.query(client, indicator, AppSettings())

        assertEquals(VerdictLevel.NO_DATA, result.verdict)
        assertEquals(0, result.detectionsCount)
        assertTrue(result.summary.contains("Invalid or corrupted Feodo Tracker response payload"))
    }

    @Test
    fun testFeodoTrackerUnexpectedJsonObjectInsteadOfArray() = runTest {
        val client = HttpClient(MockEngine { _ ->
            respond(
                content = """{"status": "error", "message": "Database maintenance in progress"}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders
            )
        })

        val provider = FeodoTrackerProvider()
        val indicator = Indicator("198.51.100.1", "198.51.100.1", IndicatorType.IP_ADDRESS)
        val result = provider.query(client, indicator, AppSettings())

        assertEquals(VerdictLevel.NO_DATA, result.verdict)
        assertTrue(result.summary.contains("Invalid or corrupted Feodo Tracker response payload"))
    }

    @Test
    fun testFeodoTrackerMalformedHeterogeneousArray() = runTest {
        // FeodoTracker handles heterogeneous arrays: skips malformed entries and matches valid IP
        val client = HttpClient(MockEngine { _ ->
            respond(
                content = """
                    [
                        null,
                        42,
                        "unexpected_string",
                        {},
                        {"ip_address": "198.51.100.77", "malware": "Emotet", "status": "active", "port": 443}
                    ]
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = jsonHeaders
            )
        })

        val provider = FeodoTrackerProvider()
        val matchedIndicator = Indicator("198.51.100.77", "198.51.100.77", IndicatorType.IP_ADDRESS)
        val matchedResult = provider.query(client, matchedIndicator, AppSettings())

        assertEquals(VerdictLevel.MALICIOUS, matchedResult.verdict)
        assertEquals(1, matchedResult.detectionsCount)
        assertTrue(matchedResult.summary.contains("Emotet"))
        assertTrue(matchedResult.summary.contains("443"))

        // Unlisted IP in the same heterogeneous array returns CLEAN
        val unlistedIndicator = Indicator("198.51.100.99", "198.51.100.99", IndicatorType.IP_ADDRESS)
        val cleanResult = provider.query(client, unlistedIndicator, AppSettings())
        assertEquals(VerdictLevel.CLEAN, cleanResult.verdict)
        assertEquals(0, cleanResult.detectionsCount)
    }

    @Test
    fun testFeodoTrackerNetworkDropExceptionHandledGracefully() = runTest {
        val client = HttpClient(MockEngine { _ ->
            throw RuntimeException("Network connection reset by peer")
        })

        val provider = FeodoTrackerProvider()
        val indicator = Indicator("198.51.100.1", "198.51.100.1", IndicatorType.IP_ADDRESS)
        val result = provider.query(client, indicator, AppSettings())

        assertEquals(VerdictLevel.NO_DATA, result.verdict)
        assertTrue(result.summary.contains("Feodo Tracker unavailable: Network connection reset by peer"))
    }

    // =========================================================================
    // 4. Concurrency Stress Test: Rapid Burst Execution
    // =========================================================================

    @Test
    fun testHighConcurrencyStressBurst() = runTest {
        val mockEngine = MockEngine { request ->
            delay(10)
            when {
                request.url.toString().contains("feodotracker") -> {
                    respond(
                        content = """[{"ip_address": "198.51.100.10", "malware": "Dridex", "status": "active"}]""",
                        status = HttpStatusCode.OK,
                        headers = jsonHeaders
                    )
                }
                else -> {
                    respond(content = "{}", status = HttpStatusCode.OK, headers = jsonHeaders)
                }
            }
        }

        val client = HttpClient(mockEngine)
        val repository = ZeroKeyThreatIntelRepositoryImpl(
            client = client,
            aggregator = ThreatIntelAggregator(client = client, providers = DEFAULT_ZERO_KEY_PROVIDERS)
        )

        // Launch 20 simultaneous triage queries across coroutines
        val tasks = (1..20).map { i ->
            async {
                val ip = if (i % 2 == 0) "198.51.100.10" else "198.51.100.99"
                val indicator = Indicator(ip, ip, IndicatorType.IP_ADDRESS)
                repository.analyze(indicator)
            }
        }

        val results = tasks.awaitAll()
        assertEquals(20, results.size)
        // All odd indices should have clean/no_data, even indices should detect Dridex from FeodoTracker
        for ((index, verdict) in results.withIndex()) {
            val i = index + 1
            if (i % 2 == 0) {
                assertEquals(VerdictLevel.MALICIOUS, verdict.overallVerdict)
                assertTrue(verdict.sourceResults.any { it.providerName.contains("Feodo") && it.verdict == VerdictLevel.MALICIOUS })
            }
        }
    }
}

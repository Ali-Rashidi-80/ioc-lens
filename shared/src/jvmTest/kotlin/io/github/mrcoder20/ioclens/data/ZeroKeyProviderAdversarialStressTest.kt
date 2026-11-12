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
import io.github.mrcoder20.ioclens.domain.model.VerdictLevel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import java.util.Collections
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Empirical Challenger Stress Harness for M1-2:
 * Rigorously attacks all 10 Zero-Key providers with:
 * 1. HTTP 429 Too Many Requests
 * 2. HTTP 500 Internal Server Error
 * 3. HTTP 200 OK with HTML bodies (Cloudflare challenge / WAF / Nginx error pages)
 * 4. HTTP 200 OK with truncated / malformed JSON
 * 5. Concurrent burst load under total rate limiting (50 simultaneous queries)
 */
class ZeroKeyProviderAdversarialStressTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private val htmlHeaders = headersOf(HttpHeaders.ContentType, "text/html")

    private val sampleIndicators = mapOf(
        IndicatorType.URL to Indicator("https://example.com/login", "https://example.com/login", IndicatorType.URL),
        IndicatorType.DOMAIN to Indicator("example.com", "example.com", IndicatorType.DOMAIN),
        IndicatorType.IP_ADDRESS to Indicator("198.51.100.42", "198.51.100.42", IndicatorType.IP_ADDRESS),
        IndicatorType.FILE_HASH to Indicator("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", IndicatorType.FILE_HASH)
    )

    private fun getApplicableIndicator(provider: ThreatIntelProvider): Indicator {
        val indicator = sampleIndicators.values.firstOrNull { provider.isApplicable(it) }
        assertNotNull(indicator, "Provider ${provider.name} must match at least one test indicator")
        return indicator
    }

    // =========================================================================
    // 1. Comprehensive HTTP 429 Across All 10 Providers
    // =========================================================================
    @Test
    fun testAllTenProvidersHandleHttp429Safely() = runTest {
        val client = HttpClient(MockEngine {
            respond(
                content = """{"error": "Too Many Requests", "message": "Rate limit reached"}""",
                status = HttpStatusCode.TooManyRequests,
                headers = jsonHeaders
            )
        })

        for (provider in DEFAULT_ZERO_KEY_PROVIDERS) {
            val indicator = getApplicableIndicator(provider)
            val result = provider.query(client, indicator, AppSettings())

            assertEquals(VerdictLevel.NO_DATA, result.verdict, "Provider ${provider.name} should return NO_DATA on HTTP 429")
            assertTrue(result.isRateLimited, "Provider ${provider.name} should flag isRateLimited = true on HTTP 429")
            assertEquals(0, result.detectionsCount, "Provider ${provider.name} detectionsCount must be 0 on HTTP 429")
        }
    }

    // =========================================================================
    // 2. Comprehensive HTTP 500 Across All 10 Providers
    // =========================================================================
    @Test
    fun testAllTenProvidersHandleHttp500InternalServerError() = runTest {
        val client = HttpClient(MockEngine {
            respond(
                content = """{"error": "Internal Server Error", "code": 500}""",
                status = HttpStatusCode.InternalServerError,
                headers = jsonHeaders
            )
        })

        for (provider in DEFAULT_ZERO_KEY_PROVIDERS) {
            val indicator = getApplicableIndicator(provider)
            val result = provider.query(client, indicator, AppSettings())

            assertEquals(VerdictLevel.NO_DATA, result.verdict, "Provider ${provider.name} should return NO_DATA on HTTP 500")
            assertTrue(
                result.summary.contains("500") || result.summary.contains("Server error", ignoreCase = true),
                "Provider ${provider.name} summary should mention HTTP 500: ${result.summary}"
            )
            assertEquals(0, result.detectionsCount, "Provider ${provider.name} detectionsCount must be 0 on HTTP 500")
        }
    }

    // =========================================================================
    // 3. HTTP 200 With HTML Interstitial / Captcha Bodies Across All 10 Providers
    // =========================================================================
    @Test
    fun testAllTenProvidersHandleHtmlBodiesAtStatus200() = runTest {
        val htmlPayload = """
            <!DOCTYPE html>
            <html>
            <head><title>Just a moment...</title></head>
            <body>
                <h1>Checking your browser before accessing the website.</h1>
                <p>This process is automatic. Your IP is 198.51.100.42.</p>
            </body>
            </html>
        """.trimIndent()

        val client = HttpClient(MockEngine {
            respond(
                content = htmlPayload,
                status = HttpStatusCode.OK,
                headers = htmlHeaders
            )
        })

        for (provider in DEFAULT_ZERO_KEY_PROVIDERS) {
            val indicator = getApplicableIndicator(provider)
            val result = provider.query(client, indicator, AppSettings())

            assertEquals(VerdictLevel.NO_DATA, result.verdict, "Provider ${provider.name} must return NO_DATA when response is HTML")
            assertEquals(0, result.detectionsCount, "Provider ${provider.name} detectionsCount must be 0 when response is HTML")
        }
    }

    // =========================================================================
    // 4. HTTP 200 With Truncated / Malformed JSON Across All 10 Providers
    // =========================================================================
    @Test
    fun testAllTenProvidersHandleMalformedOrTruncatedJson() = runTest {
        val malformedJson = """{"status": "ok", "data": [{"threat_type": "botnet", "incomp"""

        val client = HttpClient(MockEngine {
            respond(
                content = malformedJson,
                status = HttpStatusCode.OK,
                headers = jsonHeaders
            )
        })

        for (provider in DEFAULT_ZERO_KEY_PROVIDERS) {
            val indicator = getApplicableIndicator(provider)
            val result = provider.query(client, indicator, AppSettings())

            assertEquals(VerdictLevel.NO_DATA, result.verdict, "Provider ${provider.name} must return NO_DATA on malformed JSON")
            assertEquals(0, result.detectionsCount, "Provider ${provider.name} detectionsCount must be 0 on malformed JSON")
        }
    }

    // =========================================================================
    // 5. Heavy Concurrent Load (50 Calls) Under Total Rate Limiting
    // =========================================================================
    @Test
    fun testHeavyConcurrentLoadUnderTotalRateLimiting() = runTest {
        val rateLimitedClient = HttpClient(MockEngine {
            respond(
                content = """{"error": "Too Many Requests", "retry_after": 60}""",
                status = HttpStatusCode.TooManyRequests,
                headers = jsonHeaders
            )
        })

        val aggregator = ThreatIntelAggregator(
            client = rateLimitedClient,
            providers = DEFAULT_ZERO_KEY_PROVIDERS
        )
        val repository = ZeroKeyThreatIntelRepositoryImpl(
            client = rateLimitedClient,
            aggregator = aggregator
        )

        val progressLog = Collections.synchronizedList(mutableListOf<Pair<Int, Int>>())
        val concurrentTasks = (1..50).map { i ->
            async {
                val ip = "198.51.100.${(i % 100) + 1}"
                val indicator = Indicator(ip, ip, IndicatorType.IP_ADDRESS)
                repository.analyze(indicator) { completed, total ->
                    progressLog.add(completed to total)
                }
            }
        }

        val results = concurrentTasks.awaitAll()
        assertEquals(50, results.size, "All 50 concurrent analysis requests must finish")

        for (verdict in results) {
            assertEquals(VerdictLevel.NO_DATA, verdict.overallVerdict, "All rate-limited triage verdicts must be NO_DATA")
            assertEquals(0, verdict.confidenceScore, "Confidence score must be 0 on complete rate limiting")
            assertTrue(verdict.sourceResults.isNotEmpty(), "Source results must be populated")
            assertTrue(
                verdict.sourceResults.all { it.isRateLimited || it.verdict == VerdictLevel.NO_DATA },
                "Every provider source result must be rate-limited or NO_DATA"
            )
        }

        // Verify that progress events were received and reached completion
        assertTrue(progressLog.any { it.first == it.second && it.second > 0 }, "Progress updates must include completion events")
    }
}

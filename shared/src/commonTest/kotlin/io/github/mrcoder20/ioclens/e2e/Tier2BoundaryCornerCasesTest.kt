package io.github.mrcoder20.ioclens.e2e

import io.github.mrcoder20.ioclens.core.localization.AppLanguage
import io.github.mrcoder20.ioclens.core.localization.LocalizationProvider
import io.github.mrcoder20.ioclens.core.storage.InMemoryKeyValueStorage
import io.github.mrcoder20.ioclens.data.remote.UrlhausProvider
import io.github.mrcoder20.ioclens.data.remote.handleHttpError
import io.github.mrcoder20.ioclens.data.remote.safeParseJsonObject
import io.github.mrcoder20.ioclens.data.remote.ThreatIntelAggregator
import io.github.mrcoder20.ioclens.data.repository.SettingsRepositoryImpl
import io.github.mrcoder20.ioclens.data.repository.ThreatIntelRepositoryImpl
import io.github.mrcoder20.ioclens.domain.model.AppSettings
import io.github.mrcoder20.ioclens.domain.model.Indicator
import io.github.mrcoder20.ioclens.domain.model.IndicatorType
import io.github.mrcoder20.ioclens.domain.model.ParsedIoc
import io.github.mrcoder20.ioclens.domain.model.VerdictLevel
import io.github.mrcoder20.ioclens.domain.parser.IOCParser
import io.github.mrcoder20.ioclens.domain.usecase.AnalyzeIndicatorUseCase
import io.github.mrcoder20.ioclens.domain.usecase.SetIntroCompletedUseCase
import io.github.mrcoder20.ioclens.presentation.home.HomeEvent
import io.github.mrcoder20.ioclens.presentation.home.HomeViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * TIER 2: Boundary & Corner Cases (Opaque-Box E2E Test Suite)
 *
 * Verifies system resilience and precision against edge conditions:
 * 1. Empty, blank, and zero-width invisible character inputs.
 * 2. Length boundaries (RFC 1035 domains, max URL lengths, max non-URL token bounds).
 * 3. Invalid IPv4 addresses and port edge bounds (1..65535).
 * 4. IPv6 edge cases (bracketed notation, embedded IPv4-mapped v6, link-local/unique-local).
 * 5. Upstream HTTP 429 rate limit backoff and error status codes (500, 502, 503, 401).
 * 6. Corrupted/malformed upstream responses (HTML error bodies, invalid JSON payloads).
 * 7. Defang/refang normalization and special character escaping.
 * 8. Empty input handling in ViewModel search console.
 */
class Tier2BoundaryCornerCasesTest {

    private val parser = IOCParser()
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    // ---------------------------------------------------------------------------------------------
    // 1. EMPTY, BLANK & INVISIBLE ZERO-WIDTH CHARACTERS
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testEmptyAndWhitespaceInputParsing() {
        val emptyResult = parser.parse("")
        assertTrue(emptyResult is ParsedIoc.Unknown)

        val whitespaceResult = parser.parse("    \t\n   ")
        assertTrue(whitespaceResult is ParsedIoc.Unknown)

        // Zero-width characters (ZWSP, ZWNJ, ZWJ, BOM, LRM, RLM, NBSP) stripped
        val contaminated = "\u200B1.1.1.1\u200C"
        val parsed = parser.parse(contaminated)
        assertTrue(parsed is ParsedIoc.IpAddress)
        assertEquals("1.1.1.1", parsed.normalizedValue)
    }

    @Test
    fun testEmptyQueryInHomeViewModelProducesLocalizedError() = runTest {
        val storage = InMemoryKeyValueStorage()
        val settingsRepo = SettingsRepositoryImpl(storage)
        val mockAggregator = ThreatIntelAggregator(HttpClient(MockEngine { respond("{}", HttpStatusCode.OK, jsonHeaders) }))
        val threatRepo = ThreatIntelRepositoryImpl(settingsRepo, mockAggregator)
        val analyzeUseCase = AnalyzeIndicatorUseCase(threatRepo, parser)
        val introUseCase = SetIntroCompletedUseCase(settingsRepo)
        val viewModel = HomeViewModel(analyzeUseCase, introUseCase, settingsRepo)

        // Submit blank input
        viewModel.onEvent(HomeEvent.QueryChanged("   "))
        viewModel.onEvent(HomeEvent.AnalyzeClicked)

        val expectedError = LocalizationProvider.getStrings(AppLanguage.ENGLISH).enterIndicatorError
        assertEquals(expectedError, viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.activeVerdict)
    }

    // ---------------------------------------------------------------------------------------------
    // 2. EXTREME LENGTH BOUNDARIES
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testExtremeLengthInputsSafelyRejected() {
        // Non-URL input exceeding 512 characters
        val hugeNonUrl = "A".repeat(513)
        val parsedHuge = parser.parse(hugeNonUrl)
        assertTrue(parsedHuge is ParsedIoc.Unknown)

        // Domain exceeding 253 characters (RFC 1035 max)
        val hugeDomain = "sub." + "a".repeat(250) + ".com"
        val parsedDomain = parser.parse(hugeDomain)
        assertTrue(parsedDomain is ParsedIoc.Unknown)

        // URL exceeding 8192 characters
        val hugeUrl = "https://example.com/path?" + "param=".repeat(1500)
        assertTrue(hugeUrl.length > 8192)
        val parsedUrl = parser.parse(hugeUrl)
        assertTrue(parsedUrl is ParsedIoc.Unknown)
    }

    // ---------------------------------------------------------------------------------------------
    // 3. MALFORMED IPV4 & PORT BOUNDARIES
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testMalformedIpv4AddressesRejected() {
        val invalidIps = listOf(
            "256.0.0.1",          // Octet > 255
            "1.1.1.300",          // Octet > 255
            "1.2.3",              // Too few octets
            "1.2.3.4.5",          // Too many octets
            "1.1.1.1:65536",      // Port out of range (> 65535)
            "1.1.1.1:0",          // Port 0 invalid
            "1.1.1.1:abcd"        // Non-numeric port
        )

        for (invalid in invalidIps) {
            val parsed = parser.parse(invalid)
            assertFalse(
                parsed is ParsedIoc.IpAddress,
                "Input '$invalid' must not be parsed as valid IPv4"
            )
        }
    }

    @Test
    fun testValidIpv4PortBoundariesAccepted() {
        val parsedMinPort = parser.parse("1.1.1.1:1")
        assertTrue(parsedMinPort is ParsedIoc.IpAddress)
        assertEquals("1.1.1.1", parsedMinPort.normalizedValue)

        val parsedMaxPort = parser.parse("1.1.1.1:65535")
        assertTrue(parsedMaxPort is ParsedIoc.IpAddress)
        assertEquals("1.1.1.1", parsedMaxPort.normalizedValue)
    }

    // ---------------------------------------------------------------------------------------------
    // 4. IPV6 EDGE CASES & EMBEDDED NOTATION
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testIpv6EdgeCasesAndBracketedPorts() {
        // Valid bracketed loopback with port
        val parsedBracketed = parser.parse("[::1]:8080")
        assertTrue(parsedBracketed is ParsedIoc.IpAddress)
        assertTrue(parsedBracketed.isIpv6)
        assertTrue(parsedBracketed.isPrivate)
        assertEquals("::1", parsedBracketed.normalizedValue)

        // IPv4-mapped IPv6 for private subnet (::ffff:192.168.1.1)
        val parsedMappedPrivate = parser.parse("::ffff:192.168.1.1")
        assertTrue(parsedMappedPrivate is ParsedIoc.IpAddress)
        assertTrue(parsedMappedPrivate.isPrivate)

        // IPv4-mapped IPv6 for public IP (::ffff:8.8.8.8)
        val parsedMappedPublic = parser.parse("::ffff:8.8.8.8")
        assertTrue(parsedMappedPublic is ParsedIoc.IpAddress)
        assertFalse(parsedMappedPublic.isPrivate)

        // Link-local IPv6 (fe80::1)
        val parsedLinkLocal = parser.parse("fe80::1")
        assertTrue(parsedLinkLocal is ParsedIoc.IpAddress)
        assertTrue(parsedLinkLocal.isPrivate)

        // Unique-local IPv6 (fc00::1 / fd00::1)
        val parsedUniqueLocal = parser.parse("fc00::1")
        assertTrue(parsedUniqueLocal is ParsedIoc.IpAddress)
        assertTrue(parsedUniqueLocal.isPrivate)

        // Invalid IPv6 with multiple double colons
        val invalidMultipleDouble = parser.parse("2001::db8::1")
        assertFalse(invalidMultipleDouble is ParsedIoc.IpAddress)
    }

    // ---------------------------------------------------------------------------------------------
    // 5. UPSTREAM HTTP 429 RATE LIMITS & SERVER ERRORS
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testHttp429TooManyRequestsProducesRateLimitedStatus() {
        val result = handleHttpError("TestProvider", HttpStatusCode.TooManyRequests)
        assertTrue(result.isRateLimited)
        assertEquals(VerdictLevel.NO_DATA, result.verdict)
        assertTrue(result.summary.contains("429"))
        assertTrue(result.summary.contains("Rate limit exceeded", ignoreCase = true))
    }

    @Test
    fun testHttp5xxServerErrorsHandledGracefully() {
        val serverErrors = listOf(
            HttpStatusCode.InternalServerError,
            HttpStatusCode.BadGateway,
            HttpStatusCode.ServiceUnavailable,
            HttpStatusCode.GatewayTimeout
        )

        for (status in serverErrors) {
            val result = handleHttpError("ServerTestProvider", status)
            assertFalse(result.isRateLimited)
            assertEquals(VerdictLevel.NO_DATA, result.verdict)
            assertTrue(result.summary.contains("Server error", ignoreCase = true))
            assertTrue(result.summary.contains(status.value.toString()))
        }
    }

    @Test
    fun testHttp401And403AuthErrorsHandledGracefully() {
        for (status in listOf(HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden)) {
            val result = handleHttpError("AuthTestProvider", status)
            assertEquals(VerdictLevel.NO_DATA, result.verdict)
            assertTrue(result.summary.contains("Authentication failed", ignoreCase = true))
        }
    }

    // ---------------------------------------------------------------------------------------------
    // 6. CORRUPTED & MALFORMED PAYLOADS
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testSafeParseJsonObjectHandlesCorruptedAndHtmlPayloads() {
        // Empty string
        assertNull(safeParseJsonObject(""))

        // Whitespace
        assertNull(safeParseJsonObject("   "))

        // HTML 502/521 Error Page
        val htmlPage = "<!DOCTYPE html><html><head><title>502 Bad Gateway</title></head><body>Error</body></html>"
        assertNull(safeParseJsonObject(htmlPage))

        // Truncated JSON
        val truncatedJson = """{"query_status": "ok", "data": [{"id": """
        assertNull(safeParseJsonObject(truncatedJson))

        // Valid JSON
        val validJson = """{"query_status": "ok", "records": 1}"""
        val parsed = safeParseJsonObject(validJson)
        assertNotNull(parsed)
    }

    // ---------------------------------------------------------------------------------------------
    // 7. DEFANGING & REFANGING NORMALIZATION
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testDefangAndRefangNormalization() {
        // Bracketed dots
        assertEquals("1.1.1.1", parser.refang("1[.]1[.]1[.]1"))
        assertEquals("1.1.1.1", parser.refang("1(.)1(.)1(.)1"))

        // Obfuscated schemes
        assertEquals("https://malware.org/bad", parser.refang("hxxps://malware[.]org/bad"))
        assertEquals("http://malware.org/bad", parser.refang("hxxp://malware[.]org/bad"))
        assertEquals("ftp://downloads.com/file", parser.refang("fxp://downloads[.]com/file"))

        // Bracketed user-at
        assertEquals("attacker@c2server.com", parser.refang("attacker[@]c2server[.]com"))
        assertEquals("attacker@c2server.com", parser.refang("attacker[at]c2server[.]com"))
    }

    // ---------------------------------------------------------------------------------------------
    // 8. UNKNOWN INDICATOR AGGREGATION BEHAVIOR
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testUnknownIndicatorProducesNoDataWithoutEngines() = runTest {
        var networkCount = 0
        val mockEngine = MockEngine { request ->
            networkCount++
            respond("{}", HttpStatusCode.OK, jsonHeaders)
        }

        val client = HttpClient(mockEngine)
        val aggregator = ThreatIntelAggregator(client)
        val indicator = Indicator("this is just arbitrary text", "this is just arbitrary text", IndicatorType.UNKNOWN)

        val verdict = aggregator.aggregate(indicator, AppSettings())
        assertEquals(0, networkCount, "Unknown indicator must initiate 0 network requests")
        assertEquals(VerdictLevel.NO_DATA, verdict.overallVerdict)
        assertTrue(verdict.sourceResults.isEmpty())
        assertTrue(verdict.summaryDescription.contains("Unrecognized indicator format"))
    }
}

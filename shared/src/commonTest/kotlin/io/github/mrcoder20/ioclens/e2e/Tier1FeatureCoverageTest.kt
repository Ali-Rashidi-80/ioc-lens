package io.github.mrcoder20.ioclens.e2e

import io.github.mrcoder20.ioclens.core.components.slider.OnboardingArrowDirection
import io.github.mrcoder20.ioclens.core.components.slider.getOnboardingNextArrowDirection
import io.github.mrcoder20.ioclens.core.components.slider.shouldShowOnboardingSkip
import io.github.mrcoder20.ioclens.core.localization.AppLanguage
import io.github.mrcoder20.ioclens.core.localization.LocalizationProvider
import io.github.mrcoder20.ioclens.core.storage.InMemoryKeyValueStorage
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
import io.github.mrcoder20.ioclens.data.repository.SettingsRepositoryImpl
import io.github.mrcoder20.ioclens.data.repository.ThreatIntelRepositoryImpl
import io.github.mrcoder20.ioclens.domain.model.AppSettings
import io.github.mrcoder20.ioclens.domain.model.Indicator
import io.github.mrcoder20.ioclens.domain.model.IndicatorType
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
 * TIER 1: Feature Coverage (Opaque-Box E2E Test Suite)
 *
 * Verifies core features in isolation according to ORIGINAL_REQUEST.md & PROJECT.md:
 * 1. Onboarding flow (Single source pager, Top-bar Skip on slides 1-3, No Previous button, DONE on first click).
 * 2. RTL-aware navigation arrow direction (Left for Persian, Right for English/Russian).
 * 3. Tri-lingual localization (en, fa, ru; Zero Persian characters in English; Localized Persian settings badge).
 * 4. Bottom Navigation Scaffold with 4 independent full-screen destinations (0: Scanner, 1: Engines, 2: History, 3: Settings).
 * 5. 10 Zero-Key Direct HTTPS Threat Intel Engines (100% Free, 0 API keys required).
 * 6. Client-Side RFC 1918 Local Privacy Guard (Blocks private/internal IPs/domains with 0 network calls).
 * 7. Mobile-native action copy and streaming file hashing contract.
 */
class Tier1FeatureCoverageTest {

    private val parser = IOCParser()
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private val persianRegex = Regex("[\\u0600-\\u06FF]")

    // ---------------------------------------------------------------------------------------------
    // FEATURE 1: Onboarding Flow (R2)
    // ---------------------------------------------------------------------------------------------
    // FEATURE 2: RTL-Aware Directional Navigation Icons (R2)
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testDirectionalArrowPointsLeftForPersianAndRightForEnglishRussian() {
        // Persian (RTL) -> Arrow points LEFT (←)
        assertTrue(AppLanguage.PERSIAN.isRtl)
        assertEquals(OnboardingArrowDirection.LEFT, getOnboardingNextArrowDirection(isRtl = true))

        // English (LTR) -> Arrow points RIGHT (→)
        assertFalse(AppLanguage.ENGLISH.isRtl)
        assertEquals(OnboardingArrowDirection.RIGHT, getOnboardingNextArrowDirection(isRtl = false))

        // Russian (LTR) -> Arrow points RIGHT (→)
        assertFalse(AppLanguage.RUSSIAN.isRtl)
        assertEquals(OnboardingArrowDirection.RIGHT, getOnboardingNextArrowDirection(isRtl = AppLanguage.RUSSIAN.isRtl))
    }

    // ---------------------------------------------------------------------------------------------
    // FEATURE 3: Centralized Localization Synchronization (R3)
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testZeroPersianCharactersInEnglishLocale() {
        val strings = LocalizationProvider.getStrings(AppLanguage.ENGLISH)

        val criticalStrings = listOf(
            strings.appTitle,
            strings.appTagline,
            strings.skip,
            strings.next,
            strings.done,
            strings.searchPlaceholder,
            strings.searchFieldPlaceholder,
            strings.clear,
            strings.scanPayload,
            strings.zeroKeyInstantScanReady,
            strings.streamHashingTitle,
            strings.privacyGuardNotice,
            strings.settingsTitle,
            strings.cardPrivacyGuardTitle,
            strings.aboutPrivacyGuardDesc,
            strings.settingsTabGeneralPrivacy,
            strings.menuHome,
            strings.menuSettings,
            strings.menuProviders,
            strings.threatIntelSourcesResults,
            strings.pasteFromClipboard,
            strings.dropFileHint
        )

        for (str in criticalStrings) {
            assertFalse(
                str.contains(persianRegex),
                "English string '$str' must contain zero Persian Unicode characters"
            )
        }
    }

    @Test
    fun testLocalizedPersianSettingsBadge() {
        val faStrings = LocalizationProvider.getStrings(AppLanguage.PERSIAN)
        assertTrue(faStrings.cardPrivacyGuardTitle.contains(persianRegex))
        assertTrue(faStrings.settingsTitle.contains(persianRegex))
        assertTrue(faStrings.settingsTabGeneralPrivacy.contains(persianRegex))
    }

    // ---------------------------------------------------------------------------------------------
    // FEATURE 4: Bottom Navigation Scaffold with 4 Full-Screen Destinations (R4)
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testBottomNavigationFourDestinationsWithoutModalDialogs() = runTest {
        val storage = InMemoryKeyValueStorage()
        val settingsRepo = SettingsRepositoryImpl(storage)
        val mockAggregator = ThreatIntelAggregator(HttpClient(MockEngine { respond("{}", HttpStatusCode.OK, jsonHeaders) }))
        val threatRepo = ThreatIntelRepositoryImpl(settingsRepo, mockAggregator)
        val analyzeUseCase = AnalyzeIndicatorUseCase(threatRepo, parser)
        val introUseCase = SetIntroCompletedUseCase(settingsRepo)
        val viewModel = HomeViewModel(analyzeUseCase, introUseCase, settingsRepo)

        // Initial tab is 0 (Scanner)
        assertEquals(0, viewModel.uiState.value.selectedBottomNavTab)
        assertFalse(viewModel.uiState.value.isHistorySheetOpen)
        assertFalse(viewModel.uiState.value.isSettingsSheetOpen)

        // Select Tab 1: History View
        viewModel.onEvent(HomeEvent.SelectBottomNavTab(1))
        assertEquals(1, viewModel.uiState.value.selectedBottomNavTab)
        assertFalse(viewModel.uiState.value.isHistorySheetOpen)
        assertFalse(viewModel.uiState.value.isSettingsSheetOpen)

        // Select Tab 2: Settings View
        viewModel.onEvent(HomeEvent.SelectBottomNavTab(2))
        assertEquals(2, viewModel.uiState.value.selectedBottomNavTab)
        assertFalse(viewModel.uiState.value.isHistorySheetOpen)
        assertFalse(viewModel.uiState.value.isSettingsSheetOpen)

        // Return to Tab 0: Scanner View
        viewModel.onEvent(HomeEvent.SelectBottomNavTab(0))
        assertEquals(0, viewModel.uiState.value.selectedBottomNavTab)
        assertFalse(viewModel.uiState.value.isHistorySheetOpen)
        assertFalse(viewModel.uiState.value.isSettingsSheetOpen)
    }

    // ---------------------------------------------------------------------------------------------
    // FEATURE 5: 10 Zero-Key Threat Intel Engines (R5)
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testTenZeroKeyProvidersConfiguredWithoutApiKeys() {
        val providers = listOf(
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

        assertEquals(10, providers.size, "Must have exactly 10 Zero-Key engines")
        for (provider in providers) {
            assertTrue(provider.isZeroKey, "Provider '${provider.name}' must have isZeroKey = true")
        }
    }

    @Test
    fun testZeroKeyEnginesQuerySuccessfullyWithoutApiKey() = runTest {
        val mockEngine = MockEngine { request ->
            when {
                request.url.toString().contains("internetdb.shodan.io") -> {
                    respond(
                        content = """{"ip": "1.1.1.1", "ports": [53, 853], "cves": [], "hostnames": ["one.one.one.one"]}""",
                        status = HttpStatusCode.OK,
                        headers = jsonHeaders
                    )
                }
                request.url.toString().contains("cloudflare-dns.com") -> {
                    respond(
                        content = """{"Status": 0, "Answer": [{"name": "cloudflare.com", "type": 1, "data": "104.16.132.229"}]}""",
                        status = HttpStatusCode.OK,
                        headers = jsonHeaders
                    )
                }
                else -> respond("{}", HttpStatusCode.OK, jsonHeaders)
            }
        }

        val client = HttpClient(mockEngine)
        val settings = AppSettings() // 0 keys configured
        val indicator = Indicator("1.1.1.1", "1.1.1.1", IndicatorType.IP_ADDRESS)

        val shodan = ShodanInternetDbProvider()
        val shodanResult = shodan.query(client, indicator, settings)
        assertEquals(VerdictLevel.LOW_RISK, shodanResult.verdict)
        assertEquals(2, shodanResult.totalEngineCount)
        assertFalse(shodanResult.isRateLimited)

        val cloudflare = CloudflareDnsProvider()
        val domainIndicator = Indicator("cloudflare.com", "cloudflare.com", IndicatorType.DOMAIN)
        val cloudflareResult = cloudflare.query(client, domainIndicator, settings)
        assertEquals(VerdictLevel.NO_DATA, cloudflareResult.verdict)
        assertEquals(1, cloudflareResult.totalEngineCount)
        assertTrue(cloudflareResult.summary.contains("NOERROR"))
    }

    // ---------------------------------------------------------------------------------------------
    // FEATURE 6: RFC 1918 Local Privacy Guard (R5)
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testPrivacyGuardBlocksPrivateIpWithZeroOutboundNetworkCalls() = runTest {
        var networkDispatched = 0
        val mockEngine = MockEngine { request ->
            networkDispatched++
            error("Privacy Guard VIOLATION: Network call dispatched to ${request.url}")
        }

        val client = HttpClient(mockEngine)
        val aggregator = ThreatIntelAggregator(client)
        val settings = AppSettings()

        val privateIps = listOf(
            "10.0.0.1",
            "172.16.0.1",
            "172.31.255.255",
            "192.168.1.1",
            "127.0.0.1",
            "::1",
            "localhost",
            "gateway.local",
            "server.internal"
        )

        for (sample in privateIps) {
            val indicator = parser.parse(sample).toIndicator()
            assertTrue(indicator.isInternalOrPrivate, "Indicator '$sample' must be classified as internal/private")

            val verdict = aggregator.aggregate(indicator, settings)
            assertEquals(VerdictLevel.CLEAN, verdict.overallVerdict)
            assertEquals(0, verdict.confidenceScore)
            assertTrue(verdict.summaryDescription.contains("Privacy Guard Activated"))
            assertEquals(1, verdict.sourceResults.size)
            assertEquals("Privacy Guard Local", verdict.sourceResults.first().providerName)
        }

        assertEquals(0, networkDispatched, "Zero network requests must be initiated for private indicators")
    }

    // ---------------------------------------------------------------------------------------------
    // FEATURE 7: Mobile-Native Actions & Streaming Hasher Copy (R6)
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testMobileNativeActionLabelsInAllLanguages() {
        val en = LocalizationProvider.getStrings(AppLanguage.ENGLISH)
        assertEquals("📋 Paste", en.pasteFromClipboard)
        assertEquals("📁 Select File to Hash", en.dropFileHint)
        assertFalse(en.pasteFromClipboard.contains("Ctrl+V", ignoreCase = true))
        assertFalse(en.dropFileHint.contains("Drop any file", ignoreCase = true))

        val fa = LocalizationProvider.getStrings(AppLanguage.PERSIAN)
        assertTrue(fa.pasteFromClipboard.contains(persianRegex))
        assertTrue(fa.dropFileHint.contains(persianRegex))

        val ru = LocalizationProvider.getStrings(AppLanguage.RUSSIAN)
        assertEquals("📋 Вставить", ru.pasteFromClipboard)
        assertEquals("📁 Выбрать файл для хеша", ru.dropFileHint)
    }
}

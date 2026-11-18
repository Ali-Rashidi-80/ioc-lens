package io.github.mrcoder20.ioclens.e2e

import io.github.mrcoder20.ioclens.core.localization.AppLanguage
import io.github.mrcoder20.ioclens.core.localization.LocalizationProvider
import io.github.mrcoder20.ioclens.core.storage.InMemoryKeyValueStorage
import io.github.mrcoder20.ioclens.data.remote.AlienVaultOtxProvider
import io.github.mrcoder20.ioclens.data.remote.MalwareBazaarProvider
import io.github.mrcoder20.ioclens.data.remote.ThreatFoxProvider
import io.github.mrcoder20.ioclens.data.remote.ThreatIntelAggregator
import io.github.mrcoder20.ioclens.data.remote.UrlhausProvider
import io.github.mrcoder20.ioclens.data.repository.SettingsRepositoryImpl
import io.github.mrcoder20.ioclens.data.repository.ThreatIntelRepositoryImpl
import io.github.mrcoder20.ioclens.domain.model.AppSettings
import io.github.mrcoder20.ioclens.domain.model.HashAlgorithm
import io.github.mrcoder20.ioclens.domain.model.Indicator
import io.github.mrcoder20.ioclens.domain.model.IndicatorType
import io.github.mrcoder20.ioclens.domain.model.ParsedIoc
import io.github.mrcoder20.ioclens.domain.model.SourceResult
import io.github.mrcoder20.ioclens.domain.model.TriageVerdict
import io.github.mrcoder20.ioclens.domain.model.VerdictLevel
import io.github.mrcoder20.ioclens.domain.parser.IOCParser
import io.github.mrcoder20.ioclens.domain.usecase.AnalyzeIndicatorUseCase
import io.github.mrcoder20.ioclens.domain.usecase.SetIntroCompletedUseCase
import io.github.mrcoder20.ioclens.presentation.home.HomeEvent
import io.github.mrcoder20.ioclens.presentation.home.HomeViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * TIER 4: Real-World Application Scenarios (Opaque-Box E2E Test Suite)
 *
 * Verifies end-to-end security workflows and realistic multi-stage user journeys:
 * 1. SOC Analyst Malicious URL Triage:
 *    Defanged URL input -> Refanged -> Query Zero-Key engines -> Malicious verdict -> History persistence -> Search.
 * 2. Privacy-First Internal Incident Response:
 *    RFC 1918 gateway & internal host -> Instant Privacy Guard clean verdict with 0 outbound network calls.
 * 3. Malware Cryptographic Hash Hunting:
 *    Malicious SHA-256 hash -> Parsed algorithm -> MalwareBazaar & ThreatFox detections -> High confidence verdict.
 * 4. Multi-Lingual Shift Handover:
 *    English triage -> Persian analyst handover -> Russian analyst review -> Seamless reactive updates with 0 leaks.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class Tier4RealWorldScenariosTest {

    private var testDispatcher = StandardTestDispatcher()
    private val parser = IOCParser()
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private val persianRegex = Regex("[\\u0600-\\u06FF]")

    @BeforeTest
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        io.github.mrcoder20.ioclens.domain.security.SharePayloadReceiver.consume()
    }

    @AfterTest
    fun tearDown() {
        io.github.mrcoder20.ioclens.domain.security.SharePayloadReceiver.consume()
        Dispatchers.resetMain()
    }

    // ---------------------------------------------------------------------------------------------
    // SCENARIO 1: SOC Analyst Malicious URL Triage Workflow
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testSocAnalystMaliciousUrlTriageWorkflow() = runTest {
        val storage = InMemoryKeyValueStorage()
        val settingsRepo = SettingsRepositoryImpl(storage)

        val rawDefangedUrl = "hxxps://secure-bank-login[.]phish-update[.]com/auth/login"

        // 1. Refanging & Parsing
        val parsed = parser.parse(rawDefangedUrl)
        assertTrue(parsed is ParsedIoc.Url)
        assertEquals("https://secure-bank-login.phish-update.com/auth/login", parsed.normalizedValue)
        assertEquals("secure-bank-login.phish-update.com", parsed.host)
        assertFalse(parsed.isInternal)

        // 2. Mock Threat Intelligence Engines Responses
        val mockEngine = MockEngine(MockEngineConfig().apply {
            dispatcher = StandardTestDispatcher(testScheduler)
            addHandler { request ->
                val urlStr = request.url.toString()
                when {
                    urlStr.contains("urlhaus-api.abuse.ch") -> {
                        respond(
                            content = """{"query_status": "ok", "url_status": "online", "threat": "malware_download", "tags": ["emotet"]}""",
                            status = HttpStatusCode.OK,
                            headers = jsonHeaders
                        )
                    }
                    urlStr.contains("otx.alienvault.com") -> {
                        respond(
                            content = """{"pulse_info": {"count": 4, "pulses": [{"name": "Phishing Campaign"}]}}""",
                            status = HttpStatusCode.OK,
                            headers = jsonHeaders
                        )
                    }
                    urlStr.contains("threatfox-api.abuse.ch") -> {
                        respond(
                            content = """{"query_status": "ok", "data": [{"threat_type": "botnet_c2", "confidence_level": 90}]}""",
                            status = HttpStatusCode.OK,
                            headers = jsonHeaders
                        )
                    }
                    else -> respond("{}", HttpStatusCode.OK, jsonHeaders)
                }
            }
        })

        val client = HttpClient(mockEngine)
        val aggregator = ThreatIntelAggregator(client)
        val threatRepo = ThreatIntelRepositoryImpl(settingsRepo, aggregator)
        val analyzeUseCase = AnalyzeIndicatorUseCase(threatRepo, parser)
        val introUseCase = SetIntroCompletedUseCase(settingsRepo)
        val viewModel = HomeViewModel(analyzeUseCase, introUseCase, settingsRepo)

        // 3. User Enters defanged URL and triggers Scan
        viewModel.onEvent(HomeEvent.QueryChanged(rawDefangedUrl))
        viewModel.onEvent(HomeEvent.AnalyzeClicked)

        advanceUntilIdle()

        // 4. Verify Active Verdict
        val activeVerdict = viewModel.uiState.value.activeVerdict
        assertNotNull(activeVerdict, "Active verdict must be populated")
        assertEquals(VerdictLevel.MALICIOUS, activeVerdict.overallVerdict)
        assertTrue(activeVerdict.confidenceScore >= 80, "Confidence score must be high for multi-engine detections")
        assertTrue(activeVerdict.sourceResults.any { it.verdict == VerdictLevel.MALICIOUS })

        // 5. Verify Recorded in History
        val history = viewModel.uiState.value.scanHistory
        assertEquals(1, history.size)
        assertEquals(parsed.normalizedValue, history.first().indicator.normalizedValue)

        // 6. User switches to History Tab to view logged triage
        viewModel.onEvent(HomeEvent.SelectBottomNavTab(1))
        assertEquals(1, viewModel.uiState.value.selectedBottomNavTab)
        assertFalse(viewModel.uiState.value.isHistorySheetOpen)
    }

    // ---------------------------------------------------------------------------------------------
    // SCENARIO 2: Privacy-First Internal Incident Response Workflow
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testPrivacyFirstInternalIncidentResponseWorkflow() = runTest {
        val storage = InMemoryKeyValueStorage()
        val settingsRepo = SettingsRepositoryImpl(storage)

        // Mock Engine must fail immediately if ANY network call is dispatched
        var networkDispatched = 0
        val mockEngine = MockEngine { request ->
            networkDispatched++
            error("Privacy Guard VIOLATION: Network call dispatched to ${request.url}")
        }

        val client = HttpClient(mockEngine)
        val aggregator = ThreatIntelAggregator(client)
        val threatRepo = ThreatIntelRepositoryImpl(settingsRepo, aggregator)
        val analyzeUseCase = AnalyzeIndicatorUseCase(threatRepo, parser)
        val introUseCase = SetIntroCompletedUseCase(settingsRepo)
        val viewModel = HomeViewModel(analyzeUseCase, introUseCase, settingsRepo)

        // Analyst tests internal RFC 1918 gateway
        val internalIp = "172.16.50.4"
        viewModel.onEvent(HomeEvent.QueryChanged(internalIp))
        viewModel.onEvent(HomeEvent.AnalyzeClicked)

        advanceUntilIdle()

        assertEquals(0, networkDispatched, "Zero network requests must be sent for internal IP")
        val ipVerdict = viewModel.uiState.value.activeVerdict
        assertNotNull(ipVerdict)
        assertEquals(VerdictLevel.CLEAN, ipVerdict.overallVerdict)
        assertEquals(0, ipVerdict.confidenceScore)
        assertTrue(ipVerdict.summaryDescription.contains("Privacy Guard Activated"))

        // Analyst tests corporate internal domain
        val internalDomain = "dc01.corp.internal"
        viewModel.onEvent(HomeEvent.QueryChanged(internalDomain))
        viewModel.onEvent(HomeEvent.AnalyzeClicked)

        advanceUntilIdle()

        assertEquals(0, networkDispatched, "Zero network requests must be sent for internal domain")
        val domainVerdict = viewModel.uiState.value.activeVerdict
        assertNotNull(domainVerdict)
        assertEquals(VerdictLevel.CLEAN, domainVerdict.overallVerdict)
        assertTrue(domainVerdict.summaryDescription.contains("Privacy Guard Activated"))

        // Verify history has both queries
        assertEquals(2, viewModel.uiState.value.scanHistory.size)
    }

    // ---------------------------------------------------------------------------------------------
    // SCENARIO 3: Malware Cryptographic Hash Hunting Workflow
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testMalwareCryptographicHashHuntingWorkflow() = runTest {
        val storage = InMemoryKeyValueStorage()
        val settingsRepo = SettingsRepositoryImpl(storage)

        val wannaCrySha256 = "ed01ebf83334002287495601d34f40bbd16228b044dd163d70231e881b416ad0"

        // Verify parsing
        val parsed = parser.parse(wannaCrySha256)
        assertTrue(parsed is ParsedIoc.Hash)
        assertEquals(HashAlgorithm.SHA256, parsed.algorithm)
        assertEquals(wannaCrySha256, parsed.normalizedValue)

        val mockEngine = MockEngine(MockEngineConfig().apply {
            dispatcher = StandardTestDispatcher(testScheduler)
            addHandler { request ->
                when {
                    request.url.toString().contains("mb-api.abuse.ch") -> {
                        respond(
                            content = """{"query_status": "ok", "data": [{"sha256_hash": "$wannaCrySha256", "signature": "WannaCry", "file_type": "exe"}]}""",
                            status = HttpStatusCode.OK,
                            headers = jsonHeaders
                        )
                    }
                    request.url.toString().contains("threatfox-api.abuse.ch") -> {
                        respond(
                            content = """{"query_status": "ok", "data": [{"threat_type": "payload", "malware": "win.wannacry", "confidence_level": 100}]}""",
                            status = HttpStatusCode.OK,
                            headers = jsonHeaders
                        )
                    }
                    else -> respond("{}", HttpStatusCode.OK, jsonHeaders)
                }
            }
        })

        val client = HttpClient(mockEngine)
        val aggregator = ThreatIntelAggregator(client)
        val threatRepo = ThreatIntelRepositoryImpl(settingsRepo, aggregator)
        val analyzeUseCase = AnalyzeIndicatorUseCase(threatRepo, parser)
        val introUseCase = SetIntroCompletedUseCase(settingsRepo)
        val viewModel = HomeViewModel(analyzeUseCase, introUseCase, settingsRepo)

        viewModel.onEvent(HomeEvent.QueryChanged(wannaCrySha256))
        viewModel.onEvent(HomeEvent.AnalyzeClicked)

        advanceUntilIdle()

        val verdict = viewModel.uiState.value.activeVerdict
        assertNotNull(verdict)
        assertEquals(VerdictLevel.MALICIOUS, verdict.overallVerdict)
        assertTrue(verdict.confidenceScore >= 80)
        assertTrue(verdict.sourceResults.any { it.providerName == "MalwareBazaar (abuse.ch)" && it.verdict == VerdictLevel.MALICIOUS })
        assertTrue(verdict.sourceResults.any { it.providerName == "ThreatFox (abuse.ch)" && it.verdict == VerdictLevel.MALICIOUS })
    }

    // ---------------------------------------------------------------------------------------------
    // SCENARIO 4: Multi-Lingual Shift Handover with History Persistence
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testMultiLingualShiftHandoverWithHistoryPersistence() = runTest {
        val storage = InMemoryKeyValueStorage()
        val settingsRepo = SettingsRepositoryImpl(storage)
        settingsRepo.setAppLanguage(AppLanguage.ENGLISH)

        val sampleVerdict = TriageVerdict(
            indicator = Indicator("1.1.1.1", "1.1.1.1", IndicatorType.IP_ADDRESS),
            overallVerdict = VerdictLevel.CLEAN,
            confidenceScore = 98,
            summaryDescription = "Clean DNS",
            sourceResults = emptyList()
        )
        settingsRepo.saveScanHistory(listOf(sampleVerdict))

        val mockAggregator = ThreatIntelAggregator(HttpClient(MockEngine { respond("{}", HttpStatusCode.OK, jsonHeaders) }))
        val threatRepo = ThreatIntelRepositoryImpl(settingsRepo, mockAggregator)
        val analyzeUseCase = AnalyzeIndicatorUseCase(threatRepo, parser)
        val introUseCase = SetIntroCompletedUseCase(settingsRepo)
        val viewModel = HomeViewModel(analyzeUseCase, introUseCase, settingsRepo)

        // 1. English analyst verifies history
        assertEquals(AppLanguage.ENGLISH, viewModel.uiState.value.appLanguage)
        val enStrings = LocalizationProvider.getStrings(AppLanguage.ENGLISH)
        assertFalse(enStrings.settingsTitle.contains(persianRegex))
        assertFalse(enStrings.cardPrivacyGuardTitle.contains(persianRegex))

        // 2. Persian analyst takes over: switches to Persian
        viewModel.onEvent(HomeEvent.LanguageChanged(AppLanguage.PERSIAN))
        advanceUntilIdle()

        assertEquals(AppLanguage.PERSIAN, viewModel.uiState.value.appLanguage)
        val faStrings = LocalizationProvider.getStrings(AppLanguage.PERSIAN)
        assertTrue(faStrings.settingsTitle.contains(persianRegex))
        assertTrue(faStrings.cardPrivacyGuardTitle.contains(persianRegex))

        // History is preserved seamlessly
        assertEquals(1, viewModel.uiState.value.scanHistory.size)
        assertEquals("1.1.1.1", viewModel.uiState.value.scanHistory.first().indicator.rawValue)

        // 3. Russian analyst takes over: switches to Russian
        viewModel.onEvent(HomeEvent.LanguageChanged(AppLanguage.RUSSIAN))
        advanceUntilIdle()

        assertEquals(AppLanguage.RUSSIAN, viewModel.uiState.value.appLanguage)
        val ruStrings = LocalizationProvider.getStrings(AppLanguage.RUSSIAN)
        assertFalse(ruStrings.settingsTitle.contains(persianRegex))
        assertFalse(ruStrings.cardPrivacyGuardTitle.contains(persianRegex))
        assertEquals(1, viewModel.uiState.value.scanHistory.size)
    }
}

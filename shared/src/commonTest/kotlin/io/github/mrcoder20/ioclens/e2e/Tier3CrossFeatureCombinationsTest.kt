package io.github.mrcoder20.ioclens.e2e

import io.github.mrcoder20.ioclens.core.localization.AppLanguage
import io.github.mrcoder20.ioclens.core.localization.AppTheme
import io.github.mrcoder20.ioclens.core.localization.LocalizationProvider
import io.github.mrcoder20.ioclens.core.storage.InMemoryKeyValueStorage
import io.github.mrcoder20.ioclens.data.remote.ThreatIntelAggregator
import io.github.mrcoder20.ioclens.data.repository.SettingsRepositoryImpl
import io.github.mrcoder20.ioclens.data.repository.ThreatIntelRepositoryImpl
import io.github.mrcoder20.ioclens.domain.model.AppSettings
import io.github.mrcoder20.ioclens.domain.model.Indicator
import io.github.mrcoder20.ioclens.domain.model.IndicatorType
import io.github.mrcoder20.ioclens.domain.model.SourceResult
import io.github.mrcoder20.ioclens.domain.model.TriageVerdict
import io.github.mrcoder20.ioclens.domain.model.VerdictLevel
import io.github.mrcoder20.ioclens.domain.parser.IOCParser
import io.github.mrcoder20.ioclens.domain.repository.ThreatIntelRepository
import io.github.mrcoder20.ioclens.domain.usecase.AnalyzeIndicatorUseCase
import io.github.mrcoder20.ioclens.domain.usecase.GetAppSettingsUseCase
import io.github.mrcoder20.ioclens.domain.usecase.SetIntroCompletedUseCase
import io.github.mrcoder20.ioclens.presentation.home.HomeEvent
import io.github.mrcoder20.ioclens.presentation.home.HomeViewModel
import io.github.mrcoder20.ioclens.presentation.intro.IntroEvent
import io.github.mrcoder20.ioclens.presentation.intro.IntroViewModel
import io.github.mrcoder20.ioclens.presentation.main.MainViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * TIER 3: Cross-Feature Combinations (Opaque-Box E2E Test Suite)
 *
 * Verifies multi-feature interactions, reactive state bindings, and screen transitions:
 * 1. Locale change during an active scan cleanly updates UI language and preserves final verdict.
 * 2. History item selection transitions from History tab (2) directly to Scanner tab (0) with populated verdict.
 * 3. Onboarding completion drives MainViewModel/HomeScreen transition with Tab 0 ready for immediate zero-key scan.
 * 4. Clearing scan history from the dedicated History screen synchronizes persistence and resets UI state.
 * 5. Rapid successive query submissions cleanly cancel in-flight jobs without race condition state leaks.
 * 6. Theme changes maintain current bottom navigation destination across tabs.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class Tier3CrossFeatureCombinationsTest {

    private val testDispatcher = StandardTestDispatcher()
    private val parser = IOCParser()
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class DelayableThreatIntelRepository(
        private val delayMs: Long = 200L,
        private val mockVerdict: TriageVerdict
    ) : ThreatIntelRepository {
        override suspend fun analyzeIndicator(indicator: Indicator): TriageVerdict {
            if (delayMs > 0) delay(delayMs)
            return mockVerdict.copy(indicator = indicator)
        }

        override suspend fun analyzeIndicator(
            indicator: Indicator,
            onProgress: ((finishedCount: Int, totalCount: Int) -> Unit)?
        ): TriageVerdict {
            onProgress?.invoke(5, 10)
            if (delayMs > 0) delay(delayMs)
            onProgress?.invoke(10, 10)
            return mockVerdict.copy(indicator = indicator)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // COMBINATION 1: Language Switch During Active Scan
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testLanguageSwitchDuringActiveScanPreservesVerdictAndLocalizes() = runTest {
        val storage = InMemoryKeyValueStorage()
        val settingsRepo = SettingsRepositoryImpl(storage)
        settingsRepo.setAppLanguage(AppLanguage.ENGLISH)

        val sampleVerdict = TriageVerdict(
            indicator = Indicator("8.8.8.8", "8.8.8.8", IndicatorType.IP_ADDRESS),
            overallVerdict = VerdictLevel.CLEAN,
            confidenceScore = 95,
            summaryDescription = "Clean Google Public DNS",
            sourceResults = listOf(SourceResult("Cloudflare", VerdictLevel.CLEAN, 0, 1, "Resolved"))
        )

        val delayedRepo = DelayableThreatIntelRepository(delayMs = 100L, mockVerdict = sampleVerdict)
        val analyzeUseCase = AnalyzeIndicatorUseCase(delayedRepo, parser)
        val introUseCase = SetIntroCompletedUseCase(settingsRepo)
        val viewModel = HomeViewModel(analyzeUseCase, introUseCase, settingsRepo)

        // 1. Submit scan query in English
        viewModel.onEvent(HomeEvent.QueryChanged("8.8.8.8"))
        viewModel.onEvent(HomeEvent.AnalyzeClicked)

        // 2. While in-flight, switch application language to Persian
        viewModel.onEvent(HomeEvent.LanguageChanged(AppLanguage.PERSIAN))

        // Let coroutine complete
        advanceUntilIdle()

        // 3. Verify language was updated reactively without corrupting scan completion
        assertEquals(AppLanguage.PERSIAN, viewModel.uiState.value.appLanguage)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.activeVerdict)
        assertEquals("8.8.8.8", viewModel.uiState.value.activeVerdict?.indicator?.rawValue)
        assertEquals(1, viewModel.uiState.value.scanHistory.size)
    }

    // ---------------------------------------------------------------------------------------------
    // COMBINATION 2: History Item Click -> Jump to Scanner Tab with Loaded Verdict
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testHistoryItemClickTransitionsToScannerWithPopulatedVerdict() = runTest {
        val storage = InMemoryKeyValueStorage()
        val settingsRepo = SettingsRepositoryImpl(storage)
        val sampleVerdict = TriageVerdict(
            indicator = Indicator("1.1.1.1", "1.1.1.1", IndicatorType.IP_ADDRESS),
            overallVerdict = VerdictLevel.CLEAN,
            confidenceScore = 99,
            summaryDescription = "Clean Cloudflare DNS",
            sourceResults = emptyList()
        )
        settingsRepo.saveScanHistory(listOf(sampleVerdict))

        val mockAggregator = ThreatIntelAggregator(HttpClient(MockEngine { respond("{}", HttpStatusCode.OK, jsonHeaders) }))
        val threatRepo = ThreatIntelRepositoryImpl(settingsRepo, mockAggregator)
        val analyzeUseCase = AnalyzeIndicatorUseCase(threatRepo, parser)
        val introUseCase = SetIntroCompletedUseCase(settingsRepo)
        val viewModel = HomeViewModel(analyzeUseCase, introUseCase, settingsRepo)

        // User navigates to History Screen (Tab 1)
        viewModel.onEvent(HomeEvent.SelectBottomNavTab(1))
        assertEquals(1, viewModel.uiState.value.selectedBottomNavTab)

        // User taps a history card
        viewModel.onEvent(HomeEvent.HistoryItemClicked(sampleVerdict))

        // Invariant: Navigation jumps to Scanner (Tab 0) with populated verdict and input
        assertEquals(0, viewModel.uiState.value.selectedBottomNavTab, "Must navigate to Scanner tab 0")
        assertEquals(sampleVerdict, viewModel.uiState.value.activeVerdict, "Active verdict must be set")
        assertEquals("1.1.1.1", viewModel.uiState.value.queryInput, "Query input must be populated")
        assertFalse(viewModel.uiState.value.isHistorySheetOpen, "Modal sheet overlays must remain closed")
        assertFalse(viewModel.uiState.value.isSettingsSheetOpen, "Modal sheet overlays must remain closed")
    }

    // ---------------------------------------------------------------------------------------------
    // COMBINATION 3: Onboarding Completion Drives Main Dashboard Routing
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testOnboardingCompletionDrivesMainDashboardTransition() = runTest {
        val storage = InMemoryKeyValueStorage()
        val settingsRepo = SettingsRepositoryImpl(storage)
        val getAppSettingsUseCase = GetAppSettingsUseCase(settingsRepo)
        val setIntroCompletedUseCase = SetIntroCompletedUseCase(settingsRepo)

        val mainViewModel = MainViewModel(getAppSettingsUseCase)
        val introViewModel = IntroViewModel(setIntroCompletedUseCase, settingsRepo)

        // Initial startup state: Intro is NOT completed
        assertFalse(mainViewModel.uiState.value.appSettings.isIntroCompleted)

        // User taps Complete on onboarding
        introViewModel.onEvent(IntroEvent.CompleteClicked)
        advanceUntilIdle()

        // MainViewModel observes updated settings via StateFlow
        assertTrue(settingsRepo.getSettings().isIntroCompleted)
        assertTrue(mainViewModel.uiState.value.appSettings.isIntroCompleted)

        // Create HomeViewModel; Tab 0 (Scanner) is default view
        val mockAggregator = ThreatIntelAggregator(HttpClient(MockEngine { respond("{}", HttpStatusCode.OK, jsonHeaders) }))
        val threatRepo = ThreatIntelRepositoryImpl(settingsRepo, mockAggregator)
        val analyzeUseCase = AnalyzeIndicatorUseCase(threatRepo, parser)
        val homeViewModel = HomeViewModel(analyzeUseCase, setIntroCompletedUseCase, settingsRepo)

        assertEquals(0, homeViewModel.uiState.value.selectedBottomNavTab)
        assertNull(homeViewModel.uiState.value.activeVerdict)
    }

    // ---------------------------------------------------------------------------------------------
    // COMBINATION 4: Clearing History Synchronizes State and Empty View
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testClearHistorySynchronizesStateAndPersistence() = runTest {
        val storage = InMemoryKeyValueStorage()
        val settingsRepo = SettingsRepositoryImpl(storage)
        val initialHistory = listOf(
            TriageVerdict(Indicator("1.1.1.1", "1.1.1.1", IndicatorType.IP_ADDRESS), VerdictLevel.CLEAN, 90, "Clean", emptyList()),
            TriageVerdict(Indicator("bad.com", "bad.com", IndicatorType.DOMAIN), VerdictLevel.MALICIOUS, 80, "Malicious", emptyList())
        )
        settingsRepo.saveScanHistory(initialHistory)

        val mockAggregator = ThreatIntelAggregator(HttpClient(MockEngine { respond("{}", HttpStatusCode.OK, jsonHeaders) }))
        val threatRepo = ThreatIntelRepositoryImpl(settingsRepo, mockAggregator)
        val analyzeUseCase = AnalyzeIndicatorUseCase(threatRepo, parser)
        val introUseCase = SetIntroCompletedUseCase(settingsRepo)
        val viewModel = HomeViewModel(analyzeUseCase, introUseCase, settingsRepo)

        assertEquals(2, viewModel.uiState.value.scanHistory.size)

        // Navigate to History Tab
        viewModel.onEvent(HomeEvent.SelectBottomNavTab(1))

        // Trigger Clear History
        viewModel.onEvent(HomeEvent.ClearHistory)

        assertTrue(viewModel.uiState.value.scanHistory.isEmpty(), "UI state history must be empty")
        assertTrue(settingsRepo.getScanHistory().isEmpty(), "Repository history must be empty")
    }

    // ---------------------------------------------------------------------------------------------
    // COMBINATION 5: Rapid Successive Query Submissions Cancel Prior In-Flight Scans
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testRapidSuccessiveQueriesCancelPriorScanCleanly() = runTest {
        val storage = InMemoryKeyValueStorage()
        val settingsRepo = SettingsRepositoryImpl(storage)

        val verdict1 = TriageVerdict(
            indicator = Indicator("1.1.1.1", "1.1.1.1", IndicatorType.IP_ADDRESS),
            overallVerdict = VerdictLevel.CLEAN,
            confidenceScore = 90,
            summaryDescription = "Cloudflare DNS",
            sourceResults = emptyList()
        )
        val verdict2 = TriageVerdict(
            indicator = Indicator("8.8.8.8", "8.8.8.8", IndicatorType.IP_ADDRESS),
            overallVerdict = VerdictLevel.CLEAN,
            confidenceScore = 95,
            summaryDescription = "Google DNS",
            sourceResults = emptyList()
        )

        val delayedRepo = object : ThreatIntelRepository {
            override suspend fun analyzeIndicator(indicator: Indicator): TriageVerdict = analyzeIndicator(indicator, null)
            override suspend fun analyzeIndicator(
                indicator: Indicator,
                onProgress: ((finishedCount: Int, totalCount: Int) -> Unit)?
            ): TriageVerdict {
                if (indicator.rawValue == "1.1.1.1") {
                    delay(500L) // Slow query
                    return verdict1
                } else {
                    delay(50L) // Fast query
                    return verdict2
                }
            }
        }

        val analyzeUseCase = AnalyzeIndicatorUseCase(delayedRepo, parser)
        val introUseCase = SetIntroCompletedUseCase(settingsRepo)
        val viewModel = HomeViewModel(analyzeUseCase, introUseCase, settingsRepo)

        // 1. Submit Query 1 (1.1.1.1)
        viewModel.onEvent(HomeEvent.QueryChanged("1.1.1.1"))
        viewModel.onEvent(HomeEvent.AnalyzeClicked)

        // 2. Immediately submit Query 2 (8.8.8.8), canceling Query 1
        viewModel.onEvent(HomeEvent.QueryChanged("8.8.8.8"))
        viewModel.onEvent(HomeEvent.AnalyzeClicked)

        advanceUntilIdle()

        // Final active verdict must be Query 2, never overwritten by Query 1
        assertNotNull(viewModel.uiState.value.activeVerdict)
        assertEquals("8.8.8.8", viewModel.uiState.value.activeVerdict?.indicator?.rawValue)
    }

    // ---------------------------------------------------------------------------------------------
    // COMBINATION 6: Theme Toggles Across Navigation Tabs
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testThemeTogglesAcrossNavigationTabsPreservesSelectedDestination() = runTest {
        val storage = InMemoryKeyValueStorage()
        val settingsRepo = SettingsRepositoryImpl(storage)
        val mockAggregator = ThreatIntelAggregator(HttpClient(MockEngine { respond("{}", HttpStatusCode.OK, jsonHeaders) }))
        val threatRepo = ThreatIntelRepositoryImpl(settingsRepo, mockAggregator)
        val analyzeUseCase = AnalyzeIndicatorUseCase(threatRepo, parser)
        val introUseCase = SetIntroCompletedUseCase(settingsRepo)
        val viewModel = HomeViewModel(analyzeUseCase, introUseCase, settingsRepo)

        // Navigate to Settings Tab (Tab 2)
        viewModel.onEvent(HomeEvent.SelectBottomNavTab(2))
        assertEquals(2, viewModel.uiState.value.selectedBottomNavTab)

        // Switch theme to SYSTEM
        viewModel.onEvent(HomeEvent.ThemeChanged(AppTheme.SYSTEM))
        advanceUntilIdle()
        assertEquals(AppTheme.SYSTEM, viewModel.uiState.value.appTheme)
        assertEquals(2, viewModel.uiState.value.selectedBottomNavTab, "Tab selection must remain at 2")

        // Switch theme back to DARK
        viewModel.onEvent(HomeEvent.ThemeChanged(AppTheme.DARK))
        advanceUntilIdle()
        assertEquals(AppTheme.DARK, viewModel.uiState.value.appTheme)
        assertEquals(2, viewModel.uiState.value.selectedBottomNavTab)
    }
}

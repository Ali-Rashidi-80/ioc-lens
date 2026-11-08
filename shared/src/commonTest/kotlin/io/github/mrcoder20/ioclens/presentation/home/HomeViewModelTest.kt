package io.github.mrcoder20.ioclens.presentation.home

import io.github.mrcoder20.ioclens.core.localization.AppLanguage
import io.github.mrcoder20.ioclens.core.localization.AppTheme
import io.github.mrcoder20.ioclens.domain.model.AppSettings
import io.github.mrcoder20.ioclens.domain.model.Indicator
import io.github.mrcoder20.ioclens.domain.model.IndicatorType
import io.github.mrcoder20.ioclens.domain.model.SourceResult
import io.github.mrcoder20.ioclens.domain.model.TriageVerdict
import io.github.mrcoder20.ioclens.domain.model.VerdictLevel
import io.github.mrcoder20.ioclens.domain.repository.SettingsRepository
import io.github.mrcoder20.ioclens.domain.repository.ThreatIntelRepository
import io.github.mrcoder20.ioclens.domain.usecase.AnalyzeIndicatorUseCase
import io.github.mrcoder20.ioclens.domain.usecase.SetIntroCompletedUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    class FakeSettingsRepository(initialSettings: AppSettings = AppSettings()) : SettingsRepository {
        private val _settings = MutableStateFlow(initialSettings)
        override val settingsState: StateFlow<AppSettings> = _settings.asStateFlow()
        private val history = mutableListOf<TriageVerdict>()

        override fun getSettings(): AppSettings = _settings.value
        override fun setIntroCompleted(completed: Boolean) { _settings.value = _settings.value.copy(isIntroCompleted = completed) }
        override fun setPrivacyGuardEnabled(enabled: Boolean) { _settings.value = _settings.value.copy(privacyGuardEnabled = enabled) }
        override fun setAppTheme(theme: AppTheme) { _settings.value = _settings.value.copy(appTheme = theme) }
        override fun setAppLanguage(language: AppLanguage) { _settings.value = _settings.value.copy(appLanguage = language) }
        override fun updateApiKey(provider: String, key: String) {}

        override fun getScanHistory(): List<TriageVerdict> = history.toList()
        override fun saveScanHistory(history: List<TriageVerdict>) {
            this.history.clear()
            this.history.addAll(history)
        }
        override fun clearScanHistory() { history.clear() }
    }

    class FakeThreatIntelRepository : ThreatIntelRepository {
        var delayMs: Long = 0
        override suspend fun analyzeIndicator(indicator: Indicator): TriageVerdict {
            if (delayMs > 0) delay(delayMs)
            return TriageVerdict(
                indicator = indicator,
                overallVerdict = if (indicator.normalizedValue.contains("evil")) VerdictLevel.MALICIOUS else VerdictLevel.CLEAN,
                confidenceScore = 90,
                summaryDescription = "Test Verdict for ${indicator.normalizedValue}",
                sourceResults = listOf(
                    SourceResult("TestEngine", VerdictLevel.CLEAN, 0, 1, "Engine summary")
                )
            )
        }
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testEmptyQueryDisplaysValidationError() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val threatRepo = FakeThreatIntelRepository()
        val analyzeUseCase = AnalyzeIndicatorUseCase(threatRepo)
        val introUseCase = SetIntroCompletedUseCase(settingsRepo)
        val viewModel = HomeViewModel(analyzeUseCase, introUseCase, settingsRepo)

        viewModel.onEvent(HomeEvent.QueryChanged(""))
        viewModel.onEvent(HomeEvent.AnalyzeClicked)

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.activeVerdict)
    }

    @Test
    fun testClearButtonResetsState() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val threatRepo = FakeThreatIntelRepository()
        val analyzeUseCase = AnalyzeIndicatorUseCase(threatRepo)
        val introUseCase = SetIntroCompletedUseCase(settingsRepo)
        val viewModel = HomeViewModel(analyzeUseCase, introUseCase, settingsRepo)

        viewModel.onEvent(HomeEvent.QueryChanged("8.8.8.8"))
        viewModel.onEvent(HomeEvent.AnalyzeClicked)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.activeVerdict)

        viewModel.onEvent(HomeEvent.ClearClicked)
        assertEquals("", viewModel.uiState.value.queryInput)
        assertNull(viewModel.uiState.value.activeVerdict)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun testCancelScanMidFlightStopsLoadingWithoutError() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val threatRepo = FakeThreatIntelRepository().apply { delayMs = 1000 }
        val analyzeUseCase = AnalyzeIndicatorUseCase(threatRepo)
        val introUseCase = SetIntroCompletedUseCase(settingsRepo)
        val viewModel = HomeViewModel(analyzeUseCase, introUseCase, settingsRepo)

        viewModel.onEvent(HomeEvent.QueryChanged("8.8.8.8"))
        viewModel.onEvent(HomeEvent.AnalyzeClicked)
        advanceTimeBy(100)

        assertTrue(viewModel.uiState.value.isLoading, "ViewModel should be in loading state")

        viewModel.onEvent(HomeEvent.CancelScan)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading, "Loading should be false after cancel")
        assertNull(viewModel.uiState.value.errorMessage, "No error banner should display on cancel")
        assertNull(viewModel.uiState.value.activeVerdict, "No verdict should be published on cancelled scan")
    }

    @Test
    fun testRapidQueryResubmissionsCancelsPreviousAndCompletesLatest() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val threatRepo = FakeThreatIntelRepository().apply { delayMs = 500 }
        val analyzeUseCase = AnalyzeIndicatorUseCase(threatRepo)
        val introUseCase = SetIntroCompletedUseCase(settingsRepo)
        val viewModel = HomeViewModel(analyzeUseCase, introUseCase, settingsRepo)

        // Rapid submission 1
        viewModel.onEvent(HomeEvent.QueryChanged("1.1.1.1"))
        viewModel.onEvent(HomeEvent.AnalyzeClicked)
        advanceTimeBy(100)

        // Rapid submission 2 overrides submission 1
        viewModel.onEvent(HomeEvent.QueryChanged("evil-sample.com"))
        viewModel.onEvent(HomeEvent.AnalyzeClicked)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        val verdict = viewModel.uiState.value.activeVerdict
        assertNotNull(verdict)
        assertEquals("evil-sample.com", verdict.indicator.normalizedValue)
        assertEquals(VerdictLevel.MALICIOUS, verdict.overallVerdict)
    }

    @Test
    fun testTypingWhileScanningDoesNotAbortActiveScan() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val threatRepo = FakeThreatIntelRepository().apply { delayMs = 500 }
        val analyzeUseCase = AnalyzeIndicatorUseCase(threatRepo)
        val introUseCase = SetIntroCompletedUseCase(settingsRepo)
        val viewModel = HomeViewModel(analyzeUseCase, introUseCase, settingsRepo)

        viewModel.onEvent(HomeEvent.QueryChanged("8.8.8.8"))
        viewModel.onEvent(HomeEvent.AnalyzeClicked)
        advanceTimeBy(100)

        // User types while scanning
        viewModel.onEvent(HomeEvent.QueryChanged("8.8.8.8 typing more"))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.activeVerdict)
        assertEquals("8.8.8.8 typing more", viewModel.uiState.value.queryInput)
    }

    @Test
    fun testScanHistoryPersistenceAcrossViewModelLifecycle() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val threatRepo = FakeThreatIntelRepository()
        val analyzeUseCase = AnalyzeIndicatorUseCase(threatRepo)
        val introUseCase = SetIntroCompletedUseCase(settingsRepo)

        val vm1 = HomeViewModel(analyzeUseCase, introUseCase, settingsRepo)
        vm1.onEvent(HomeEvent.QueryChanged("1.1.1.1"))
        vm1.onEvent(HomeEvent.AnalyzeClicked)
        advanceUntilIdle()

        assertEquals(1, vm1.uiState.value.scanHistory.size)
        assertEquals(1, settingsRepo.getScanHistory().size)

        // Recreate ViewModel (e.g. app restart)
        val vm2 = HomeViewModel(analyzeUseCase, introUseCase, settingsRepo)
        advanceUntilIdle()

        assertEquals(1, vm2.uiState.value.scanHistory.size, "Persisted history must be loaded on startup")
        assertEquals("1.1.1.1", vm2.uiState.value.scanHistory[0].indicator.normalizedValue)

        // Clear history
        vm2.onEvent(HomeEvent.ClearHistory)
        assertTrue(vm2.uiState.value.scanHistory.isEmpty())
        assertTrue(settingsRepo.getScanHistory().isEmpty())
    }

    @Test
    fun testScanStatePreservedAcrossThemeAndLanguageToggles() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val threatRepo = FakeThreatIntelRepository()
        val analyzeUseCase = AnalyzeIndicatorUseCase(threatRepo)
        val introUseCase = SetIntroCompletedUseCase(settingsRepo)
        val viewModel = HomeViewModel(analyzeUseCase, introUseCase, settingsRepo)

        viewModel.onEvent(HomeEvent.QueryChanged("8.8.8.8"))
        viewModel.onEvent(HomeEvent.AnalyzeClicked)
        advanceUntilIdle()

        val verdictBefore = viewModel.uiState.value.activeVerdict
        assertNotNull(verdictBefore)

        // Toggle theme and language
        viewModel.onEvent(HomeEvent.ThemeChanged(AppTheme.SYSTEM))
        viewModel.onEvent(HomeEvent.LanguageChanged(AppLanguage.PERSIAN))
        advanceUntilIdle()

        assertEquals(AppTheme.SYSTEM, viewModel.uiState.value.appTheme)
        assertEquals(AppLanguage.PERSIAN, viewModel.uiState.value.appLanguage)
        assertEquals("8.8.8.8", viewModel.uiState.value.queryInput)
        assertEquals(verdictBefore, viewModel.uiState.value.activeVerdict)
    }

    @Test
    fun testStaleVerdictClearedImmediatelyOnNewScan() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val threatRepo = FakeThreatIntelRepository().apply { delayMs = 500 }
        val analyzeUseCase = AnalyzeIndicatorUseCase(threatRepo)
        val introUseCase = SetIntroCompletedUseCase(settingsRepo)
        val viewModel = HomeViewModel(analyzeUseCase, introUseCase, settingsRepo)

        // Complete first scan
        viewModel.onEvent(HomeEvent.QueryChanged("1.1.1.1"))
        viewModel.onEvent(HomeEvent.AnalyzeClicked)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.activeVerdict)
        assertEquals("1.1.1.1", viewModel.uiState.value.activeVerdict?.indicator?.normalizedValue)

        // Start new scan on different indicator
        viewModel.onEvent(HomeEvent.QueryChanged("evil-domain.com"))
        viewModel.onEvent(HomeEvent.AnalyzeClicked)
        advanceTimeBy(50)

        // While in-flight: stale verdict MUST be cleared, targetIndicator must be set
        assertTrue(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.activeVerdict, "Stale verdict must be cleared immediately when starting new scan")
        assertEquals("evil-domain.com", viewModel.uiState.value.targetIndicator?.normalizedValue)
        assertEquals(IndicatorType.DOMAIN, viewModel.uiState.value.targetIndicator?.type)

        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.activeVerdict)
        assertEquals("evil-domain.com", viewModel.uiState.value.activeVerdict?.indicator?.normalizedValue)
    }

    @Test
    fun testLiveScanBannerEngineProgressUpdates() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val threatRepo = object : ThreatIntelRepository {
            override suspend fun analyzeIndicator(indicator: Indicator): TriageVerdict = analyzeIndicator(indicator, null)
            override suspend fun analyzeIndicator(
                indicator: Indicator,
                onProgress: ((finishedCount: Int, totalCount: Int) -> Unit)?
            ): TriageVerdict {
                onProgress?.invoke(0, 10)
                delay(100)
                onProgress?.invoke(5, 10)
                delay(100)
                onProgress?.invoke(10, 10)
                return TriageVerdict(
                    indicator = indicator,
                    overallVerdict = VerdictLevel.CLEAN,
                    confidenceScore = 0,
                    summaryDescription = "Clean",
                    sourceResults = emptyList()
                )
            }
        }
        val analyzeUseCase = AnalyzeIndicatorUseCase(threatRepo)
        val introUseCase = SetIntroCompletedUseCase(settingsRepo)
        val viewModel = HomeViewModel(analyzeUseCase, introUseCase, settingsRepo)

        viewModel.onEvent(HomeEvent.QueryChanged("8.8.8.8"))
        viewModel.onEvent(HomeEvent.AnalyzeClicked)
        advanceTimeBy(10)

        assertTrue(viewModel.uiState.value.isLoading)
        assertEquals(0, viewModel.uiState.value.scanFinishedEngines)
        assertEquals(10, viewModel.uiState.value.scanTotalEngines)

        advanceTimeBy(100)
        assertEquals(5, viewModel.uiState.value.scanFinishedEngines)

        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.activeVerdict)
    }
}

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
import io.github.mrcoder20.ioclens.domain.security.FileHashResult
import io.github.mrcoder20.ioclens.domain.security.SharePayloadReceiver
import io.github.mrcoder20.ioclens.domain.security.SharedPayload
import io.github.mrcoder20.ioclens.domain.security.StreamHasher
import io.github.mrcoder20.ioclens.domain.security.TemporaryFileCleaner
import io.github.mrcoder20.ioclens.domain.usecase.AnalyzeIndicatorUseCase
import io.github.mrcoder20.ioclens.domain.usecase.SetIntroCompletedUseCase
import io.github.mrcoder20.ioclens.domain.vision.BarcodePayload
import io.github.mrcoder20.ioclens.domain.vision.BarcodeType
import io.github.mrcoder20.ioclens.domain.vision.QuishingRisk
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

    private var testDispatcher = StandardTestDispatcher()

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
        override fun deleteScanHistoryItem(rawValue: String) {
            history.removeAll { it.indicator.rawValue == rawValue }
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
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        SharePayloadReceiver.consume()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        SharePayloadReceiver.consume()
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

    @Test
    fun testBottomNavTabSelectionAndStateManagement() {
        val settingsRepo = FakeSettingsRepository()
        val threatRepo = FakeThreatIntelRepository()
        val analyzeUseCase = AnalyzeIndicatorUseCase(threatRepo)
        val introUseCase = SetIntroCompletedUseCase(settingsRepo)
        val viewModel = HomeViewModel(analyzeUseCase, introUseCase, settingsRepo)

        // 1. Initial State: Tab 0 (Scanner) is selected by default
        assertEquals(0, viewModel.uiState.value.selectedBottomNavTab)
        assertFalse(viewModel.uiState.value.isSettingsSheetOpen)
        assertFalse(viewModel.uiState.value.isHistorySheetOpen)

        // 2. Select Tab 1 (History): dedicated full-screen view without modal dialog overlay
        viewModel.onEvent(HomeEvent.SelectBottomNavTab(1))
        assertEquals(1, viewModel.uiState.value.selectedBottomNavTab)
        assertFalse(viewModel.uiState.value.isHistorySheetOpen)
        assertFalse(viewModel.uiState.value.isSettingsSheetOpen)

        // 3. SetHistorySheetOpen directly handles modal sheet lifecycle
        viewModel.onEvent(HomeEvent.SetHistorySheetOpen(true))
        assertTrue(viewModel.uiState.value.isHistorySheetOpen)
        viewModel.onEvent(HomeEvent.SetHistorySheetOpen(false))
        assertEquals(0, viewModel.uiState.value.selectedBottomNavTab)
        assertFalse(viewModel.uiState.value.isHistorySheetOpen)

        // 4. Select Tab 2 (Settings): dedicated full-screen view without modal dialog overlay
        viewModel.onEvent(HomeEvent.SelectBottomNavTab(2))
        assertEquals(2, viewModel.uiState.value.selectedBottomNavTab)
        assertFalse(viewModel.uiState.value.isSettingsSheetOpen)
        assertFalse(viewModel.uiState.value.isHistorySheetOpen)

        // 5. SetSettingsSheetOpen directly handles modal sheet lifecycle
        viewModel.onEvent(HomeEvent.SetSettingsSheetOpen(true))
        assertTrue(viewModel.uiState.value.isSettingsSheetOpen)
        viewModel.onEvent(HomeEvent.SetSettingsSheetOpen(false))
        assertEquals(0, viewModel.uiState.value.selectedBottomNavTab)
        assertFalse(viewModel.uiState.value.isSettingsSheetOpen)

        // 6. Explicitly selecting tab 0 keeps tab 0 selected
        viewModel.onEvent(HomeEvent.SelectBottomNavTab(0))
        assertEquals(0, viewModel.uiState.value.selectedBottomNavTab)
    }

    @Test
    fun testDeleteHistoryItemAndJumpNavigation() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val threatRepo = FakeThreatIntelRepository()
        val analyzeUseCase = AnalyzeIndicatorUseCase(threatRepo)
        val introUseCase = SetIntroCompletedUseCase(settingsRepo)
        val viewModel = HomeViewModel(analyzeUseCase, introUseCase, settingsRepo)

        // Seed scan history with two items
        val item1 = TriageVerdict(
            indicator = Indicator("8.8.8.8", "8.8.8.8", IndicatorType.IP_ADDRESS),
            overallVerdict = VerdictLevel.CLEAN,
            confidenceScore = 95,
            summaryDescription = "Google DNS",
            sourceResults = emptyList()
        )
        val item2 = TriageVerdict(
            indicator = Indicator("malware-test.com", "malware-test.com", IndicatorType.DOMAIN),
            overallVerdict = VerdictLevel.MALICIOUS,
            confidenceScore = 90,
            summaryDescription = "Known Bad Domain",
            sourceResults = emptyList()
        )
        settingsRepo.saveScanHistory(listOf(item1, item2))
        viewModel.onEvent(HomeEvent.ClearClicked) // Refresh state or trigger event

        // Re-initialize to load history
        val activeViewModel = HomeViewModel(analyzeUseCase, introUseCase, settingsRepo)
        advanceUntilIdle()
        assertEquals(2, activeViewModel.uiState.value.scanHistory.size)

        // 1. History Item Click jumps navigation to Tab 0 and sets active verdict
        activeViewModel.onEvent(HomeEvent.SelectBottomNavTab(1)) // On history tab
        assertEquals(1, activeViewModel.uiState.value.selectedBottomNavTab)

        activeViewModel.onEvent(HomeEvent.HistoryItemClicked(item2))
        assertEquals(0, activeViewModel.uiState.value.selectedBottomNavTab)
        assertFalse(activeViewModel.uiState.value.isHistorySheetOpen)
        assertEquals(item2.indicator.rawValue, activeViewModel.uiState.value.activeVerdict?.indicator?.rawValue)

        // 2. Delete History Item removes it from storage and UI state
        activeViewModel.onEvent(HomeEvent.DeleteHistoryItem(item2))
        advanceUntilIdle()

        val remainingHistory = activeViewModel.uiState.value.scanHistory
        assertEquals(1, remainingHistory.size)
        assertEquals("8.8.8.8", remainingHistory.first().indicator.rawValue)
        assertEquals(1, settingsRepo.getScanHistory().size)
    }

    @Test
    fun testConsecutiveScanRaceConditionPreservesLoadingAndCancelability() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val threatRepo = FakeThreatIntelRepository().apply { delayMs = 1000 }
        val analyzeUseCase = AnalyzeIndicatorUseCase(threatRepo)
        val introUseCase = SetIntroCompletedUseCase(settingsRepo)
        val viewModel = HomeViewModel(analyzeUseCase, introUseCase, settingsRepo)

        // Launch Scan 1
        viewModel.onEvent(HomeEvent.QueryChanged("1.1.1.1"))
        viewModel.onEvent(HomeEvent.AnalyzeClicked)
        advanceTimeBy(100)
        assertTrue(viewModel.uiState.value.isLoading)

        // Launch Scan 2 (cancels Scan 1)
        viewModel.onEvent(HomeEvent.QueryChanged("8.8.8.8"))
        viewModel.onEvent(HomeEvent.AnalyzeClicked)
        advanceTimeBy(50)

        // CRITICAL DEFECT 10 ASSERTION: Scan 1's cancellation MUST NOT flip isLoading to false
        assertTrue(viewModel.uiState.value.isLoading, "Scan 2 must be actively loading; Scan 1 must not overwrite isLoading")
        assertEquals("8.8.8.8", viewModel.uiState.value.targetIndicator?.rawValue)

        // CRITICAL DEFECT 10 ASSERTION: Scan 2 must remain cancelable (scanJob must not be nullified by Scan 1)
        viewModel.onEvent(HomeEvent.CancelScan)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.activeVerdict, "Scan 2 must be cleanly cancelled, no verdict published")
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun testFileHashingLifecycleAndCancellation() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val threatRepo = FakeThreatIntelRepository()
        val analyzeUseCase = AnalyzeIndicatorUseCase(threatRepo)
        val introUseCase = SetIntroCompletedUseCase(settingsRepo)

        val fakeHasher = object : StreamHasher {
            var delayMs = 500L
            override suspend fun hashFile(filePath: String): FileHashResult {
                delay(delayMs)
                return FileHashResult("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", "d41d8cd98f00b204e9800998ecf8427e", 0L)
            }
        }
        val viewModel = HomeViewModel(analyzeUseCase, introUseCase, settingsRepo, streamHasher = fakeHasher)

        // Start file hashing
        viewModel.onEvent(HomeEvent.FileDropped("/path/to/test.bin", "test.bin"))
        advanceTimeBy(100)
        assertTrue(viewModel.uiState.value.isHashingFile)
        assertEquals("test.bin", viewModel.uiState.value.hashingFileName)

        // Cancel mid-hashing
        viewModel.onEvent(HomeEvent.CancelScan)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isHashingFile)
        assertNull(viewModel.uiState.value.hashingFileName)
        assertNull(viewModel.uiState.value.errorMessage, "No error banner should display on canceled hash")
        assertNull(viewModel.uiState.value.activeVerdict)
    }

    @Test
    fun testTemporaryFileCleanedOnCompletionAndCancellation() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val threatRepo = FakeThreatIntelRepository()
        val analyzeUseCase = AnalyzeIndicatorUseCase(threatRepo)
        val introUseCase = SetIntroCompletedUseCase(settingsRepo)

        val cleanedFiles = mutableListOf<String>()
        val fakeCleaner = object : TemporaryFileCleaner {
            override fun cleanIfTemporary(filePath: String): Boolean {
                cleanedFiles.add(filePath)
                return true
            }
        }
        val fakeHasher = object : StreamHasher {
            override suspend fun hashFile(filePath: String): FileHashResult =
                FileHashResult("abc", "def", 100L)
        }

        val viewModel = HomeViewModel(analyzeUseCase, introUseCase, settingsRepo, streamHasher = fakeHasher, temporaryFileCleaner = fakeCleaner)

        // Dropping a temporary file (isTemporary = true)
        viewModel.onEvent(HomeEvent.FileDropped("/cache/picked_123_sample.bin", "sample.bin", isTemporary = true))
        advanceUntilIdle()

        assertTrue(cleanedFiles.contains("/cache/picked_123_sample.bin"), "Temporary file must be cleaned on completion")

        // Dropping a user Desktop file (isTemporary = false)
        viewModel.onEvent(HomeEvent.FileDropped("C:\\Users\\Desktop\\sample.bin", "sample.bin", isTemporary = false))
        advanceUntilIdle()

        assertEquals(1, cleanedFiles.size, "User original desktop file must NEVER be cleaned")
    }

    @Test
    fun testInvalidTabIndexClamped() {
        val settingsRepo = FakeSettingsRepository()
        val threatRepo = FakeThreatIntelRepository()
        val analyzeUseCase = AnalyzeIndicatorUseCase(threatRepo)
        val introUseCase = SetIntroCompletedUseCase(settingsRepo)
        val viewModel = HomeViewModel(analyzeUseCase, introUseCase, settingsRepo)

        // Tab 3 clamps to 2 (Settings)
        viewModel.onEvent(HomeEvent.SelectBottomNavTab(3))
        assertEquals(2, viewModel.uiState.value.selectedBottomNavTab, "Tab 3 must clamp to Tab 2 (Settings)")

        // Out-of-bounds large tab index clamps to 2 (Settings)
        viewModel.onEvent(HomeEvent.SelectBottomNavTab(99))
        assertEquals(2, viewModel.uiState.value.selectedBottomNavTab, "Large tab index must clamp to Tab 2 (Settings)")

        // Negative tab index clamps to 0 (Scanner)
        viewModel.onEvent(HomeEvent.SelectBottomNavTab(-1))
        assertEquals(0, viewModel.uiState.value.selectedBottomNavTab, "Negative tab index must clamp to Tab 0 (Scanner)")

        // Collapse provider deck first
        viewModel.onEvent(HomeEvent.ToggleProviderDeck)
        assertFalse(viewModel.uiState.value.isProviderDeckExpanded)
        // Tab 1 selects History without forcing isProviderDeckExpanded = true
        viewModel.onEvent(HomeEvent.SelectBottomNavTab(1))
        assertEquals(1, viewModel.uiState.value.selectedBottomNavTab)
        assertFalse(viewModel.uiState.value.isProviderDeckExpanded, "History tab selection must not mutate isProviderDeckExpanded")
    }

    @Test
    fun testSharedPayloadErrorMessageSetsErrorBanner() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val threatRepo = FakeThreatIntelRepository()
        val analyzeUseCase = AnalyzeIndicatorUseCase(threatRepo)
        val introUseCase = SetIntroCompletedUseCase(settingsRepo)
        val viewModel = HomeViewModel(analyzeUseCase, introUseCase, settingsRepo)

        // Start on Tab 1
        viewModel.onEvent(HomeEvent.SelectBottomNavTab(1))
        assertEquals(1, viewModel.uiState.value.selectedBottomNavTab)

        // Process error payload (e.g. from Desktop folder drop)
        viewModel.handleIncomingPayload(SharedPayload(errorMessage = "Cannot analyze folder 'test'."))

        assertEquals(0, viewModel.uiState.value.selectedBottomNavTab, "Must navigate to Scanner tab 0")
        assertEquals("Cannot analyze folder 'test'.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun testErrorMessageAutoDismissesAfterTimer() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val threatRepo = FakeThreatIntelRepository()
        val analyzeUseCase = AnalyzeIndicatorUseCase(threatRepo)
        val introUseCase = SetIntroCompletedUseCase(settingsRepo)
        val viewModel = HomeViewModel(analyzeUseCase, introUseCase, settingsRepo)

        viewModel.onEvent(HomeEvent.QueryChanged(""))
        viewModel.onEvent(HomeEvent.AnalyzeClicked)

        assertNotNull(viewModel.uiState.value.errorMessage, "Error must be immediately present")

        // Advance virtual clock past the 4000ms auto-dismiss delay
        advanceTimeBy(4001L)

        assertNull(viewModel.uiState.value.errorMessage, "Error must be automatically dismissed after timer")
    }

    @Test
    fun testQueryChangedCancelsErrorDismissTimerAndClearsImmediately() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val threatRepo = FakeThreatIntelRepository()
        val analyzeUseCase = AnalyzeIndicatorUseCase(threatRepo)
        val introUseCase = SetIntroCompletedUseCase(settingsRepo)
        val viewModel = HomeViewModel(analyzeUseCase, introUseCase, settingsRepo)

        viewModel.onEvent(HomeEvent.QueryChanged(""))
        viewModel.onEvent(HomeEvent.AnalyzeClicked)
        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.onEvent(HomeEvent.QueryChanged("8.8.8.8"))
        assertNull(viewModel.uiState.value.errorMessage, "Typing must clear error immediately")
    }

    @Test
    fun testOpenAndCloseVisionLens() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val threatRepo = FakeThreatIntelRepository()
        val analyzeUseCase = AnalyzeIndicatorUseCase(threatRepo)
        val introUseCase = SetIntroCompletedUseCase(settingsRepo)
        val viewModel = HomeViewModel(analyzeUseCase, introUseCase, settingsRepo)

        assertFalse(viewModel.uiState.value.isVisionLensOpen)

        viewModel.onEvent(HomeEvent.OpenVisionLens)
        assertTrue(viewModel.uiState.value.isVisionLensOpen)

        viewModel.onEvent(HomeEvent.CloseVisionLens)
        assertFalse(viewModel.uiState.value.isVisionLensOpen)
    }

    @Test
    fun testVisionPayloadScannedSetsDefangedInputAndLaunchesTriage() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val threatRepo = FakeThreatIntelRepository()
        val analyzeUseCase = AnalyzeIndicatorUseCase(threatRepo)
        val introUseCase = SetIntroCompletedUseCase(settingsRepo)
        val viewModel = HomeViewModel(analyzeUseCase, introUseCase, settingsRepo)

        viewModel.onEvent(HomeEvent.OpenVisionLens)
        assertTrue(viewModel.uiState.value.isVisionLensOpen)

        val payload = BarcodePayload(
            rawText = "https://185.220.101.5/malware.exe",
            defangedText = "hxxps[://]185[.]220[.]101[.]5/malware.exe",
            type = BarcodeType.QR_CODE,
            detectedIocType = "URL",
            quishingRisk = QuishingRisk.HIGH_RISK,
            riskReason = "Direct executable",
            unmaskedTargetUrl = null
        )

        viewModel.onEvent(HomeEvent.VisionPayloadScanned(payload))

        // Vision dialog must be closed
        assertFalse(viewModel.uiState.value.isVisionLensOpen)
        // Search bar query input must display defanged text for safe viewing
        assertEquals("hxxps[://]185[.]220[.]101[.]5/malware.exe", viewModel.uiState.value.queryInput)
        // Active payload must be recorded
        assertEquals(payload, viewModel.uiState.value.activeVisionPayload)

        advanceUntilIdle()

        // Threat intel triage must be finished and verdict active
        assertNotNull(viewModel.uiState.value.activeVerdict)
        assertEquals("https://185.220.101.5/malware.exe", viewModel.uiState.value.activeVerdict?.indicator?.normalizedValue)
    }

    @Test
    fun testVisionPayloadScannedWithUnmaskedTargetTriagesUnmaskedUrl() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val threatRepo = FakeThreatIntelRepository()
        val analyzeUseCase = AnalyzeIndicatorUseCase(threatRepo)
        val introUseCase = SetIntroCompletedUseCase(settingsRepo)
        val viewModel = HomeViewModel(analyzeUseCase, introUseCase, settingsRepo)

        val payload = BarcodePayload(
            rawText = "https://login.live.com/auth?dest=aHR0cHM6Ly9ldmlsLnh5ei9wd24=",
            defangedText = "hxxps[://]login[.]live[.]com/auth?dest=aHR0cHM6Ly9ldmlsLnh5ei9wd24=",
            type = BarcodeType.QR_CODE,
            detectedIocType = "URL",
            quishingRisk = QuishingRisk.HIGH_RISK,
            riskReason = "Nested Base64 redirection",
            unmaskedTargetUrl = "https://evil.xyz/pwn"
        )

        viewModel.onEvent(HomeEvent.VisionPayloadScanned(payload))

        assertFalse(viewModel.uiState.value.isVisionLensOpen)
        assertEquals("hxxps[://]login[.]live[.]com/auth?dest=aHR0cHM6Ly9ldmlsLnh5ei9wd24=", viewModel.uiState.value.queryInput)

        advanceUntilIdle()

        // Verdict must triage the unmasked destination target
        assertNotNull(viewModel.uiState.value.activeVerdict)
        assertEquals("https://evil.xyz/pwn", viewModel.uiState.value.activeVerdict?.indicator?.normalizedValue)
        assertEquals(VerdictLevel.MALICIOUS, viewModel.uiState.value.activeVerdict?.overallVerdict)
    }
}

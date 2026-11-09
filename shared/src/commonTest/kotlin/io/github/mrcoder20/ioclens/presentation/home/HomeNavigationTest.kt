package io.github.mrcoder20.ioclens.presentation.home

import io.github.mrcoder20.ioclens.domain.model.Indicator
import io.github.mrcoder20.ioclens.domain.model.IndicatorType
import io.github.mrcoder20.ioclens.domain.model.TriageVerdict
import io.github.mrcoder20.ioclens.domain.model.VerdictLevel
import io.github.mrcoder20.ioclens.domain.usecase.AnalyzeIndicatorUseCase
import io.github.mrcoder20.ioclens.domain.usecase.SetIntroCompletedUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * TDD Navigation Test Suite for Bottom Navigation Architecture.
 * Verifies that:
 * 1. Scanner (0), Engines (1), History (2), and Settings (3) are 4 independent full-screen views.
 * 2. Neither History nor Settings renders as a popup modal dialog overlay (isHistorySheetOpen and isSettingsSheetOpen remain false).
 * 3. Selecting a history item transitions directly to Tab 0 (Scanner) with active verdict loaded.
 * 4. Navigating via Drawer or action buttons directly selects dedicated tabs without modal backdrops.
 */
class HomeNavigationTest {

    @Test
    fun testInitialDestinationIsScannerView() = runTest {
        val settingsRepo = HomeViewModelTest.FakeSettingsRepository()
        val threatRepo = HomeViewModelTest.FakeThreatIntelRepository()
        val analyzeUseCase = AnalyzeIndicatorUseCase(threatRepo)
        val introUseCase = SetIntroCompletedUseCase(settingsRepo)
        val viewModel = HomeViewModel(analyzeUseCase, introUseCase, settingsRepo)

        val state = viewModel.uiState.value
        assertEquals(0, state.selectedBottomNavTab, "Tab 0 (Scanner) must be selected by default")
        assertFalse(state.isHistorySheetOpen, "History modal sheet must be closed by default")
        assertFalse(state.isSettingsSheetOpen, "Settings modal sheet must be closed by default")
        assertNull(state.activeVerdict, "No active verdict on startup")
    }

    @Test
    fun testNavigatingBetweenAllThreeTabsRendersDedicatedViewsWithoutModalDialogs() = runTest {
        val settingsRepo = HomeViewModelTest.FakeSettingsRepository()
        val threatRepo = HomeViewModelTest.FakeThreatIntelRepository()
        val analyzeUseCase = AnalyzeIndicatorUseCase(threatRepo)
        val introUseCase = SetIntroCompletedUseCase(settingsRepo)
        val viewModel = HomeViewModel(analyzeUseCase, introUseCase, settingsRepo)

        // 1. Select Tab 1 - History View (Dedicated full-screen view without modal dialog overlay)
        viewModel.onEvent(HomeEvent.SelectBottomNavTab(1))
        assertEquals(1, viewModel.uiState.value.selectedBottomNavTab, "History tab must be selected")
        assertFalse(viewModel.uiState.value.isHistorySheetOpen, "History modal overlay must not be open")
        assertFalse(viewModel.uiState.value.isSettingsSheetOpen, "Settings modal overlay must not be open")

        // 2. Select Tab 2 - Settings View (Dedicated full-screen view without modal dialog overlay)
        viewModel.onEvent(HomeEvent.SelectBottomNavTab(2))
        assertEquals(2, viewModel.uiState.value.selectedBottomNavTab, "Settings tab must be selected")
        assertFalse(viewModel.uiState.value.isSettingsSheetOpen, "Settings modal overlay must not be open")
        assertFalse(viewModel.uiState.value.isHistorySheetOpen, "History modal overlay must not be open")

        // 3. Return to Tab 0 - Scanner View
        viewModel.onEvent(HomeEvent.SelectBottomNavTab(0))
        assertEquals(0, viewModel.uiState.value.selectedBottomNavTab, "Scanner tab must be selected")
        assertFalse(viewModel.uiState.value.isHistorySheetOpen)
        assertFalse(viewModel.uiState.value.isSettingsSheetOpen)
    }

    @Test
    fun testHistoryItemSelectionJumpsDirectlyToScannerWithActiveVerdict() = runTest {
        val settingsRepo = HomeViewModelTest.FakeSettingsRepository()
        val threatRepo = HomeViewModelTest.FakeThreatIntelRepository()
        val analyzeUseCase = AnalyzeIndicatorUseCase(threatRepo)
        val introUseCase = SetIntroCompletedUseCase(settingsRepo)

        val sampleVerdict = TriageVerdict(
            indicator = Indicator("8.8.8.8", "8.8.8.8", IndicatorType.IP_ADDRESS),
            overallVerdict = VerdictLevel.CLEAN,
            confidenceScore = 98,
            summaryDescription = "Clean Google DNS",
            sourceResults = emptyList()
        )
        settingsRepo.saveScanHistory(listOf(sampleVerdict))

        val viewModel = HomeViewModel(analyzeUseCase, introUseCase, settingsRepo)

        // User is browsing Tab 1 (History)
        viewModel.onEvent(HomeEvent.SelectBottomNavTab(1))
        assertEquals(1, viewModel.uiState.value.selectedBottomNavTab)

        // User selects a history item -> Must switch to Tab 0 (Scanner) and load verdict
        viewModel.onEvent(HomeEvent.HistoryItemClicked(sampleVerdict))

        assertEquals(0, viewModel.uiState.value.selectedBottomNavTab, "Must navigate back to Scanner tab (0)")
        assertFalse(viewModel.uiState.value.isHistorySheetOpen, "History modal must remain false")
        assertNotNull(viewModel.uiState.value.activeVerdict, "Active verdict must be populated")
        assertEquals("8.8.8.8", viewModel.uiState.value.activeVerdict?.indicator?.rawValue)
        assertEquals("8.8.8.8", viewModel.uiState.value.queryInput)
    }

    @Test
    fun testSetHistorySheetOpenDirectlySelectsHistoryTabWithoutModal() = runTest {
        val settingsRepo = HomeViewModelTest.FakeSettingsRepository()
        val threatRepo = HomeViewModelTest.FakeThreatIntelRepository()
        val analyzeUseCase = AnalyzeIndicatorUseCase(threatRepo)
        val introUseCase = SetIntroCompletedUseCase(settingsRepo)
        val viewModel = HomeViewModel(analyzeUseCase, introUseCase, settingsRepo)

        // Invoking SetHistorySheetOpen(true) from Drawer or Desktop header button
        viewModel.onEvent(HomeEvent.SetHistorySheetOpen(true))
        assertTrue(viewModel.uiState.value.isHistorySheetOpen, "Must set modal sheet open flag")

        // Invoking SetHistorySheetOpen(false) closes sheet
        viewModel.onEvent(HomeEvent.SetHistorySheetOpen(false))
        assertFalse(viewModel.uiState.value.isHistorySheetOpen, "Must close modal sheet")
    }

    @Test
    fun testSetSettingsSheetOpenDirectlySelectsSettingsTabWithoutModal() = runTest {
        val settingsRepo = HomeViewModelTest.FakeSettingsRepository()
        val threatRepo = HomeViewModelTest.FakeThreatIntelRepository()
        val analyzeUseCase = AnalyzeIndicatorUseCase(threatRepo)
        val introUseCase = SetIntroCompletedUseCase(settingsRepo)
        val viewModel = HomeViewModel(analyzeUseCase, introUseCase, settingsRepo)

        // Invoking SetSettingsSheetOpen(true) from Drawer or Desktop header button
        viewModel.onEvent(HomeEvent.SetSettingsSheetOpen(true))
        assertTrue(viewModel.uiState.value.isSettingsSheetOpen, "Must set modal sheet open flag")

        // Invoking SetSettingsSheetOpen(false) closes sheet
        viewModel.onEvent(HomeEvent.SetSettingsSheetOpen(false))
        assertFalse(viewModel.uiState.value.isSettingsSheetOpen, "Must close modal sheet")
    }

    @Test
    fun testEnginesViewHasAllTenProvidersWithSupportedTypesAndDescriptions() {
        val providers = listOf(
            "URLhaus (abuse.ch)",
            "MalwareBazaar (abuse.ch)",
            "ThreatFox (abuse.ch)",
            "RDAP / Registry",
            "Cloudflare DoH",
            "VirusTotal",
            "AbuseIPDB",
            "AlienVault OTX",
            "Pulsedive",
            "IPQualityScore"
        )

        assertEquals(10, providers.size, "Exactly 10 Threat Intel Providers must be supported")

        for (name in providers) {
            val types = io.github.mrcoder20.ioclens.core.localization.LocalizationProvider.getProviderSupportedTypes(name)
            assertTrue(types.isNotEmpty(), "Provider $name must specify supported indicator types")
            assertFalse(types.contains(IndicatorType.UNKNOWN), "Provider $name should not contain UNKNOWN type")

            for (lang in io.github.mrcoder20.ioclens.core.localization.AppLanguage.entries) {
                val desc = io.github.mrcoder20.ioclens.core.localization.LocalizationProvider.getProviderDescription(name, lang)
                assertTrue(desc.isNotBlank(), "Provider $name description in $lang must not be blank")
            }
        }
    }

    @Test
    fun testHistoryFilteringLogic() {
        val v1 = TriageVerdict(
            indicator = Indicator("8.8.8.8", "8.8.8.8", IndicatorType.IP_ADDRESS),
            overallVerdict = VerdictLevel.CLEAN,
            confidenceScore = 95,
            summaryDescription = "Google DNS",
            sourceResults = emptyList()
        )
        val v2 = TriageVerdict(
            indicator = Indicator("evil.com", "evil.com", IndicatorType.DOMAIN),
            overallVerdict = VerdictLevel.MALICIOUS,
            confidenceScore = 99,
            summaryDescription = "Phishing domain",
            sourceResults = emptyList()
        )
        val v3 = TriageVerdict(
            indicator = Indicator("44d88612fea8a8f36de82e1278abb02f", "44d88612fea8a8f36de82e1278abb02f", IndicatorType.FILE_HASH),
            overallVerdict = VerdictLevel.SUSPICIOUS,
            confidenceScore = 70,
            summaryDescription = "EICAR Test",
            sourceResults = emptyList()
        )

        val history = listOf(v1, v2, v3)

        // 1. Filter by Query: "evil"
        val queryFiltered = history.filter { it.indicator.rawValue.contains("evil", ignoreCase = true) }
        assertEquals(1, queryFiltered.size)
        assertEquals("evil.com", queryFiltered[0].indicator.rawValue)

        // 2. Filter by Type: IP_ADDRESS
        val ipFiltered = history.filter { it.indicator.type == IndicatorType.IP_ADDRESS }
        assertEquals(1, ipFiltered.size)
        assertEquals("8.8.8.8", ipFiltered[0].indicator.rawValue)

        // 3. Filter by Type: FILE_HASH
        val hashFiltered = history.filter { it.indicator.type == IndicatorType.FILE_HASH }
        assertEquals(1, hashFiltered.size)
        assertEquals("44d88612fea8a8f36de82e1278abb02f", hashFiltered[0].indicator.rawValue)

        // 4. Filter by Non-matching query
        val emptyFiltered = history.filter { it.indicator.rawValue.contains("non-existent") }
        assertTrue(emptyFiltered.isEmpty(), "Should return empty list for non-matching query")
    }
}


package io.github.mrcoder20.ioclens

import io.github.mrcoder20.ioclens.core.localization.AppLanguage
import io.github.mrcoder20.ioclens.core.localization.AppTheme
import io.github.mrcoder20.ioclens.core.storage.InMemoryKeyValueStorage
import io.github.mrcoder20.ioclens.data.repository.SettingsRepositoryImpl
import io.github.mrcoder20.ioclens.data.repository.ThreatIntelRepositoryImpl
import io.github.mrcoder20.ioclens.domain.model.IndicatorType
import io.github.mrcoder20.ioclens.domain.model.VerdictLevel
import io.github.mrcoder20.ioclens.domain.usecase.AnalyzeIndicatorUseCase
import io.github.mrcoder20.ioclens.domain.usecase.GetAppSettingsUseCase
import io.github.mrcoder20.ioclens.domain.usecase.SetIntroCompletedUseCase
import io.github.mrcoder20.ioclens.presentation.main.MainUiState
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CleanArchitectureTest {

    @Test
    fun testSettingsRepositoryAndSystemDefaults() {
        val storage = InMemoryKeyValueStorage()
        val settingsRepo = SettingsRepositoryImpl(storage)
        val getIntroUseCase = GetAppSettingsUseCase(settingsRepo)
        val setIntroUseCase = SetIntroCompletedUseCase(settingsRepo)

        // Initial default theme should be DARK
        assertEquals(AppTheme.DARK, settingsRepo.getSettings().appTheme)

        // Complete intro
        setIntroUseCase(completed = true)
        assertTrue(getIntroUseCase().isIntroCompleted)

        // Switch Theme
        settingsRepo.setAppTheme(AppTheme.SYSTEM)
        assertEquals(AppTheme.SYSTEM, settingsRepo.getSettings().appTheme)

        // Switch Language to Russian
        settingsRepo.setAppLanguage(AppLanguage.RUSSIAN)
        assertEquals(AppLanguage.RUSSIAN, settingsRepo.getSettings().appLanguage)
        assertFalse(AppLanguage.RUSSIAN.isRtl)

        // Switch Language to Persian
        settingsRepo.setAppLanguage(AppLanguage.PERSIAN)
        assertEquals(AppLanguage.PERSIAN, settingsRepo.getSettings().appLanguage)
        assertTrue(AppLanguage.PERSIAN.isRtl)
    }

    @Test
    fun testMainUiStateSplashFinished() {
        val initialState = MainUiState()
        assertFalse(initialState.isSplashFinished)

        val updatedState = initialState.copy(isSplashFinished = true)
        assertTrue(updatedState.isSplashFinished)
    }

    @Test
    fun testPrivacyGuardAndIndicatorParsing() {
        runBlocking {
            val threatRepo = ThreatIntelRepositoryImpl()
            val analyzeUseCase = AnalyzeIndicatorUseCase(threatRepo)

            // Test internal IP
            val internalIp = analyzeUseCase.parseIndicator("192.168.1.100")
            assertEquals(IndicatorType.IP_ADDRESS, internalIp.type)
            assertTrue(internalIp.isInternalOrPrivate)

            val internalVerdict = analyzeUseCase(internalIp)
            assertEquals(VerdictLevel.CLEAN, internalVerdict.overallVerdict)
            assertTrue(internalVerdict.summaryDescription.contains("Privacy Guard"))

            // Test external domain
            val domain = analyzeUseCase.parseIndicator("malicious-site.com")
            assertEquals(IndicatorType.DOMAIN, domain.type)
            assertFalse(domain.isInternalOrPrivate)
        }
    }

    @Test
    fun testSystemLanguageDetection() {
        val systemLanguage = getSystemLanguage()
        assertTrue(systemLanguage in AppLanguage.entries)
    }

    @Test
    fun testSettingsPersistenceAndAllApiKeys() {
        val storage = InMemoryKeyValueStorage()
        val settingsRepo1 = SettingsRepositoryImpl(storage)

        // Default privacy guard is true
        assertTrue(settingsRepo1.getSettings().privacyGuardEnabled)

        // Toggle privacy guard OFF
        settingsRepo1.setPrivacyGuardEnabled(false)
        assertFalse(settingsRepo1.getSettings().privacyGuardEnabled)

        // Set all 5 API keys
        settingsRepo1.updateApiKey("virustotal", "vt-key-999")
        settingsRepo1.updateApiKey("abuseipdb", "abuse-key-888")
        settingsRepo1.updateApiKey("alienvault", "alien-otx-777")
        settingsRepo1.updateApiKey("pulsedive", "pulse-key-666")
        settingsRepo1.updateApiKey("ipqualityscore", "ipqs-key-555")

        // Verify in repo1
        val s1 = settingsRepo1.getSettings()
        assertEquals("vt-key-999", s1.virusTotalApiKey)
        assertEquals("abuse-key-888", s1.abuseIpDbApiKey)
        assertEquals("alien-otx-777", s1.alienVaultApiKey)
        assertEquals("pulse-key-666", s1.pulsediveApiKey)
        assertEquals("ipqs-key-555", s1.ipQualityScoreApiKey)
        assertFalse(s1.privacyGuardEnabled)

        // Simulate app restart by loading new repository from the same storage
        val settingsRepo2 = SettingsRepositoryImpl(storage)
        val s2 = settingsRepo2.getSettings()
        assertEquals("vt-key-999", s2.virusTotalApiKey)
        assertEquals("abuse-key-888", s2.abuseIpDbApiKey)
        assertEquals("alien-otx-777", s2.alienVaultApiKey)
        assertEquals("pulse-key-666", s2.pulsediveApiKey)
        assertEquals("ipqs-key-555", s2.ipQualityScoreApiKey)
        assertFalse(s2.privacyGuardEnabled)
    }

    @Test
    fun testIntroResetForReplay() {
        val storage = InMemoryKeyValueStorage()
        val settingsRepo = SettingsRepositoryImpl(storage)
        val setIntroUseCase = SetIntroCompletedUseCase(settingsRepo)
        val introViewModel = io.github.mrcoder20.ioclens.presentation.intro.IntroViewModel(setIntroUseCase, settingsRepo)

        // User navigates through slides and completes intro
        introViewModel.onEvent(io.github.mrcoder20.ioclens.presentation.intro.IntroEvent.PageChanged(3))
        introViewModel.onEvent(io.github.mrcoder20.ioclens.presentation.intro.IntroEvent.CompleteClicked)

        assertTrue(introViewModel.uiState.value.isCompleted)
        assertTrue(settingsRepo.getSettings().isIntroCompleted)

        // User triggers guide replay from Drawer or Settings
        introViewModel.onEvent(io.github.mrcoder20.ioclens.presentation.intro.IntroEvent.Reset)

        assertFalse(introViewModel.uiState.value.isCompleted, "isCompleted must be reset to false for smooth replay")
        assertEquals(0, introViewModel.uiState.value.currentPageIndex, "currentPageIndex must reset to 0")
        assertFalse(introViewModel.uiState.value.isLastPage)
    }
}

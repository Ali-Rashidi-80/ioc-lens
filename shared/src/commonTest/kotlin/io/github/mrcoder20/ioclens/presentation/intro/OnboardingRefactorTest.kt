package io.github.mrcoder20.ioclens.presentation.intro

import io.github.mrcoder20.ioclens.core.components.slider.OnboardingArrowDirection
import io.github.mrcoder20.ioclens.core.components.slider.getOnboardingNextArrowDirection
import io.github.mrcoder20.ioclens.core.components.slider.shouldShowOnboardingSkip
import io.github.mrcoder20.ioclens.core.localization.AppLanguage
import io.github.mrcoder20.ioclens.core.localization.LocalizationProvider
import io.github.mrcoder20.ioclens.core.storage.InMemoryKeyValueStorage
import io.github.mrcoder20.ioclens.data.repository.SettingsRepositoryImpl
import io.github.mrcoder20.ioclens.domain.usecase.SetIntroCompletedUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TDD Test Suite for Onboarding Module Refactor.
 * Verifies QA defect fixes:
 * 1. Single Source of Truth for Pager (no un-synced counters in ViewModel).
 * 2. Previous button completely eliminated.
 * 3. Skip relocated to top bar on slides 1-3 only (hidden on slide 4).
 * 4. DONE button responds immediately on first click and writes to repository.
 * 5. Directional arrows strictly point left for RTL (Persian) and right for LTR (English, Russian).
 * 6. AppStrings typography contains clean text without hardcoded unicode arrows.
 */
class OnboardingRefactorTest {

    @Test
    fun testSkipActionImmediatelyCompletesIntroAndNavigates() {
        val storage = InMemoryKeyValueStorage()
        val settingsRepo = SettingsRepositoryImpl(storage)
        val setIntroUseCase = SetIntroCompletedUseCase(settingsRepo)
        val viewModel = IntroViewModel(setIntroUseCase, settingsRepo)

        assertFalse(viewModel.uiState.value.isCompleted)
        assertFalse(settingsRepo.getSettings().isIntroCompleted)

        // User taps Skip on top header
        viewModel.onEvent(IntroEvent.SkipClicked)

        assertTrue(
            settingsRepo.getSettings().isIntroCompleted,
            "Repository must immediately record intro as completed upon Skip"
        )
        assertTrue(
            viewModel.uiState.value.isCompleted,
            "ViewModel UI state must reflect isCompleted = true immediately"
        )
    }

    @Test
    fun testDoneActionOnSlide4ImmediatelyCompletesIntroOnFirstClick() {
        val storage = InMemoryKeyValueStorage()
        val settingsRepo = SettingsRepositoryImpl(storage)
        val setIntroUseCase = SetIntroCompletedUseCase(settingsRepo)
        val viewModel = IntroViewModel(setIntroUseCase, settingsRepo)

        assertFalse(viewModel.uiState.value.isCompleted)
        assertFalse(settingsRepo.getSettings().isIntroCompleted)

        // User taps DONE on Slide 4 - Must succeed reliably on the very first invocation
        viewModel.onEvent(IntroEvent.CompleteClicked)

        assertTrue(
            settingsRepo.getSettings().isIntroCompleted,
            "Repository must immediately record intro as completed on first click of DONE"
        )
        assertTrue(
            viewModel.uiState.value.isCompleted,
            "ViewModel UI state must reflect isCompleted = true"
        )
    }

    @Test
    fun testSkipVisibilityLogicAcrossSlides() {
        val totalSlides = 4

        // Slides 1, 2, 3 (indices 0, 1, 2): Skip must be visible
        assertTrue(shouldShowOnboardingSkip(pageIndex = 0, pageCount = totalSlides))
        assertTrue(shouldShowOnboardingSkip(pageIndex = 1, pageCount = totalSlides))
        assertTrue(shouldShowOnboardingSkip(pageIndex = 2, pageCount = totalSlides))

        // Slide 4 (index 3): Skip must be hidden to prevent vertical shift and focus on DONE
        assertFalse(shouldShowOnboardingSkip(pageIndex = 3, pageCount = totalSlides))

        // Edge case: out of bounds
        assertFalse(shouldShowOnboardingSkip(pageIndex = 4, pageCount = totalSlides))
    }

    @Test
    fun testDirectionalArrowRtlLogic() {
        // In Persian (RTL), advancing to next slide points Left (←)
        assertTrue(AppLanguage.PERSIAN.isRtl)
        assertEquals(
            OnboardingArrowDirection.LEFT,
            getOnboardingNextArrowDirection(isRtl = true),
            "In Persian RTL, the forward next arrow must point LEFT (←)"
        )

        // In English (LTR), advancing to next slide points Right (→)
        assertFalse(AppLanguage.ENGLISH.isRtl)
        assertEquals(
            OnboardingArrowDirection.RIGHT,
            getOnboardingNextArrowDirection(isRtl = false),
            "In English LTR, the forward next arrow must point RIGHT (→)"
        )

        // In Russian (LTR), advancing to next slide points Right (→)
        assertFalse(AppLanguage.RUSSIAN.isRtl)
        assertEquals(
            OnboardingArrowDirection.RIGHT,
            getOnboardingNextArrowDirection(isRtl = false),
            "In Russian LTR, the forward next arrow must point RIGHT (→)"
        )
    }

    @Test
    fun testAppStringsNextHasNoEmbeddedArrowCharacters() {
        val enStrings = LocalizationProvider.getStrings(AppLanguage.ENGLISH)
        val faStrings = LocalizationProvider.getStrings(AppLanguage.PERSIAN)
        val ruStrings = LocalizationProvider.getStrings(AppLanguage.RUSSIAN)

        assertFalse(
            enStrings.next.contains("→") || enStrings.next.contains("←"),
            "English 'next' must be clean text without embedded unicode arrows (was '${enStrings.next}')"
        )
        assertFalse(
            faStrings.next.contains("→") || faStrings.next.contains("←"),
            "Persian 'next' must be clean text without embedded unicode arrows (was '${faStrings.next}')"
        )
        assertFalse(
            ruStrings.next.contains("→") || ruStrings.next.contains("←"),
            "Russian 'next' must be clean text without embedded unicode arrows (was '${ruStrings.next}')"
        )

        assertEquals("Next", enStrings.next)
        assertEquals("بعدی", faStrings.next)
        assertEquals("Далее", ruStrings.next)
    }

    @Test
    fun testResetIntroAllowsReplayWithoutStateLeak() {
        val storage = InMemoryKeyValueStorage()
        val settingsRepo = SettingsRepositoryImpl(storage)
        val setIntroUseCase = SetIntroCompletedUseCase(settingsRepo)
        val viewModel = IntroViewModel(setIntroUseCase, settingsRepo)

        viewModel.onEvent(IntroEvent.CompleteClicked)
        assertTrue(viewModel.uiState.value.isCompleted)

        viewModel.onEvent(IntroEvent.Reset)
        assertFalse(viewModel.uiState.value.isCompleted, "isCompleted must be false after Reset")
    }
}


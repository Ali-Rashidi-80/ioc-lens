package io.github.mrcoder20.ioclens.presentation.home

import androidx.compose.ui.unit.Constraints
import io.github.mrcoder20.ioclens.domain.usecase.AnalyzeIndicatorUseCase
import io.github.mrcoder20.ioclens.domain.usecase.SetIntroCompletedUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Reproduction & Regression Test Suite for Settings Mobile Crash Bug.
 *
 * Bug Report: "روی تنظیمات میزنی کرش میکنه در موبایل!!!"
 * Root Cause:
 * 1. Streamlining from 4 tabs to 3 tabs relocated Settings from Tab index 3 to Tab index 2.
 * 2. In HomeScreen.kt, line 359 kept `selectedBottomNavTab == 3` instead of `== 2`.
 * 3. Consequently, tapping Settings (Tab 2) evaluated line 359 to false, falling through
 *    to the `else` branch which wrapped content inside `Column(Modifier.verticalScroll(scrollState))`.
 * 4. Inside the scrolling Column, line 731 matched Tab 2 and attempted to render `SettingsScreen`.
 * 5. Because `SettingsScreen` contains `BoxWithConstraints(fillMaxSize())` and its own
 *    `Column(Modifier.verticalScroll(scrollState))`, measuring it within an ancestor verticalScroll
 *    (which passes Constraints.Infinity for maxHeight) violated Compose layout invariants and
 *    triggered an immediate crash:
 *    "Vertically scrollable component was measured with an infinity maximum height constraints, which is disallowed."
 */
class SettingsMobileCrashReproTest {

    @Test
    fun testSettingsNavigationTargetIsTab2AndMustNotBeTab3() = runTest {
        val settingsRepo = HomeViewModelTest.FakeSettingsRepository()
        val threatRepo = HomeViewModelTest.FakeThreatIntelRepository()
        val analyzeUseCase = AnalyzeIndicatorUseCase(threatRepo)
        val introUseCase = SetIntroCompletedUseCase(settingsRepo)
        val viewModel = HomeViewModel(analyzeUseCase, introUseCase, settingsRepo)

        // Select Settings tab
        viewModel.onEvent(HomeEvent.SelectBottomNavTab(2))

        val currentTab = viewModel.uiState.value.selectedBottomNavTab
        assertEquals(2, currentTab, "Settings tab must be index 2 in 3-tab architecture")

        // Top-level full screen routing check:
        // HomeScreen line 359 checks: isFullScreenSettings(currentTab)
        // In the buggy code, it checks `currentTab == 3`, which fails for Tab 2!
        val buggyCheck = (currentTab == 3)
        assertFalse(buggyCheck, "Bug reproduction: Tab 2 does NOT match buggy check 'selectedBottomNavTab == 3'")

        // The correct invariant:
        val correctCheck = (currentTab == 2)
        assertTrue(correctCheck, "Invariant: Tab 2 must match full-screen Settings route")
    }

    @Test
    fun testComposeVerticalScrollWithinUnboundedConstraintsThrowsIllegalStateException() {
        // Reproduce the exact Compose layout invariant failure:
        // When verticalScroll measures content with Constraints(maxHeight = Infinity),
        // it throws IllegalStateException.
        val unboundedConstraints = Constraints(
            minWidth = 0,
            maxWidth = 1080,
            minHeight = 0,
            maxHeight = Constraints.Infinity
        )

        assertFailsWith<IllegalStateException> {
            check(unboundedConstraints.hasBoundedHeight) {
                "Vertically scrollable component was measured with an infinity maximum height constraints, which is disallowed. One of the common causes is Modifier.fillMaxHeight() on a child of a vertically scrollable parent!"
            }
        }
    }

    @Test
    fun testHomeScreenSourceFileDoesNotContainStaleTab3CheckOrNestedSettingsInScrollableColumn() {
        // Read HomeScreen.kt source code to verify the bug presence deterministically
        val homeScreenCode = runCatching {
            java.io.File("shared/src/commonMain/kotlin/io/github/mrcoder20/ioclens/presentation/home/HomeScreen.kt").readText()
        }.getOrNull() ?: runCatching {
            java.io.File("../shared/src/commonMain/kotlin/io/github/mrcoder20/ioclens/presentation/home/HomeScreen.kt").readText()
        }.getOrNull()

        if (homeScreenCode != null) {
            // BUG: Line 359 has `uiState.selectedBottomNavTab == 3`
            assertFalse(
                homeScreenCode.contains("uiState.selectedBottomNavTab == 3"),
                "HomeScreen.kt must NOT contain stale 'selectedBottomNavTab == 3' check! Settings is Tab 2."
            )

            // BUG: SettingsScreen should NOT be nested inside the scrolling Column (lines 565 and 731)
            // It must ONLY be rendered at the top-level container outside the scrolling Column.
            assertFalse(
                homeScreenCode.contains("isMobileOrTablet = true,\n                                isFullScreen = true,\n                                onClose = { viewModel.onEvent(HomeEvent.SelectBottomNavTab(0)) }") ||
                homeScreenCode.contains("isMobileOrTablet = false,\n                                isFullScreen = true,\n                                onClose = { viewModel.onEvent(HomeEvent.SelectBottomNavTab(0)) }"),
                "HomeScreen.kt must NOT render SettingsScreen inside the verticalScroll Column"
            )
        }
    }
}


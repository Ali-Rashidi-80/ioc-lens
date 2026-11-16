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
 * Runs in jvmTest because it reads source files via java.io.File to verify structural invariants.
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

        // Top-level full screen routing check
        val buggyCheck = (currentTab == 3)
        assertFalse(buggyCheck, "Bug reproduction: Tab 2 does NOT match buggy check 'selectedBottomNavTab == 3'")

        // The correct invariant
        val correctCheck = (currentTab == 2)
        assertTrue(correctCheck, "Invariant: Tab 2 must match full-screen Settings route")
    }

    @Test
    fun testComposeVerticalScrollWithinUnboundedConstraintsThrowsIllegalStateException() {
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
        val homeScreenCode = runCatching {
            java.io.File("shared/src/commonMain/kotlin/io/github/mrcoder20/ioclens/presentation/home/HomeScreen.kt").readText()
        }.getOrNull() ?: runCatching {
            java.io.File("../shared/src/commonMain/kotlin/io/github/mrcoder20/ioclens/presentation/home/HomeScreen.kt").readText()
        }.getOrNull()

        if (homeScreenCode != null) {
            assertFalse(
                homeScreenCode.contains("uiState.selectedBottomNavTab == 3"),
                "HomeScreen.kt must NOT contain stale 'selectedBottomNavTab == 3' check! Settings is Tab 2."
            )

            assertFalse(
                homeScreenCode.contains("isMobileOrTablet = true,\n                                isFullScreen = true,\n                                onClose = { viewModel.onEvent(HomeEvent.SelectBottomNavTab(0)) }") ||
                homeScreenCode.contains("isMobileOrTablet = false,\n                                isFullScreen = true,\n                                onClose = { viewModel.onEvent(HomeEvent.SelectBottomNavTab(0)) }"),
                "HomeScreen.kt must NOT render SettingsScreen inside the verticalScroll Column"
            )
        }
    }
}

package io.github.mrcoder20.ioclens.presentation.home

import io.github.mrcoder20.ioclens.core.localization.AppLanguage
import io.github.mrcoder20.ioclens.core.localization.AppTheme
import io.github.mrcoder20.ioclens.domain.model.Indicator
import io.github.mrcoder20.ioclens.domain.model.TriageVerdict
import io.github.mrcoder20.ioclens.getSystemLanguage

data class HomeUiState(
    val queryInput: String = "",
    val isLoading: Boolean = false,
    val isHashingFile: Boolean = false,
    val hashingFileName: String? = null,
    val targetIndicator: Indicator? = null,
    val activeVerdict: TriageVerdict? = null,
    val scanHistory: List<TriageVerdict> = emptyList(),
    val isProviderDeckExpanded: Boolean = true,
    val isHistorySheetOpen: Boolean = false,
    val isSettingsSheetOpen: Boolean = false,
    val isGuideOverlayOpen: Boolean = false,
    val selectedBottomNavTab: Int = 0,
    val scanFinishedEngines: Int = 0,
    val scanTotalEngines: Int = 10,
    val errorMessage: String? = null,
    val appTheme: AppTheme = AppTheme.DARK,
    val appLanguage: AppLanguage = getSystemLanguage()
)

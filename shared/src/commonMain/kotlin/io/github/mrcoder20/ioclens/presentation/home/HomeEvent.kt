package io.github.mrcoder20.ioclens.presentation.home

import io.github.mrcoder20.ioclens.core.localization.AppLanguage
import io.github.mrcoder20.ioclens.core.localization.AppTheme
import io.github.mrcoder20.ioclens.domain.model.TriageVerdict

sealed interface HomeEvent {
    data class QueryChanged(val newQuery: String) : HomeEvent
    data class ThemeChanged(val theme: AppTheme) : HomeEvent
    data class LanguageChanged(val language: AppLanguage) : HomeEvent
    data class FileDropped(val filePath: String, val fileName: String) : HomeEvent
    data class HistoryItemClicked(val verdict: TriageVerdict) : HomeEvent
    data object AnalyzeClicked : HomeEvent
    data object CancelScan : HomeEvent
    data object ClearClicked : HomeEvent
    data object ToggleProviderDeck : HomeEvent
    data class SetHistorySheetOpen(val isOpen: Boolean) : HomeEvent
    data class SetSettingsSheetOpen(val isOpen: Boolean) : HomeEvent
    data class SetGuideOverlayOpen(val isOpen: Boolean) : HomeEvent
    data class SelectBottomNavTab(val tabIndex: Int) : HomeEvent
    data class DeleteHistoryItem(val verdict: TriageVerdict) : HomeEvent
    data object ClearHistory : HomeEvent
}

package io.github.mrcoder20.ioclens.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.mrcoder20.ioclens.core.localization.LocalizationProvider
import io.github.mrcoder20.ioclens.domain.model.TriageVerdict
import io.github.mrcoder20.ioclens.domain.repository.SettingsRepository
import io.github.mrcoder20.ioclens.domain.security.ChunkedStreamHasher
import io.github.mrcoder20.ioclens.domain.security.SharePayloadReceiver
import io.github.mrcoder20.ioclens.domain.security.SharedPayload
import io.github.mrcoder20.ioclens.domain.usecase.AnalyzeIndicatorUseCase
import io.github.mrcoder20.ioclens.domain.usecase.SetIntroCompletedUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(
    private val analyzeIndicatorUseCase: AnalyzeIndicatorUseCase,
    private val setIntroCompletedUseCase: SetIntroCompletedUseCase,
    private val settingsRepository: SettingsRepository,
    private val streamHasher: ChunkedStreamHasher = ChunkedStreamHasher()
) : ViewModel() {

    private val initialSettings = settingsRepository.getSettings()

    private val _uiState = MutableStateFlow(
        HomeUiState(
            appTheme = initialSettings.appTheme,
            appLanguage = initialSettings.appLanguage
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeSettings()
        observeSharePayloads()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settingsState.collect { settings ->
                _uiState.update {
                    it.copy(
                        appTheme = settings.appTheme,
                        appLanguage = settings.appLanguage
                    )
                }
            }
        }
    }

    private fun observeSharePayloads() {
        viewModelScope.launch {
            SharePayloadReceiver.incomingPayload.collect { payload ->
                if (payload != null) {
                    SharePayloadReceiver.consume()
                    handleIncomingPayload(payload)
                }
            }
        }
    }

    private fun handleIncomingPayload(payload: SharedPayload) {
        if (!payload.filePath.isNullOrBlank()) {
            onEvent(HomeEvent.FileDropped(payload.filePath, payload.fileName ?: "payload.bin"))
        } else if (!payload.textOrUrl.isNullOrBlank()) {
            _uiState.update { it.copy(queryInput = payload.textOrUrl) }
            analyzeQuery()
        }
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.QueryChanged -> {
                _uiState.update { it.copy(queryInput = event.newQuery, errorMessage = null) }
            }
            is HomeEvent.ThemeChanged -> {
                settingsRepository.setAppTheme(event.theme)
            }
            is HomeEvent.LanguageChanged -> {
                settingsRepository.setAppLanguage(event.language)
            }
            is HomeEvent.FileDropped -> {
                hashAndAnalyzeFile(event.filePath, event.fileName)
            }
            is HomeEvent.HistoryItemClicked -> {
                _uiState.update {
                    it.copy(
                        activeVerdict = event.verdict,
                        queryInput = event.verdict.indicator.rawValue,
                        isHistorySheetOpen = false
                    )
                }
            }
            HomeEvent.AnalyzeClicked -> {
                analyzeQuery()
            }
            HomeEvent.ClearClicked -> {
                _uiState.update { it.copy(queryInput = "", activeVerdict = null, errorMessage = null) }
            }
            HomeEvent.ToggleProviderDeck -> {
                _uiState.update { it.copy(isProviderDeckExpanded = !it.isProviderDeckExpanded) }
            }
            is HomeEvent.SetHistorySheetOpen -> {
                _uiState.update { it.copy(isHistorySheetOpen = event.isOpen) }
            }
            is HomeEvent.SetSettingsSheetOpen -> {
                _uiState.update { it.copy(isSettingsSheetOpen = event.isOpen) }
            }
            is HomeEvent.SetGuideOverlayOpen -> {
                _uiState.update { it.copy(isGuideOverlayOpen = event.isOpen) }
            }
            HomeEvent.ClearHistory -> {
                _uiState.update { it.copy(scanHistory = emptyList()) }
            }
        }
    }

    private fun hashAndAnalyzeFile(filePath: String, fileName: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isHashingFile = true,
                    hashingFileName = fileName,
                    errorMessage = null
                )
            }
            try {
                val hashResult = withContext(Dispatchers.Default) {
                    streamHasher.hashFile(filePath)
                }
                _uiState.update {
                    it.copy(
                        isHashingFile = false,
                        hashingFileName = null,
                        queryInput = hashResult.sha256
                    )
                }
                analyzeQuery()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isHashingFile = false,
                        hashingFileName = null,
                        errorMessage = "Stream Hashing Failed: ${e.message}"
                    )
                }
            }
        }
    }

    private fun analyzeQuery() {
        val input = _uiState.value.queryInput.trim()
        val strings = LocalizationProvider.getStrings(_uiState.value.appLanguage)

        if (input.isBlank()) {
            _uiState.update { it.copy(errorMessage = strings.enterIndicatorError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val indicator = analyzeIndicatorUseCase.parseIndicator(input)
                val verdict = analyzeIndicatorUseCase(indicator)
                _uiState.update { state ->
                    val updatedHistory = listOf(verdict) + state.scanHistory.filterNot {
                        it.indicator.normalizedValue == verdict.indicator.normalizedValue
                    }.take(19)
                    state.copy(
                        isLoading = false,
                        activeVerdict = verdict,
                        scanHistory = updatedHistory
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Analysis failed") }
            }
        }
    }
}

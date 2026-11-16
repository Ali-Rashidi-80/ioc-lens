package io.github.mrcoder20.ioclens.presentation.home
import kotlinx.coroutines.isActive
import io.github.mrcoder20.ioclens.core.components.PickedFile
import io.github.mrcoder20.ioclens.domain.model.Indicator
import io.github.mrcoder20.ioclens.domain.model.IndicatorType

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.mrcoder20.ioclens.core.localization.LocalizationProvider
import io.github.mrcoder20.ioclens.domain.model.TriageVerdict
import io.github.mrcoder20.ioclens.domain.repository.SettingsRepository
import io.github.mrcoder20.ioclens.domain.security.ChunkedStreamHasher
import io.github.mrcoder20.ioclens.domain.security.DefaultTemporaryFileCleaner
import io.github.mrcoder20.ioclens.domain.security.SharePayloadReceiver
import io.github.mrcoder20.ioclens.domain.security.SharedPayload
import io.github.mrcoder20.ioclens.domain.security.StreamHasher
import io.github.mrcoder20.ioclens.domain.security.TemporaryFileCleaner
import io.github.mrcoder20.ioclens.domain.usecase.AnalyzeIndicatorUseCase
import io.github.mrcoder20.ioclens.domain.usecase.SetIntroCompletedUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private val streamHasher: StreamHasher = ChunkedStreamHasher(),
    private val temporaryFileCleaner: TemporaryFileCleaner = DefaultTemporaryFileCleaner
) : ViewModel() {

    private val initialSettings = settingsRepository.getSettings()
    private val initialHistory = settingsRepository.getScanHistory()

    private val _uiState = MutableStateFlow(
        HomeUiState(
            appTheme = initialSettings.appTheme,
            appLanguage = initialSettings.appLanguage,
            scanHistory = initialHistory
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null
    private var hashJob: Job? = null
    private var currentGeneration: Long = 0L
    private var errorDismissJob: Job? = null
    private var queueIdSequence: Long = 0L

    private fun nextQueueId(index: Int = 0): String {
        queueIdSequence++
        return "queue_${queueIdSequence}_$index"
    }

    private fun setTimedErrorMessage(message: String?, autoDismissDelayMs: Long = 4000L) {
        errorDismissJob?.cancel()
        _uiState.update { it.copy(errorMessage = message) }
        if (!message.isNullOrBlank() && autoDismissDelayMs > 0) {
            errorDismissJob = viewModelScope.launch {
                delay(autoDismissDelayMs)
                _uiState.update { state ->
                    if (state.errorMessage == message) {
                        state.copy(errorMessage = null)
                    } else state
                }
            }
        }
    }

    private fun clearErrorMessage() {
        errorDismissJob?.cancel()
        errorDismissJob = null
        _uiState.update { it.copy(errorMessage = null) }
    }

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

    internal fun handleIncomingPayload(payload: SharedPayload) {
        // Automatically switch to Scanner view (Tab 0) and dismiss any open overlays
        _uiState.update {
            it.copy(
                selectedBottomNavTab = 0,
                isSettingsSheetOpen = false,
                isHistorySheetOpen = false
            )
        }
        if (!payload.errorMessage.isNullOrBlank()) {
            setTimedErrorMessage(payload.errorMessage)
            return
        }
        if (!payload.batchFiles.isNullOrEmpty()) {
            onEvent(HomeEvent.BatchFilesSelected(payload.batchFiles, payload.isTemporary))
            return
        }
        if (!payload.filePath.isNullOrBlank()) {
            onEvent(HomeEvent.FileDropped(payload.filePath, payload.fileName ?: "payload.bin", payload.isTemporary))
        } else if (!payload.textOrUrl.isNullOrBlank()) {
            val iocParser = io.github.mrcoder20.ioclens.domain.parser.IOCParser()
            val extracted = iocParser.extractFirstIoc(payload.textOrUrl)
            val queryText = extracted?.rawValue ?: payload.textOrUrl.trim()
            _uiState.update { it.copy(queryInput = queryText) }
            analyzeQuery()
        }
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.QueryChanged -> {
                clearErrorMessage()
                _uiState.update { it.copy(queryInput = event.newQuery) }
            }
            is HomeEvent.ThemeChanged -> {
                settingsRepository.setAppTheme(event.theme)
            }
            is HomeEvent.LanguageChanged -> {
                settingsRepository.setAppLanguage(event.language)
            }
            is HomeEvent.FileDropped -> {
                hashAndAnalyzeFile(event.filePath, event.fileName, event.isTemporary)
            }
            is HomeEvent.BatchFilesSelected -> {
                handleBatchFilesSelected(event.files, event.isTemporary)
            }
            HomeEvent.ClearFileQueue -> {
                clearFileQueue()
            }
            is HomeEvent.SelectQueueItemVerdict -> {
                event.item.verdict?.let { verdict ->
                    _uiState.update {
                        it.copy(
                            activeVerdict = verdict,
                            targetIndicator = verdict.indicator,
                            queryInput = event.item.sha256 ?: verdict.indicator.rawValue,
                            selectedBottomNavTab = 0
                        )
                    }
                }
            }
            is HomeEvent.HistoryItemClicked -> {
                _uiState.update {
                    it.copy(
                        activeVerdict = event.verdict,
                        targetIndicator = event.verdict.indicator,
                        queryInput = event.verdict.indicator.rawValue,
                        isHistorySheetOpen = false,
                        selectedBottomNavTab = 0
                    )
                }
            }
            HomeEvent.AnalyzeClicked -> {
                analyzeQuery()
            }
            HomeEvent.CancelScan -> {
                cancelScan()
            }
            HomeEvent.ClearClicked -> {
                cancelScan()
                clearErrorMessage()
                _uiState.update { it.copy(queryInput = "", targetIndicator = null, activeVerdict = null) }
            }
            HomeEvent.ToggleProviderDeck -> {
                _uiState.update { it.copy(isProviderDeckExpanded = !it.isProviderDeckExpanded) }
            }
            is HomeEvent.SelectBottomNavTab -> {
                val targetTab = event.tabIndex.coerceIn(0, 2)
                _uiState.update {
                    it.copy(
                        selectedBottomNavTab = targetTab,
                        isSettingsSheetOpen = false,
                        isHistorySheetOpen = false
                    )
                }
            }
            is HomeEvent.SetHistorySheetOpen -> {
                _uiState.update {
                    it.copy(
                        isHistorySheetOpen = event.isOpen,
                        selectedBottomNavTab = if (!event.isOpen && it.selectedBottomNavTab == 1) 0 else it.selectedBottomNavTab
                    )
                }
            }
            is HomeEvent.SetSettingsSheetOpen -> {
                _uiState.update {
                    it.copy(
                        isSettingsSheetOpen = event.isOpen,
                        selectedBottomNavTab = if (!event.isOpen && it.selectedBottomNavTab == 2) 0 else it.selectedBottomNavTab
                    )
                }
            }
            is HomeEvent.DeleteHistoryItem -> {
                settingsRepository.deleteScanHistoryItem(event.verdict.indicator.rawValue)
                _uiState.update { current ->
                    current.copy(
                        scanHistory = current.scanHistory.filter { it.indicator.rawValue != event.verdict.indicator.rawValue }
                    )
                }
            }
            HomeEvent.ClearHistory -> {
                settingsRepository.clearScanHistory()
                _uiState.update { it.copy(scanHistory = emptyList()) }
            }
            HomeEvent.OpenVisionLens -> {
                _uiState.update { it.copy(isVisionLensOpen = true) }
            }
            HomeEvent.CloseVisionLens -> {
                _uiState.update { it.copy(isVisionLensOpen = false) }
            }
            is HomeEvent.VisionPayloadScanned -> {
                val targetToTriage = event.payload.unmaskedTargetUrl ?: event.payload.rawText
                _uiState.update {
                    it.copy(
                        queryInput = event.payload.defangedText,
                        isVisionLensOpen = false,
                        activeVisionPayload = event.payload
                    )
                }
                cancelScan()
                executeAnalyzeQuery(targetToTriage, currentGeneration)
            }
        }
    }

    private fun cancelScan() {
        currentGeneration++
        hashJob?.cancel()
        hashJob = null
        scanJob?.cancel()
        scanJob = null
        clearErrorMessage()
        _uiState.update {
            it.copy(
                isLoading = false,
                isHashingFile = false,
                hashingFileName = null,
                targetIndicator = null,
                scanFinishedEngines = 0,
                scanTotalEngines = 10
            )
        }
    }

    private fun hashAndAnalyzeFile(filePath: String, fileName: String, isTemporary: Boolean = false) {
        cancelScan()
        val generation = currentGeneration

        clearErrorMessage()
        _uiState.update {
            it.copy(
                isHashingFile = true,
                hashingFileName = fileName,
                activeVerdict = null
            )
        }

        hashJob = viewModelScope.launch {
            try {
                val hashResult = streamHasher.hashFile(filePath)
                if (generation == currentGeneration) {
                    _uiState.update {
                        it.copy(
                            isHashingFile = false,
                            hashingFileName = null,
                            queryInput = hashResult.sha256
                        )
                    }
                    executeAnalyzeQuery(hashResult.sha256, generation)
                }
            } catch (e: CancellationException) {
                if (generation == currentGeneration) {
                    _uiState.update {
                        it.copy(
                            isHashingFile = false,
                            hashingFileName = null
                        )
                    }
                }
                throw e
            } catch (e: Exception) {
                if (generation == currentGeneration) {
                    val strings = LocalizationProvider.getStrings(_uiState.value.appLanguage)
                    _uiState.update {
                        it.copy(
                            isHashingFile = false,
                            hashingFileName = null
                        )
                    }
                    setTimedErrorMessage("${strings.streamHashingFailed}: ${e.message ?: "error"}")
                }
            } finally {
                if (isTemporary) {
                    temporaryFileCleaner.cleanIfTemporary(filePath)
                }
                if (generation == currentGeneration) {
                    hashJob = null
                }
            }
        }
    }

    private fun analyzeQuery() {
        val input = _uiState.value.queryInput.trim()
        val strings = LocalizationProvider.getStrings(_uiState.value.appLanguage)

        if (input.isBlank()) {
            setTimedErrorMessage(strings.enterIndicatorError)
            return
        }

        // Cancel any active in-flight scan to prevent race conditions from rapid re-submissions
        cancelScan()
        val generation = currentGeneration

        executeAnalyzeQuery(input, generation)
    }

    private fun executeAnalyzeQuery(input: String, generation: Long) {
        val strings = LocalizationProvider.getStrings(_uiState.value.appLanguage)
        val indicator = analyzeIndicatorUseCase.parseIndicator(input)

        // Immediately clear stale previous verdict and set loading state with target indicator
        clearErrorMessage()
        _uiState.update {
            it.copy(
                isLoading = true,
                targetIndicator = indicator,
                activeVerdict = null,
                scanFinishedEngines = 0,
                scanTotalEngines = 10
            )
        }

        scanJob = viewModelScope.launch {
            try {
                val verdict = analyzeIndicatorUseCase(indicator) { finished, total ->
                    if (generation == currentGeneration) {
                        _uiState.update { state ->
                            if (state.isLoading) {
                                state.copy(scanFinishedEngines = finished, scanTotalEngines = total)
                            } else state
                        }
                    }
                }
                if (generation == currentGeneration) {
                    _uiState.update { state ->
                        val updatedHistory = (listOf(verdict) + state.scanHistory.filterNot {
                            it.indicator.normalizedValue == verdict.indicator.normalizedValue
                        }).take(20)
                        settingsRepository.saveScanHistory(updatedHistory)
                        state.copy(
                            isLoading = false,
                            activeVerdict = verdict,
                            scanHistory = updatedHistory
                        )
                    }
                }
            } catch (e: CancellationException) {
                if (generation == currentGeneration) {
                    // Cooperative coroutine cancellation: clean exit, no error banner
                    _uiState.update { it.copy(isLoading = false, targetIndicator = null, scanFinishedEngines = 0) }
                }
                throw e
            } catch (e: Exception) {
                if (generation == currentGeneration) {
                    val errorMsg = when {
                        e.message?.contains("Rate Limit", ignoreCase = true) == true -> strings.errorRateLimit
                        e.message?.contains("timeout", ignoreCase = true) == true -> strings.errorTimeout
                        e.message?.contains("Network", ignoreCase = true) == true || e.message?.contains("Connect", ignoreCase = true) == true -> strings.errorNetwork
                        else -> e.message ?: strings.errorNetwork
                    }
                    _uiState.update { it.copy(isLoading = false, targetIndicator = null) }
                    setTimedErrorMessage(errorMsg)
                }
            } finally {
                if (generation == currentGeneration) {
                    scanJob = null
                }
            }
        }
    }

    private var queueJob: Job? = null

    private fun handleBatchFilesSelected(files: List<PickedFile>, isTemporary: Boolean = false) {
        if (files.isEmpty()) return
        if (files.size == 1) {
            val first = files.first()
            hashAndAnalyzeFile(first.path, first.name, isTemporary)
            val queueItem = FileQueueItem(
                id = nextQueueId(0),
                filePath = first.path,
                fileName = first.name,
                fileSizeBytes = first.sizeBytes,
                status = QueueItemStatus.HASHING,
                isTemporary = isTemporary
            )
            _uiState.update { it.copy(fileQueue = listOf(queueItem)) }
            return
        }

        val newQueueItems = files.mapIndexed { index, file ->
            FileQueueItem(
                id = nextQueueId(index),
                filePath = file.path,
                fileName = file.name,
                fileSizeBytes = file.sizeBytes,
                status = QueueItemStatus.QUEUED,
                isTemporary = isTemporary
            )
        }

        _uiState.update {
            it.copy(
                fileQueue = it.fileQueue + newQueueItems,
                isProcessingQueue = true
            )
        }

        startQueueProcessing()
    }

    private fun startQueueProcessing() {
        if (queueJob?.isActive == true) return
        queueJob = viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { it.copy(isProcessingQueue = true) }

            while (isActive) {
                val nextItem = _uiState.value.fileQueue.firstOrNull { it.status == QueueItemStatus.QUEUED }
                if (nextItem == null) break

                _uiState.update { state ->
                    state.copy(
                        activeQueueItemId = nextItem.id,
                        fileQueue = state.fileQueue.map {
                            if (it.id == nextItem.id) it.copy(status = QueueItemStatus.HASHING) else it
                        }
                    )
                }

                val hash = try {
                    val res = streamHasher.hashFile(nextItem.filePath)
                    res.sha256
                } catch (_: Exception) {
                    null
                }

                if (hash == null) {
                    _uiState.update { state ->
                        state.copy(
                            fileQueue = state.fileQueue.map {
                                if (it.id == nextItem.id) it.copy(status = QueueItemStatus.ERROR, errorMessage = "Failed to calculate SHA-256") else it
                            }
                        )
                    }
                    if (nextItem.isTemporary) {
                        temporaryFileCleaner.cleanIfTemporary(nextItem.filePath)
                    }
                    continue
                }

                _uiState.update { state ->
                    state.copy(
                        fileQueue = state.fileQueue.map {
                            if (it.id == nextItem.id) it.copy(sha256 = hash, status = QueueItemStatus.TRIAGING) else it
                        }
                    )
                }

                val verdict = try {
                    val indicator = Indicator(hash, hash, IndicatorType.FILE_HASH)
                    analyzeIndicatorUseCase(indicator)
                } catch (_: Exception) {
                    null
                }

                if (verdict != null) {
                    _uiState.update { state ->
                        val updatedHistory = (listOf(verdict) + state.scanHistory.filterNot {
                            it.indicator.normalizedValue == verdict.indicator.normalizedValue
                        }).take(20)
                        settingsRepository.saveScanHistory(updatedHistory)
                        state.copy(
                            scanHistory = updatedHistory,
                            activeVerdict = if (state.activeVerdict == null) verdict else state.activeVerdict,
                            targetIndicator = if (state.targetIndicator == null) verdict.indicator else state.targetIndicator,
                            fileQueue = state.fileQueue.map {
                                if (it.id == nextItem.id) it.copy(status = QueueItemStatus.DONE, verdict = verdict) else it
                            }
                        )
                    }
                } else {
                    _uiState.update { state ->
                        state.copy(
                            fileQueue = state.fileQueue.map {
                                if (it.id == nextItem.id) it.copy(status = QueueItemStatus.ERROR, errorMessage = "Triage engines unreachable") else it
                            }
                        )
                    }
                }

                if (nextItem.isTemporary) {
                    temporaryFileCleaner.cleanIfTemporary(nextItem.filePath)
                }
            }

            _uiState.update { it.copy(isProcessingQueue = false, activeQueueItemId = null) }
        }
    }

    private fun clearFileQueue() {
        queueJob?.cancel()
        queueJob = null
        _uiState.update {
            it.copy(
                fileQueue = emptyList(),
                isProcessingQueue = false,
                activeQueueItemId = null
            )
        }
    }
}

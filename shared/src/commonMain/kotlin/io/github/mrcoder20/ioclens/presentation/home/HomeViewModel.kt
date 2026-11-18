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
            privacyGuardEnabled = initialSettings.privacyGuardEnabled,
            scanHistory = initialHistory
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null
    private var hashJob: Job? = null
    private var currentGeneration: Long = 0L
    private var errorDismissJob: Job? = null
    private var feedbackDismissJob: Job? = null
    private var queueIdSequence: Long = 0L

    private fun nextQueueId(index: Int = 0): String {
        queueIdSequence++
        return "queue_${queueIdSequence}_$index"
    }

    private fun isManagedTemporaryFile(filePath: String, explicitFlag: Boolean): Boolean {
        if (explicitFlag) return true
        val normalized = filePath.replace('\\', '/')
        val fileName = normalized.substringAfterLast('/')
        val hasManagedPrefix = fileName.startsWith("share_") ||
            fileName.startsWith("picked_") ||
            fileName.startsWith("shared_stream_") ||
            fileName.startsWith("batch_") ||
            fileName.startsWith("ioc_drop_") ||
            fileName.startsWith("ioc_pick_")
        if (!hasManagedPrefix) return false
        val lowerPath = normalized.lowercase()
        return lowerPath.contains("/cache/") ||
            lowerPath.contains("/tmp/") ||
            lowerPath.contains("/temp/")
    }

    private fun setStatusFeedback(message: String?, autoDismissDelayMs: Long = 2600L) {
        feedbackDismissJob?.cancel()
        _uiState.update { it.copy(statusFeedbackMessage = message) }
        if (!message.isNullOrBlank() && autoDismissDelayMs > 0) {
            feedbackDismissJob = viewModelScope.launch {
                delay(autoDismissDelayMs)
                _uiState.update { state ->
                    if (state.statusFeedbackMessage == message) {
                        state.copy(statusFeedbackMessage = null)
                    } else state
                }
            }
        }
    }

    private fun updateQueueItem(
        itemId: String?,
        transform: (FileQueueItem) -> FileQueueItem
    ) {
        if (itemId == null) return
        _uiState.update { state ->
            state.copy(
                fileQueue = state.fileQueue.map { item ->
                    if (item.id == itemId) transform(item) else item
                }
            )
        }
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
                        appLanguage = settings.appLanguage,
                        privacyGuardEnabled = settings.privacyGuardEnabled
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
            is HomeEvent.RemoveQueueItem -> {
                val removed = _uiState.value.fileQueue.firstOrNull { it.id == event.itemId }
                _uiState.update { state ->
                    state.copy(
                        fileQueue = state.fileQueue.filterNot { it.id == event.itemId },
                        activeQueueItemId = if (state.activeQueueItemId == event.itemId) null else state.activeQueueItemId
                    )
                }
                if (removed != null && isManagedTemporaryFile(removed.filePath, removed.isTemporary)) {
                    temporaryFileCleaner.cleanIfTemporary(removed.filePath)
                }
            }
            is HomeEvent.RetryQueueItem -> {
                val targetItem = _uiState.value.fileQueue.firstOrNull { it.id == event.itemId }
                if (targetItem != null && targetItem.status == QueueItemStatus.ERROR) {
                    _uiState.update { state ->
                        state.copy(
                            fileQueue = state.fileQueue.map { item ->
                                if (item.id == event.itemId) {
                                    item.copy(status = QueueItemStatus.QUEUED, errorMessage = null)
                                } else item
                            }
                        )
                    }
                    startQueueProcessing()
                }
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
            HomeEvent.ReScanIndicator -> {
                val target = _uiState.value.targetIndicator?.rawValue?.ifBlank { null }
                    ?: _uiState.value.queryInput.trim()
                if (target.isNotBlank()) {
                    cancelScan()
                    executeAnalyzeQuery(target, currentGeneration)
                }
            }
            HomeEvent.CancelScan -> {
                cancelScan()
            }
            HomeEvent.ClearClicked -> {
                cancelScan()
                clearErrorMessage()
                _uiState.update { it.copy(queryInput = "", targetIndicator = null, activeVerdict = null) }
            }
            is HomeEvent.SelectSampleQuery -> {
                clearErrorMessage()
                _uiState.update { it.copy(queryInput = event.sample) }
                analyzeQuery()
            }
            is HomeEvent.ShowStatusFeedback -> {
                setStatusFeedback(event.message)
            }
            HomeEvent.DismissStatusFeedback -> {
                setStatusFeedback(null, autoDismissDelayMs = 0L)
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
                        scanHistory = current.scanHistory.filterNot {
                            it.indicator.rawValue == event.verdict.indicator.rawValue ||
                                it.indicator.normalizedValue == event.verdict.indicator.normalizedValue
                        }
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

    private fun hashAndAnalyzeFile(
        filePath: String,
        fileName: String,
        isTemporary: Boolean = false,
        queueItemId: String? = null
    ) {
        cancelScan()
        val generation = currentGeneration
        val managedTemporary = isManagedTemporaryFile(filePath, isTemporary)

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
                    updateQueueItem(queueItemId) { item ->
                        item.copy(sha256 = hashResult.sha256, status = QueueItemStatus.TRIAGING)
                    }
                    _uiState.update {
                        it.copy(
                            isHashingFile = false,
                            hashingFileName = null,
                            queryInput = hashResult.sha256
                        )
                    }
                    executeAnalyzeQuery(hashResult.sha256, generation, queueItemId = queueItemId)
                }
            } catch (e: CancellationException) {
                if (generation == currentGeneration) {
                    val strings = LocalizationProvider.getStrings(_uiState.value.appLanguage)
                    updateQueueItem(queueItemId) { item ->
                        item.copy(status = QueueItemStatus.ERROR, errorMessage = strings.cancelScan)
                    }
                    _uiState.update {
                        it.copy(
                            isHashingFile = false,
                            hashingFileName = null,
                            activeQueueItemId = if (it.activeQueueItemId == queueItemId) null else it.activeQueueItemId
                        )
                    }
                }
                throw e
            } catch (e: Exception) {
                if (generation == currentGeneration) {
                    val strings = LocalizationProvider.getStrings(_uiState.value.appLanguage)
                    val errText = "${strings.streamHashingFailed}: ${e.message ?: "error"}"
                    updateQueueItem(queueItemId) { item ->
                        item.copy(status = QueueItemStatus.ERROR, errorMessage = errText)
                    }
                    _uiState.update {
                        it.copy(
                            isHashingFile = false,
                            hashingFileName = null,
                            activeQueueItemId = if (it.activeQueueItemId == queueItemId) null else it.activeQueueItemId
                        )
                    }
                    setTimedErrorMessage(errText)
                }
            } finally {
                if (managedTemporary) {
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

    private fun executeAnalyzeQuery(input: String, generation: Long, queueItemId: String? = null) {
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
                    val maxEntries = settingsRepository.getSettings().maxHistoryEntries.coerceAtLeast(1)
                    var persistedHistory: List<TriageVerdict> = emptyList()
                    _uiState.update { state ->
                        val updatedHistory = (listOf(verdict) + state.scanHistory.filterNot {
                            it.indicator.normalizedValue == verdict.indicator.normalizedValue
                        }).take(maxEntries)
                        persistedHistory = updatedHistory
                        state.copy(
                            isLoading = false,
                            activeVerdict = verdict,
                            scanHistory = updatedHistory,
                            activeQueueItemId = if (state.activeQueueItemId == queueItemId) null else state.activeQueueItemId,
                            fileQueue = if (queueItemId != null) {
                                state.fileQueue.map { item ->
                                    if (item.id == queueItemId) {
                                        item.copy(status = QueueItemStatus.DONE, verdict = verdict, errorMessage = null)
                                    } else item
                                }
                            } else state.fileQueue
                        )
                    }
                    settingsRepository.saveScanHistory(persistedHistory)
                }
            } catch (e: CancellationException) {
                if (generation == currentGeneration) {
                    updateQueueItem(queueItemId) { item ->
                        item.copy(status = QueueItemStatus.ERROR, errorMessage = strings.cancelScan)
                    }
                    // Cooperative coroutine cancellation: clean exit, no error banner
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            targetIndicator = null,
                            scanFinishedEngines = 0,
                            activeQueueItemId = if (it.activeQueueItemId == queueItemId) null else it.activeQueueItemId
                        )
                    }
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
                    updateQueueItem(queueItemId) { item ->
                        item.copy(status = QueueItemStatus.ERROR, errorMessage = errorMsg)
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            targetIndicator = null,
                            activeQueueItemId = if (it.activeQueueItemId == queueItemId) null else it.activeQueueItemId
                        )
                    }
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
            val managedTemp = isManagedTemporaryFile(first.path, isTemporary)
            val queueItem = FileQueueItem(
                id = nextQueueId(0),
                filePath = first.path,
                fileName = first.name,
                fileSizeBytes = first.sizeBytes,
                status = QueueItemStatus.HASHING,
                isTemporary = managedTemp
            )
            _uiState.update { it.copy(fileQueue = listOf(queueItem), activeQueueItemId = queueItem.id) }
            hashAndAnalyzeFile(first.path, first.name, managedTemp, queueItemId = queueItem.id)
            return
        }

        val newQueueItems = files.mapIndexed { index, file ->
            FileQueueItem(
                id = nextQueueId(index),
                filePath = file.path,
                fileName = file.name,
                fileSizeBytes = file.sizeBytes,
                status = QueueItemStatus.QUEUED,
                isTemporary = isManagedTemporaryFile(file.path, isTemporary)
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
            val batchVerdictCache = mutableMapOf<String, TriageVerdict>()
            _uiState.value.fileQueue.forEach { existing ->
                val sha = existing.sha256
                val verdict = existing.verdict
                if (existing.status == QueueItemStatus.DONE && sha != null && verdict != null) {
                    batchVerdictCache[sha] = verdict
                }
            }
            _uiState.update { it.copy(isProcessingQueue = true) }

            try {
                while (isActive) {
                    val nextItem = _uiState.value.fileQueue.firstOrNull { it.status == QueueItemStatus.QUEUED }
                    if (nextItem == null) break

                    val strings = LocalizationProvider.getStrings(_uiState.value.appLanguage)
                    _uiState.update { state ->
                        state.copy(
                            activeQueueItemId = nextItem.id,
                            fileQueue = state.fileQueue.map {
                                if (it.id == nextItem.id) it.copy(status = QueueItemStatus.HASHING, errorMessage = null) else it
                            }
                        )
                    }

                    val hash = nextItem.sha256 ?: try {
                        streamHasher.hashFile(nextItem.filePath).sha256
                    } catch (ce: CancellationException) {
                        throw ce
                    } catch (_: Exception) {
                        null
                    }

                    if (hash == null) {
                        _uiState.update { state ->
                            state.copy(
                                fileQueue = state.fileQueue.map {
                                    if (it.id == nextItem.id) {
                                        it.copy(status = QueueItemStatus.ERROR, errorMessage = strings.streamHashingFailed)
                                    } else it
                                }
                            )
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

                    val cachedVerdict = batchVerdictCache[hash]
                    val verdict = cachedVerdict ?: try {
                        val indicator = Indicator(hash, hash, IndicatorType.FILE_HASH)
                        analyzeIndicatorUseCase(indicator)
                    } catch (ce: CancellationException) {
                        throw ce
                    } catch (_: Exception) {
                        null
                    }

                    if (verdict != null) {
                        batchVerdictCache[hash] = verdict
                        val maxEntries = settingsRepository.getSettings().maxHistoryEntries.coerceAtLeast(1)
                        var persistedHistory: List<TriageVerdict> = emptyList()
                        _uiState.update { state ->
                            val updatedHistory = (listOf(verdict) + state.scanHistory.filterNot {
                                it.indicator.normalizedValue == verdict.indicator.normalizedValue
                            }).take(maxEntries)
                            persistedHistory = updatedHistory
                            state.copy(
                                scanHistory = updatedHistory,
                                activeVerdict = if (state.activeVerdict == null) verdict else state.activeVerdict,
                                targetIndicator = if (state.targetIndicator == null) verdict.indicator else state.targetIndicator,
                                fileQueue = state.fileQueue.map {
                                    if (it.id == nextItem.id) {
                                        it.copy(status = QueueItemStatus.DONE, verdict = verdict, errorMessage = null)
                                    } else it
                                }
                            )
                        }
                        settingsRepository.saveScanHistory(persistedHistory)
                        if (isManagedTemporaryFile(nextItem.filePath, nextItem.isTemporary)) {
                            temporaryFileCleaner.cleanIfTemporary(nextItem.filePath)
                        }
                    } else {
                        _uiState.update { state ->
                            state.copy(
                                fileQueue = state.fileQueue.map {
                                    if (it.id == nextItem.id) {
                                        it.copy(status = QueueItemStatus.ERROR, errorMessage = strings.errorNetwork)
                                    } else it
                                }
                            )
                        }
                    }
                }
            } finally {
                _uiState.update { it.copy(isProcessingQueue = false, activeQueueItemId = null) }
            }
        }
    }

    private fun cleanTemporaryQueueFiles(items: List<FileQueueItem>) {
        items.forEach { item ->
            if (isManagedTemporaryFile(item.filePath, item.isTemporary)) {
                temporaryFileCleaner.cleanIfTemporary(item.filePath)
            }
        }
    }

    private fun clearFileQueue() {
        queueJob?.cancel()
        queueJob = null
        val snapshot = _uiState.value.fileQueue
        _uiState.update {
            it.copy(
                fileQueue = emptyList(),
                isProcessingQueue = false,
                activeQueueItemId = null
            )
        }
        cleanTemporaryQueueFiles(snapshot)
    }

    override fun onCleared() {
        super.onCleared()
        cleanTemporaryQueueFiles(_uiState.value.fileQueue)
    }
}

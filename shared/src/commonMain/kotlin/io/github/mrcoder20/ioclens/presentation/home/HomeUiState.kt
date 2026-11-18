package io.github.mrcoder20.ioclens.presentation.home

import io.github.mrcoder20.ioclens.core.localization.AppLanguage
import io.github.mrcoder20.ioclens.core.localization.AppTheme
import io.github.mrcoder20.ioclens.domain.model.Indicator
import io.github.mrcoder20.ioclens.domain.model.TriageVerdict
import io.github.mrcoder20.ioclens.getSystemLanguage

enum class QueueItemStatus {
    QUEUED,
    HASHING,
    TRIAGING,
    DONE,
    ERROR
}

data class FileQueueItem(
    val id: String,
    val filePath: String,
    val fileName: String,
    val fileSizeBytes: Long = 0L,
    val sha256: String? = null,
    val status: QueueItemStatus = QueueItemStatus.QUEUED,
    val verdict: TriageVerdict? = null,
    val errorMessage: String? = null,
    val isTemporary: Boolean = false
)

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
    val selectedBottomNavTab: Int = 0,
    val scanFinishedEngines: Int = 0,
    val scanTotalEngines: Int = 10,
    val errorMessage: String? = null,
    val appTheme: AppTheme = AppTheme.DARK,
    val appLanguage: AppLanguage = AppLanguage.ENGLISH,
    val privacyGuardEnabled: Boolean = true,
    val fileQueue: List<FileQueueItem> = emptyList(),
    val isProcessingQueue: Boolean = false,
    val activeQueueItemId: String? = null,
    val statusFeedbackMessage: String? = null,
    val isVisionLensOpen: Boolean = false,
    val activeVisionPayload: io.github.mrcoder20.ioclens.domain.vision.BarcodePayload? = null
)

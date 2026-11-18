package io.github.mrcoder20.ioclens.domain.model

import io.github.mrcoder20.ioclens.core.localization.AppLanguage
import io.github.mrcoder20.ioclens.core.localization.AppTheme

data class AppSettings(
    val isIntroCompleted: Boolean = false,
    val privacyGuardEnabled: Boolean = true,
    val autoScanClipboard: Boolean = true,
    val appTheme: AppTheme = AppTheme.IOC_LENS_CLASSIC,
    val appLanguage: AppLanguage = AppLanguage.ENGLISH,
    val virusTotalApiKey: String = "",
    val abuseIpDbApiKey: String = "",
    val alienVaultApiKey: String = "",
    val pulsediveApiKey: String = "",
    val ipQualityScoreApiKey: String = "",
    val requestTimeoutMs: Long = DEFAULT_REQUEST_TIMEOUT_MS,
    val maxHistoryEntries: Int = DEFAULT_MAX_HISTORY_ENTRIES,
    val historyRetentionDays: Int = DEFAULT_HISTORY_RETENTION_DAYS,
    val disabledProviders: Set<String> = emptySet(),
    val customPrivacyRules: List<String> = emptyList()
) {
    companion object {
        const val DEFAULT_REQUEST_TIMEOUT_MS = 10_000L
        const val MIN_REQUEST_TIMEOUT_MS = 3_000L
        const val MAX_REQUEST_TIMEOUT_MS = 30_000L
        const val DEFAULT_MAX_HISTORY_ENTRIES = 30
        const val DEFAULT_HISTORY_RETENTION_DAYS = 30
    }
}

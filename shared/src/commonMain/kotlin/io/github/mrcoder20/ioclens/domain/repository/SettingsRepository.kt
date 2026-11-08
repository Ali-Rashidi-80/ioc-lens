package io.github.mrcoder20.ioclens.domain.repository

import io.github.mrcoder20.ioclens.core.localization.AppLanguage
import io.github.mrcoder20.ioclens.core.localization.AppTheme
import io.github.mrcoder20.ioclens.domain.model.AppSettings
import io.github.mrcoder20.ioclens.domain.model.TriageVerdict
import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    val settingsState: StateFlow<AppSettings>
    fun getSettings(): AppSettings
    fun setIntroCompleted(completed: Boolean)
    fun setPrivacyGuardEnabled(enabled: Boolean)
    fun setAppTheme(theme: AppTheme)
    fun setAppLanguage(language: AppLanguage)
    fun updateApiKey(provider: String, key: String)

    fun getScanHistory(): List<TriageVerdict>
    fun saveScanHistory(history: List<TriageVerdict>)
    fun deleteScanHistoryItem(rawValue: String)
    fun clearScanHistory()
}

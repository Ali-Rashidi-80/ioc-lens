package io.github.mrcoder20.ioclens.data.repository

import io.github.mrcoder20.ioclens.core.concurrency.PlatformLock
import io.github.mrcoder20.ioclens.core.concurrency.withLock
import io.github.mrcoder20.ioclens.core.localization.AppLanguage
import io.github.mrcoder20.ioclens.core.localization.AppTheme
import io.github.mrcoder20.ioclens.core.localization.LocaleManager
import io.github.mrcoder20.ioclens.core.storage.KeyValueStorage
import io.github.mrcoder20.ioclens.currentTimeMillis
import io.github.mrcoder20.ioclens.domain.model.AppSettings
import io.github.mrcoder20.ioclens.domain.model.TriageVerdict
import io.github.mrcoder20.ioclens.domain.repository.SettingsRepository
import io.github.mrcoder20.ioclens.logDiagnostic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SettingsRepositoryImpl(
    private val storage: KeyValueStorage
) : SettingsRepository {

    private val lock = PlatformLock()

    private val KEY_INTRO_COMPLETED = "key_intro_completed"
    private val KEY_PRIVACY_GUARD = "key_privacy_guard"
    private val KEY_AUTO_SCAN = "key_auto_scan"
    private val KEY_THEME = "key_theme"
    private val KEY_LANGUAGE = "key_language"
    private val KEY_VT_API = "key_vt_api"
    private val KEY_ABUSEIPDB_API = "key_abuseipdb_api"
    private val KEY_ALIENVAULT_API = "key_alienvault_api"
    private val KEY_PULSEDIVE_API = "key_pulsedive_api"
    private val KEY_IPQUALITYSCORE_API = "key_ipqualityscore_api"
    private val KEY_REQUEST_TIMEOUT_MS = "key_request_timeout_ms"
    private val KEY_MAX_HISTORY_ENTRIES = "key_max_history_entries"
    private val KEY_HISTORY_RETENTION_DAYS = "key_history_retention_days"
    private val KEY_DISABLED_PROVIDERS = "key_disabled_providers"
    private val KEY_CUSTOM_PRIVACY_RULES = "key_privacy_rules_json"
    private val KEY_SCAN_HISTORY = "key_scan_history_json"

    private val historyJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val _settingsState = MutableStateFlow(loadSettingsFromStorage().also {
        LocaleManager.setLanguage(it.appLanguage)
    })
    override val settingsState: StateFlow<AppSettings> = _settingsState.asStateFlow()

    override fun getSettings(): AppSettings = _settingsState.value

    override fun setIntroCompleted(completed: Boolean) {
        lock.withLock {
            storage.setBoolean(KEY_INTRO_COMPLETED, completed)
            _settingsState.value = _settingsState.value.copy(isIntroCompleted = completed)
        }
    }

    override fun setPrivacyGuardEnabled(enabled: Boolean) {
        lock.withLock {
            storage.setBoolean(KEY_PRIVACY_GUARD, enabled)
            _settingsState.value = _settingsState.value.copy(privacyGuardEnabled = enabled)
        }
    }

    override fun setAppTheme(theme: AppTheme) {
        lock.withLock {
            storage.setString(KEY_THEME, theme.name)
            _settingsState.value = _settingsState.value.copy(appTheme = theme)
        }
    }

    override fun setAppLanguage(language: AppLanguage) {
        lock.withLock {
            storage.setString(KEY_LANGUAGE, language.name)
            LocaleManager.setLanguage(language)
            _settingsState.value = _settingsState.value.copy(appLanguage = language)
        }
    }

    override fun setAutoScanClipboard(enabled: Boolean) {
        lock.withLock {
            storage.setBoolean(KEY_AUTO_SCAN, enabled)
            _settingsState.value = _settingsState.value.copy(autoScanClipboard = enabled)
        }
    }

    override fun setRequestTimeoutMs(timeoutMs: Long) {
        val clamped = timeoutMs.coerceIn(AppSettings.MIN_REQUEST_TIMEOUT_MS, AppSettings.MAX_REQUEST_TIMEOUT_MS)
        lock.withLock {
            storage.setString(KEY_REQUEST_TIMEOUT_MS, clamped.toString())
            _settingsState.value = _settingsState.value.copy(requestTimeoutMs = clamped)
        }
    }

    override fun setMaxHistoryEntries(maxEntries: Int) {
        val clamped = maxEntries.coerceIn(10, 200)
        lock.withLock {
            storage.setString(KEY_MAX_HISTORY_ENTRIES, clamped.toString())
            _settingsState.value = _settingsState.value.copy(maxHistoryEntries = clamped)
        }
    }

    override fun setProviderEnabled(providerName: String, enabled: Boolean) {
        lock.withLock {
            val current = _settingsState.value.disabledProviders
            val updated = if (enabled) current - providerName else current + providerName
            val serialized = historyJson.encodeToString(updated.toList())
            storage.setString(KEY_DISABLED_PROVIDERS, serialized)
            _settingsState.value = _settingsState.value.copy(disabledProviders = updated)
        }
    }

    override fun addCustomPrivacyRule(rule: String): Boolean {
        val cleaned = rule.trim().lowercase()
        if (cleaned.isEmpty() || cleaned.length > 255 || cleaned.any { it.isWhitespace() }) {
            return false
        }
        return lock.withLock {
            val current = _settingsState.value.customPrivacyRules
            if (cleaned in current) return@withLock false
            val updated = (current + cleaned).take(100)
            val serialized = historyJson.encodeToString(updated)
            storage.setString(KEY_CUSTOM_PRIVACY_RULES, serialized)
            _settingsState.value = _settingsState.value.copy(customPrivacyRules = updated)
            true
        }
    }

    override fun removeCustomPrivacyRule(rule: String) {
        val cleaned = rule.trim().lowercase()
        lock.withLock {
            val updated = _settingsState.value.customPrivacyRules.filterNot { it == cleaned }
            val serialized = historyJson.encodeToString(updated)
            storage.setString(KEY_CUSTOM_PRIVACY_RULES, serialized)
            _settingsState.value = _settingsState.value.copy(customPrivacyRules = updated)
        }
    }

    override fun updateApiKey(provider: String, key: String) {
        val sanitized = key.trim().replace("\r", "").replace("\n", "")
        lock.withLock {
            when (provider.lowercase()) {
                "virustotal" -> storage.setString(KEY_VT_API, sanitized)
                "abuseipdb" -> storage.setString(KEY_ABUSEIPDB_API, sanitized)
                "alienvault" -> storage.setString(KEY_ALIENVAULT_API, sanitized)
                "pulsedive" -> storage.setString(KEY_PULSEDIVE_API, sanitized)
                "ipqualityscore" -> storage.setString(KEY_IPQUALITYSCORE_API, sanitized)
            }
            _settingsState.value = loadSettingsFromStorage()
        }
    }

    override fun getScanHistory(): List<TriageVerdict> = lock.withLock {
        val jsonStr = storage.getString(KEY_SCAN_HISTORY, "")
        if (jsonStr.isBlank()) return emptyList()
        return try {
            val decoded = historyJson.decodeFromString<List<TriageVerdict>>(jsonStr)
            pruneExpiredHistory(decoded)
        } catch (e: Exception) {
            logDiagnostic("SettingsRepo", "Failed to decode scan history; clearing corrupt cache", e)
            storage.remove(KEY_SCAN_HISTORY)
            emptyList()
        }
    }

    override fun saveScanHistory(history: List<TriageVerdict>) {
        lock.withLock {
            try {
                val limit = _settingsState.value.maxHistoryEntries.coerceIn(10, 200)
                val pruned = pruneExpiredHistory(history).take(limit)
                val jsonStr = historyJson.encodeToString(pruned)
                storage.setString(KEY_SCAN_HISTORY, jsonStr)
            } catch (e: Exception) {
                logDiagnostic("SettingsRepo", "Failed to encode/save scan history", e)
            }
        }
    }

    override fun deleteScanHistoryItem(rawValue: String) {
        lock.withLock {
            val filtered = getScanHistory().filterNot {
                it.indicator.rawValue == rawValue || it.indicator.normalizedValue == rawValue
            }
            saveScanHistory(filtered)
        }
    }

    override fun clearScanHistory() {
        lock.withLock {
            storage.setString(KEY_SCAN_HISTORY, "")
            storage.remove(KEY_SCAN_HISTORY)
        }
    }

    private fun pruneExpiredHistory(history: List<TriageVerdict>): List<TriageVerdict> {
        val retentionDays = _settingsState.value.historyRetentionDays.coerceAtLeast(1)
        val cutoffEpochMs = currentTimeMillis() - retentionDays.toLong() * 86_400_000L
        return history.filter { item ->
            item.timestampEpochMs <= 0L || item.timestampEpochMs >= cutoffEpochMs
        }
    }

    private fun loadSettingsFromStorage(): AppSettings {
        val defaultLang = AppLanguage.ENGLISH
        val themeStr = storage.getString(KEY_THEME, AppTheme.IOC_LENS_CLASSIC.name)
        val langStr = storage.getString(KEY_LANGUAGE, defaultLang.name)

        val theme = runCatching { AppTheme.valueOf(themeStr) }.getOrDefault(AppTheme.IOC_LENS_CLASSIC)
        val language = runCatching { AppLanguage.valueOf(langStr) }.getOrDefault(defaultLang)

        val timeoutMs = storage.getString(KEY_REQUEST_TIMEOUT_MS, AppSettings.DEFAULT_REQUEST_TIMEOUT_MS.toString())
            .toLongOrNull()
            ?.coerceIn(AppSettings.MIN_REQUEST_TIMEOUT_MS, AppSettings.MAX_REQUEST_TIMEOUT_MS)
            ?: AppSettings.DEFAULT_REQUEST_TIMEOUT_MS

        val maxHistory = storage.getString(KEY_MAX_HISTORY_ENTRIES, AppSettings.DEFAULT_MAX_HISTORY_ENTRIES.toString())
            .toIntOrNull()
            ?.coerceIn(10, 200)
            ?: AppSettings.DEFAULT_MAX_HISTORY_ENTRIES

        val retentionDays = storage.getString(KEY_HISTORY_RETENTION_DAYS, AppSettings.DEFAULT_HISTORY_RETENTION_DAYS.toString())
            .toIntOrNull()
            ?.coerceIn(1, 365)
            ?: AppSettings.DEFAULT_HISTORY_RETENTION_DAYS

        val disabledProvidersRaw = storage.getString(KEY_DISABLED_PROVIDERS, "")
        val disabledProviders = if (disabledProvidersRaw.isBlank()) {
            emptySet()
        } else {
            runCatching { historyJson.decodeFromString<List<String>>(disabledProvidersRaw).toSet() }
                .getOrDefault(emptySet())
        }

        val customRulesRaw = storage.getString(KEY_CUSTOM_PRIVACY_RULES, "")
        val customRules = if (customRulesRaw.isBlank()) {
            emptyList()
        } else {
            runCatching { historyJson.decodeFromString<List<String>>(customRulesRaw) }
                .getOrDefault(emptyList())
        }

        return AppSettings(
            isIntroCompleted = storage.getBoolean(KEY_INTRO_COMPLETED, false),
            privacyGuardEnabled = storage.getBoolean(KEY_PRIVACY_GUARD, true),
            autoScanClipboard = storage.getBoolean(KEY_AUTO_SCAN, true),
            appTheme = theme,
            appLanguage = language,
            virusTotalApiKey = storage.getString(KEY_VT_API, ""),
            abuseIpDbApiKey = storage.getString(KEY_ABUSEIPDB_API, ""),
            alienVaultApiKey = storage.getString(KEY_ALIENVAULT_API, ""),
            pulsediveApiKey = storage.getString(KEY_PULSEDIVE_API, ""),
            ipQualityScoreApiKey = storage.getString(KEY_IPQUALITYSCORE_API, ""),
            requestTimeoutMs = timeoutMs,
            maxHistoryEntries = maxHistory,
            historyRetentionDays = retentionDays,
            disabledProviders = disabledProviders,
            customPrivacyRules = customRules
        )
    }
}

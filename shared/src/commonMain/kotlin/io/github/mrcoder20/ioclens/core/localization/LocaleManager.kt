package io.github.mrcoder20.ioclens.core.localization

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Centralized singleton managing reactive application language state,
 * string retrieval, and number/score formatting.
 */
object LocaleManager {
    private val _currentLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    fun getStrings(language: AppLanguage = currentLanguage.value): AppStrings {
        return LocalizationProvider.getStrings(language)
    }

    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
    }

    fun formatNumber(number: Number, language: AppLanguage = currentLanguage.value): String {
        return LocalizationProvider.formatNumber(number, language)
    }

    fun formatScore(score: Int, language: AppLanguage = currentLanguage.value): String {
        return LocalizationProvider.formatScore(score, language)
    }
}

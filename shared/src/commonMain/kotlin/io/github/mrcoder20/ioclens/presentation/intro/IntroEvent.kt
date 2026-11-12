package io.github.mrcoder20.ioclens.presentation.intro

import io.github.mrcoder20.ioclens.core.localization.AppLanguage

sealed interface IntroEvent {
    data class LanguageChanged(val language: AppLanguage) : IntroEvent
    data object SkipClicked : IntroEvent
    data object CompleteClicked : IntroEvent
    data object Reset : IntroEvent
}


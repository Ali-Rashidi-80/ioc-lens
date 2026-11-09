package io.github.mrcoder20.ioclens.presentation.intro

import io.github.mrcoder20.ioclens.core.localization.AppLanguage

sealed interface IntroEvent {
    data class LanguageChanged(val language: AppLanguage) : IntroEvent
    object SkipClicked : IntroEvent
    object CompleteClicked : IntroEvent
    object Reset : IntroEvent
}

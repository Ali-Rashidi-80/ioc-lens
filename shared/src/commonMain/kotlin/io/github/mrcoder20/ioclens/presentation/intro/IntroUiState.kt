package io.github.mrcoder20.ioclens.presentation.intro

import io.github.mrcoder20.ioclens.core.localization.AppLanguage
import io.github.mrcoder20.ioclens.domain.model.IntroSlide
import io.github.mrcoder20.ioclens.getSystemLanguage

/**
 * Single Source of Truth: Pager slide index is bound strictly to Compose PagerState.
 * IntroUiState only maintains slide definitions, completion status, and active language.
 */
data class IntroUiState(
    val slides: List<IntroSlide> = IntroSlideData.getSlides(getSystemLanguage()),
    val isCompleted: Boolean = false,
    val currentLanguage: AppLanguage = getSystemLanguage()
)

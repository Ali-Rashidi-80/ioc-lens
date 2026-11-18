package io.github.mrcoder20.ioclens.presentation.intro

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mrcoder20.ioclens.core.components.CountryFlag
import io.github.mrcoder20.ioclens.core.components.CyberPolygonBackground
import io.github.mrcoder20.ioclens.core.components.LiquidmorphicBackground
import io.github.mrcoder20.ioclens.core.components.SlideAnimatedGraphic
import io.github.mrcoder20.ioclens.core.components.TypewriterText
import io.github.mrcoder20.ioclens.core.components.slider.OnboardingSlider
import io.github.mrcoder20.ioclens.core.components.slider.PageTransitionStyle
import io.github.mrcoder20.ioclens.core.localization.AppLanguage
import io.github.mrcoder20.ioclens.core.localization.LocalizationProvider
import io.github.mrcoder20.ioclens.core.theme.LocalIocColors
import io.github.mrcoder20.ioclens.core.theme.rememberMonospaceFontFamily
import io.github.mrcoder20.ioclens.domain.model.IntroSlide
import kotlin.math.abs

/**
 * Ultra-Sleek Glassmorphic Intro Screen with Parallax Zoom, Liquidmorphism & Cyber Polygon animations.
 * Features Compottie Lottie animations, elastic WormDotsIndicator, and zero closing lag.
 */
@Composable
fun IntroScreen(
    viewModel: IntroViewModel,
    onIntroFinished: () -> Unit,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { uiState.slides.size })
    var isLanguageMenuExpanded by remember { mutableStateOf(false) }
    var isClosing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.onEvent(IntroEvent.Reset)
        if (pagerState.currentPage != 0) {
            pagerState.scrollToPage(0)
        }
    }

    val layoutDirection = if (uiState.currentLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    val handleFinish: () -> Unit = {
        if (!isClosing) {
            isClosing = true
            viewModel.onEvent(IntroEvent.CompleteClicked)
            onIntroFinished()
        }
    }

    val handleSkip: () -> Unit = {
        if (!isClosing) {
            isClosing = true
            viewModel.onEvent(IntroEvent.SkipClicked)
            onIntroFinished()
        }
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection
    ) {
        Box(
            modifier = modifier.fillMaxSize()
        ) {
            // 1. Liquidmorphism Animated Background Blobs (Paused on exit to eliminate closing lag)
            LiquidmorphicBackground(
                modifier = Modifier.fillMaxSize(),
                isPaused = isClosing
            )

            // 2. Fractured Cyber Polygon Mesh Background (Paused on exit to eliminate closing lag)
            CyberPolygonBackground(
                modifier = Modifier.fillMaxSize(),
                isPaused = isClosing
            )

            // 3. Onboarding Slider Container with Parallax Zoom GPU Transition & Worm Dots
            val currentStrings = remember(uiState.currentLanguage) { LocalizationProvider.getStrings(uiState.currentLanguage) }
            val palette = LocalIocColors.current
            val monoFont = rememberMonospaceFontFamily()
            OnboardingSlider(
                items = uiState.slides,
                pagerState = pagerState,
                transitionStyle = PageTransitionStyle.PARALLAX_ZOOM,
                doneText = currentStrings.done,
                nextText = currentStrings.next,
                skipText = currentStrings.skip,
                onFinishClicked = handleFinish,
                onSkipClicked = handleSkip,
                topBarContent = {
                    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

                    val titleBadge = @Composable {
                        Surface(
                            color = palette.accentPrimary.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, palette.accentPrimary.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = "IOC LENS",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = monoFont,
                                color = palette.accentPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }

                    val actionsGroup = @Composable {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (onClose != null && isRtl) {
                                Surface(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable { onClose() }
                                        .pointerHoverIcon(PointerIcon.Hand),
                                    color = palette.surfaceCard.copy(alpha = 0.88f),
                                    shape = CircleShape,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, palette.borderStrong)
                                ) {
                                    Text(
                                        text = "✕",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        color = palette.textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Country Flag Language Selector
                            Box {
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { isLanguageMenuExpanded = true }
                                        .pointerHoverIcon(PointerIcon.Hand),
                                    color = palette.surfaceCard.copy(alpha = 0.88f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, palette.borderSubtle)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        CountryFlag(language = uiState.currentLanguage, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = uiState.currentLanguage.displayName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.textPrimary,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = "▾",
                                            color = palette.accentPrimary,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = isLanguageMenuExpanded,
                                    onDismissRequest = { isLanguageMenuExpanded = false },
                                    modifier = Modifier.background(palette.surfaceCard)
                                ) {
                                    AppLanguage.entries.forEach { lang ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    CountryFlag(language = lang, modifier = Modifier.size(16.dp))
                                                    Text(
                                                        text = lang.displayName,
                                                        color = if (lang == uiState.currentLanguage) palette.accentPrimary else palette.textPrimary,
                                                        fontWeight = if (lang == uiState.currentLanguage) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                }
                                            },
                                            onClick = {
                                                viewModel.onEvent(IntroEvent.LanguageChanged(lang))
                                                isLanguageMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            if (onClose != null && !isRtl) {
                                Surface(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable { onClose() }
                                        .pointerHoverIcon(PointerIcon.Hand),
                                    color = palette.surfaceCard.copy(alpha = 0.88f),
                                    shape = CircleShape,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, palette.borderStrong)
                                ) {
                                    Text(
                                        text = "✕",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        color = palette.textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        titleBadge()
                        actionsGroup()
                    }
                }
            ) { slide, page ->
                val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                val isVisibleSlide = abs(pageOffset) < 1.05f
                IntroSlideCard(
                    slide = slide,
                    isScrollInProgress = pagerState.isScrollInProgress,
                    isActive = isVisibleSlide && !isClosing
                )
            }
        }
    }
}

@Composable
private fun IntroSlideCard(
    slide: IntroSlide,
    isScrollInProgress: Boolean,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = LocalIocColors.current
    val monoFont = rememberMonospaceFontFamily()
    // Translucent Glassmorphic Card
    Card(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = palette.surfaceCard.copy(alpha = if (palette.isDark) 0.55f else 0.96f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.2.dp,
            Brush.linearGradient(
                colors = listOf(
                    palette.borderStrong,
                    palette.accentPrimary.copy(alpha = 0.45f),
                    palette.borderSubtle
                )
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (palette.isDark) 0.dp else 4.dp)
    ) {
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Top Lottie / Vector Graphic (140dp)
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                SlideAnimatedGraphic(
                    slideId = slide.id,
                    emoji = slide.iconEmoji,
                    size = 140.dp,
                    emojiFontSize = 36,
                    isScrollInProgress = isScrollInProgress,
                    isActive = isActive
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Status Badge Pill
            Surface(
                color = palette.surfaceInset.copy(alpha = 0.9f),
                shape = RoundedCornerShape(50),
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.accentPrimary.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(palette.accentPrimary)
                    )
                    Text(
                        text = slide.badgeText,
                        color = palette.accentPrimary,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = monoFont,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Responsive Title
            Text(
                text = slide.title,
                style = MaterialTheme.typography.headlineMedium,
                color = palette.textPrimary,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                fontSize = 20.sp,
                lineHeight = 26.sp,
                letterSpacing = (-0.02).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (slide.subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = slide.subtitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.accentPrimary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    fontSize = 12.5.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // High-Performance Description Text
            TypewriterText(
                text = slide.description,
                style = MaterialTheme.typography.bodyLarge,
                color = palette.textSecondary,
                textAlign = TextAlign.Center,
                fontSize = 13.5.sp,
                lineHeight = 20.sp
            )
        }
    }
}

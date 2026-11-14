package io.github.mrcoder20.ioclens.core.components.slider

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.github.mrcoder20.ioclens.core.components.springPress
import io.github.mrcoder20.ioclens.core.theme.BackgroundDark
import io.github.mrcoder20.ioclens.core.theme.PrimaryCyan
import io.github.mrcoder20.ioclens.core.theme.SurfaceDark
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

enum class PageTransitionStyle {
    CLOCK_DIAL_360,
    CAROUSEL_WHEEL_3D,
    FADE,
    PARALLAX_ZOOM,
    CUBE_3D,
    DEPTH_SLIDE,
    BOOK_FLIP_3D
}

enum class OnboardingArrowDirection {
    LEFT,
    RIGHT
}

/**
 * Returns directional orientation for forward Next arrow.
 * In RTL (Persian), advancing forward points LEFT (←).
 * In LTR (English, Russian), advancing forward points RIGHT (→).
 */
fun getOnboardingNextArrowDirection(isRtl: Boolean): OnboardingArrowDirection {
    return if (isRtl) OnboardingArrowDirection.LEFT else OnboardingArrowDirection.RIGHT
}

/**
 * Determines whether Skip action is visible.
 */
fun shouldShowOnboardingSkip(pageIndex: Int, pageCount: Int): Boolean {
    return pageIndex < pageCount - 1
}

/**
 * High-performance Onboarding Slider with Single Source of Truth PagerState.
 * - Single 2-element bottom control deck (Worm Dots + Next/DONE) with zero vertical layout shift.
 * - Smooth 120 FPS GPU-accelerated parallax/zoom transitions.
 * - Debounced finish action to eliminate close lag and double executions.
 */
@Composable
fun <T> OnboardingSlider(
    items: List<T>,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    transitionStyle: PageTransitionStyle = PageTransitionStyle.PARALLAX_ZOOM,
    containerBackgroundColor: Color = Color(0xFF06080D).copy(alpha = 0.82f),
    topBarContent: @Composable (() -> Unit)? = null,
    onFinishClicked: () -> Unit,
    onSkipClicked: (() -> Unit)? = null,
    doneText: String = "DONE",
    nextText: String? = null,
    skipText: String? = null,
    pageContent: @Composable (item: T, page: Int) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == items.size - 1
    val focusRequester = remember { FocusRequester() }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    var isClosing by remember { mutableStateOf(false) }

    val safeFinishClicked: () -> Unit = {
        if (!isClosing) {
            isClosing = true
            onFinishClicked()
        }
    }

    val animSpec = remember { tween<Float>(durationMillis = 350, easing = FastOutSlowInEasing) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(containerBackgroundColor)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionRight, Key.PageDown -> {
                            val moveForward = !isRtl
                            if (moveForward && pagerState.currentPage < items.size - 1) {
                                coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1, animationSpec = animSpec) }
                                true
                            } else if (!moveForward && pagerState.currentPage > 0) {
                                coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1, animationSpec = animSpec) }
                                true
                            } else false
                        }
                        Key.DirectionLeft, Key.PageUp -> {
                            val moveBack = !isRtl
                            if (moveBack && pagerState.currentPage > 0) {
                                coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1, animationSpec = animSpec) }
                                true
                            } else if (!moveBack && pagerState.currentPage < items.size - 1) {
                                coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1, animationSpec = animSpec) }
                                true
                            } else false
                        }
                        Key.DirectionDown -> {
                            if (pagerState.currentPage < items.size - 1) {
                                coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1, animationSpec = animSpec) }
                                true
                            } else false
                        }
                        Key.DirectionUp -> {
                            if (pagerState.currentPage > 0) {
                                coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1, animationSpec = animSpec) }
                                true
                            } else false
                        }
                        Key.Enter, Key.NumPadEnter, Key.Spacebar -> {
                            if (isLastPage) {
                                safeFinishClicked()
                            } else {
                                coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1, animationSpec = animSpec) }
                            }
                            true
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        // Top Mesh Glow Gradient matching Header #041C24
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(420.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            BackgroundDark.copy(alpha = 0.55f),
                            SurfaceDark.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Bar Slot (Title Badge + Language Switcher + Skip Button)
            if (topBarContent != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .zIndex(10f)
                ) {
                    topBarContent()
                }
            }

            // Pager Deck with GPU-accelerated Parallax Zoom & 3D Transitions
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                val item = items[page]

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .graphicsLayer {
                            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                            val absOffset = pageOffset.absoluteValue
                            val signOffset = if (pageOffset > 0f) 1f else if (pageOffset < 0f) -1f else 0f

                            when (transitionStyle) {
                                PageTransitionStyle.CLOCK_DIAL_360 -> {
                                    val angle = pageOffset * 22f
                                    transformOrigin = TransformOrigin(0.5f, 1.8f)
                                    rotationZ = angle
                                    alpha = (1f - absOffset * 0.45f).coerceIn(0.15f, 1f)
                                }
                                PageTransitionStyle.CAROUSEL_WHEEL_3D -> {
                                    val angle = signOffset * -42f
                                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                                    rotationY = angle
                                    cameraDistance = 14f * density
                                    scaleX = 0.88f + (1f - absOffset) * 0.12f
                                    scaleY = 0.88f + (1f - absOffset) * 0.12f
                                    translationX = -signOffset * size.width * 0.22f
                                    alpha = (1f - absOffset * 0.45f).coerceIn(0.15f, 1f)
                                }
                                PageTransitionStyle.FADE -> {
                                    alpha = (1f - absOffset * 1.2f).coerceIn(0f, 1f)
                                    scaleX = 0.96f + (1f - absOffset) * 0.04f
                                    scaleY = 0.96f + (1f - absOffset) * 0.04f
                                    translationX = -signOffset * size.width * 0.05f
                                }
                                PageTransitionStyle.PARALLAX_ZOOM -> {
                                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                                    scaleX = 0.92f + (1f - absOffset) * 0.08f
                                    scaleY = 0.92f + (1f - absOffset) * 0.08f
                                    alpha = (1f - absOffset * 0.5f).coerceIn(0.2f, 1f)
                                    translationX = -signOffset * size.width * 0.15f
                                }
                                PageTransitionStyle.CUBE_3D -> {
                                    val pivotX = if (signOffset < 0f) 1f else 0f
                                    transformOrigin = TransformOrigin(pivotX, 0.5f)
                                    rotationY = signOffset * 60f
                                    alpha = (1f - absOffset * 0.4f).coerceIn(0.2f, 1f)
                                    cameraDistance = 12f * density
                                }
                                PageTransitionStyle.DEPTH_SLIDE -> {
                                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                                    if (pageOffset > 0f) {
                                        scaleX = 0.85f + (1f - absOffset) * 0.15f
                                        scaleY = 0.85f + (1f - absOffset) * 0.15f
                                        alpha = (1f - absOffset * 0.7f).coerceIn(0.2f, 1f)
                                    } else {
                                        translationX = -signOffset * size.width * absOffset
                                        alpha = 1f
                                    }
                                }
                                PageTransitionStyle.BOOK_FLIP_3D -> {
                                    val pivotX = if (isRtl) (if (signOffset > 0f) 1f else 0f) else (if (signOffset < 0f) 1f else 0f)
                                    transformOrigin = TransformOrigin(pivotX, 0.5f)
                                    rotationY = signOffset * -85f
                                    cameraDistance = 16f * density
                                    alpha = (1f - absOffset * 0.55f).coerceIn(0.1f, 1f)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    pageContent(item, page)
                }
            }

            // Bottom Navigation Deck (Worm Dots Indicator on Side 1 + Next/DONE Button on Side 2)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .zIndex(10f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Side 1: Pagination Indicator Dots (Worm Dots)
                    WormDotsIndicator(
                        pageCount = items.size,
                        currentPage = pagerState.currentPage,
                        currentPageOffsetFraction = pagerState.currentPageOffsetFraction,
                        onDotClick = { targetPage ->
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(targetPage, animationSpec = animSpec)
                            }
                        }
                    )

                    // Side 2: Next (Slides 1-3) or DONE Button (Slide 4)
                    if (isLastPage) {
                        Button(
                            onClick = safeFinishClicked,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryCyan,
                                contentColor = Color(0xFF041019)
                            ),
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 26.dp, vertical = 10.dp),
                            modifier = Modifier
                                .height(44.dp)
                                .border(1.2.dp, PrimaryCyan, RoundedCornerShape(50))
                                .springPress()
                        ) {
                            Text(
                                text = doneText,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = Color(0xFF041019),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1, animationSpec = animSpec)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF131C30).copy(alpha = 0.92f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                            modifier = Modifier
                                .height(44.dp)
                                .border(1.2.dp, PrimaryCyan.copy(alpha = 0.65f), RoundedCornerShape(50))
                                .springPress()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (nextText != null) {
                                    Text(
                                        text = nextText,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                }
                                VectorNextArrow(isRtl = isRtl)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Crisp Vector Path Arrow Icon for Next Button.
 * In RTL (Persian), arrow points Left (←) towards reading progression.
 * In LTR (English, Russian), arrow points Right (→).
 */
@Composable
fun VectorNextArrow(
    isRtl: Boolean,
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Canvas(modifier = modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 2.2.dp.toPx()

        if (isRtl) {
            // Arrow points Left (←)
            drawLine(
                color = color,
                start = Offset(w * 0.72f, h * 0.5f),
                end = Offset(w * 0.28f, h * 0.5f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            val path = Path().apply {
                moveTo(w * 0.48f, h * 0.28f)
                lineTo(w * 0.28f, h * 0.5f)
                lineTo(w * 0.48f, h * 0.72f)
            }
            drawPath(
                path = path,
                color = color,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        } else {
            // Arrow points Right (→)
            drawLine(
                color = color,
                start = Offset(w * 0.28f, h * 0.5f),
                end = Offset(w * 0.72f, h * 0.5f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            val path = Path().apply {
                moveTo(w * 0.52f, h * 0.28f)
                lineTo(w * 0.72f, h * 0.5f)
                lineTo(w * 0.52f, h * 0.72f)
            }
            drawPath(
                path = path,
                color = color,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}


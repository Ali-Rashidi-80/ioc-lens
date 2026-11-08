package io.github.mrcoder20.ioclens.core.components

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

/**
 * Premium Continuous Train-Scroll (Marquee) Looper.
 *
 * Designed for 120 FPS high-refresh multiplatform rendering (Skiko Desktop JVM + Android Compose).
 *
 * Features:
 * - Thread-controlled linear scroll driven by Compose's display frame clock (zero hitch, zero GC churn).
 * - Instant pause on mouse hover (Desktop) and touch hold (Mobile) for effortless user clicking.
 * - Gentle grace-period resume after interaction releases.
 * - Infinite seamless cyclic wrap-around with zero visual jump.
 * - Hardware-accelerated fading alpha edge masks.
 */
@Composable
fun <T> TrainMarqueeRow(
    items: List<T>,
    modifier: Modifier = Modifier,
    velocityDpPerSecond: Float = 32f,
    spacing: Dp = 8.dp,
    pauseOnHover: Boolean = true,
    fadeEdges: Boolean = true,
    fadeWidth: Dp = 20.dp,
    itemContent: @Composable (item: T) -> Unit
) {
    if (items.isEmpty()) return

    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    var cycleWidthPx by remember { mutableStateOf(0) }

    // If user is actively dragging or holding, pause auto-scroll
    val isPaused = (pauseOnHover && isHovered) || isPressed || scrollState.isScrollInProgress

    LaunchedEffect(isPaused, cycleWidthPx, items) {
        if (cycleWidthPx <= 0 || isPaused) return@LaunchedEffect

        // Small grace pause after hover/interaction release
        delay(350)

        val pxPerSec = with(density) { velocityDpPerSecond.dp.toPx() }

        try {
            scrollState.scroll(MutatePriority.Default) {
                var lastFrameNanos = 0L
                while (isActive && !isPaused) {
                    withFrameNanos { frameTimeNanos ->
                        if (lastFrameNanos != 0L) {
                            val dtSec = (frameTimeNanos - lastFrameNanos) / 1_000_000_000f
                            val deltaPx = pxPerSec * dtSec
                            scrollBy(deltaPx)

                            // Seamless cyclic wrap when past one full cycle
                            if (cycleWidthPx > 0 && scrollState.value >= cycleWidthPx) {
                                scrollBy(-cycleWidthPx.toFloat())
                            }
                        }
                        lastFrameNanos = frameTimeNanos
                    }
                }
            }
        } catch (_: CancellationException) {
            // Normal cancellation when user gestures or recomposition occurs
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .hoverable(interactionSource)
            .then(
                if (fadeEdges && cycleWidthPx > 0) {
                    Modifier
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            val fadePx = fadeWidth.toPx().coerceAtMost(size.width / 4f)
                            if (fadePx > 0f) {
                                // Start edge fade
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Color.Transparent, Color.Black),
                                        startX = 0f,
                                        endX = fadePx
                                    ),
                                    blendMode = BlendMode.DstIn,
                                    size = Size(fadePx, size.height)
                                )
                                // End edge fade
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Color.Black, Color.Transparent),
                                        startX = size.width - fadePx,
                                        endX = size.width
                                    ),
                                    blendMode = BlendMode.DstIn,
                                    topLeft = Offset(size.width - fadePx, 0f),
                                    size = Size(fadePx, size.height)
                                )
                            }
                        }
                } else Modifier
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cycle 1: Primary items (measured to establish loop period)
            Row(
                modifier = Modifier.onGloballyPositioned { layoutCoordinates ->
                    val measuredWidth = layoutCoordinates.size.width
                    val spacingPx = with(density) { spacing.toPx().roundToInt() }
                    if (measuredWidth > 0 && measuredWidth + spacingPx != cycleWidthPx) {
                        cycleWidthPx = measuredWidth + spacingPx
                    }
                },
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    itemContent(item)
                }
            }

            // Cycle 2: Duplicate set for zero-seam wrap-around
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    itemContent(item)
                }
            }

            // Cycle 3: Triplicate set to ensure seamless coverage on wide widescreen displays
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    itemContent(item)
                }
            }
        }
    }
}


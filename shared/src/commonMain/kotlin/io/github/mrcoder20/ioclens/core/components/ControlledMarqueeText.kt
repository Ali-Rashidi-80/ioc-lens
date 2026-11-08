package io.github.mrcoder20.ioclens.core.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

/**
 * Universal Hardware-Accelerated Marquee Text.
 *
 * Automatically detects when text overflows its allocated layout container,
 * and engages a smooth, 120 FPS ping-pong marquee animation (Start -> Tail -> Start).
 *
 * If the text fits comfortably inside the container, it remains completely static.
 *
 * BiDi Layout-Aware:
 * - In LTR (English): Glides leftward to reveal the hidden right tail, then returns.
 * - In RTL (Persian): Glides rightward to reveal the hidden left tail, then returns.
 */
@Composable
fun ControlledMarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    style: TextStyle = LocalTextStyle.current,
    textAlign: TextAlign? = null,
    velocityDpPerSecond: Float = 32f,
    startPauseMs: Long = 1400L,
    endPauseMs: Long = 1200L
) {
    val density = LocalDensity.current
    val isRtl = LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl

    var overflowPx by remember(text) { mutableFloatStateOf(0f) }
    val anim = remember(text) { Animatable(0f) }

    LaunchedEffect(overflowPx, isRtl, text) {
        if (overflowPx <= 6f) {
            anim.snapTo(0f)
            return@LaunchedEffect
        }

        val pxPerSec = with(density) { velocityDpPerSecond.dp.toPx() }
        val durationMs = ((overflowPx / pxPerSec) * 1000).toInt().coerceIn(600, 6000)
        val returnDurationMs = (durationMs * 0.7f).toInt().coerceIn(400, 4000)
        val targetTranslation = if (isRtl) overflowPx else -overflowPx

        while (isActive) {
            try {
                // Initial calm reading pause at start
                anim.snapTo(0f)
                delay(startPauseMs)

                // Smooth linear glide to reveal overflowing tail
                anim.animateTo(
                    targetValue = targetTranslation,
                    animationSpec = tween(durationMillis = durationMs, easing = LinearEasing)
                )

                // Calm pause to let user read the tail
                delay(endPauseMs)

                // Smooth spring-like return to start
                anim.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = returnDurationMs, easing = FastOutSlowInEasing)
                )

                // Pause before next cycle
                delay(startPauseMs / 2)
            } catch (_: CancellationException) {
                break
            }
        }
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .layout { measurable, constraints ->
                // Measure the text unconstrained to determine its true natural width
                val placeable = measurable.measure(
                    constraints.copy(
                        minWidth = 0,
                        maxWidth = Constraints.Infinity
                    )
                )
                val textW = placeable.width.toFloat()
                val boxWidth = if (constraints.hasBoundedWidth) {
                    placeable.width.coerceIn(constraints.minWidth, constraints.maxWidth)
                } else {
                    placeable.width.coerceAtLeast(constraints.minWidth)
                }
                val containerW = boxWidth.toFloat()
                val detectedOverflow = (textW - containerW).coerceAtLeast(0f)

                if (kotlin.math.abs(detectedOverflow - overflowPx) > 1f) {
                    overflowPx = detectedOverflow
                }

                layout(boxWidth, placeable.height) {
                    placeable.placeRelative(0, 0)
                }
            }
    ) {
        Text(
            text = text,
            modifier = Modifier.graphicsLayer {
                translationX = anim.value
            },
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            style = style,
            textAlign = textAlign,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
    }
}

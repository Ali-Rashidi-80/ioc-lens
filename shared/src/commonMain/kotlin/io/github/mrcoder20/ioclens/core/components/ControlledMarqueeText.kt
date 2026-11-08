package io.github.mrcoder20.ioclens.core.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

/**
 * Thread-Safe, Hardware-Accelerated Marquee Text.
 *
 * Replaces unreliable Compose Desktop Skiko `basicMarquee` with a dedicated,
 * coroutine-controlled, 120 FPS linear animation loop that activates ONLY when text overflows.
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
    velocityDpPerSecond: Float = 30f,
    startPauseMs: Long = 1400L,
    endPauseMs: Long = 1200L
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    var textWidthPx by remember { mutableStateOf(0) }
    var containerWidthPx by remember { mutableStateOf(0) }

    val overflows = textWidthPx > containerWidthPx && containerWidthPx > 0

    LaunchedEffect(overflows, textWidthPx, containerWidthPx, text) {
        if (!overflows) {
            scrollState.scrollTo(0)
            return@LaunchedEffect
        }

        val overflowDistance = (textWidthPx - containerWidthPx).toFloat()
        val pxPerSec = with(density) { velocityDpPerSecond.dp.toPx() }
        val durationMs = ((overflowDistance / pxPerSec) * 1000).toInt().coerceAtLeast(300)

        while (isActive) {
            try {
                // Initial calm pause
                scrollState.scrollTo(0)
                delay(startPauseMs)

                // Smooth linear scroll to end
                scrollState.animateScrollTo(
                    value = overflowDistance.roundToInt(),
                    animationSpec = tween(durationMillis = durationMs, easing = LinearEasing)
                )

                // Pause at end
                delay(endPauseMs)

                // Smooth reset
                scrollState.animateScrollTo(0, tween(400))
                delay(600)
            } catch (_: CancellationException) {
                break
            }
        }
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .onGloballyPositioned { containerWidthPx = it.size.width }
    ) {
        Text(
            text = text,
            modifier = Modifier
                .let { if (overflows) it.horizontalScroll(scrollState, enabled = false) else it }
                .onGloballyPositioned { textWidthPx = it.size.width },
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            style = style,
            textAlign = textAlign,
            maxLines = 1,
            softWrap = false,
            overflow = if (overflows) TextOverflow.Clip else TextOverflow.Ellipsis
        )
    }
}


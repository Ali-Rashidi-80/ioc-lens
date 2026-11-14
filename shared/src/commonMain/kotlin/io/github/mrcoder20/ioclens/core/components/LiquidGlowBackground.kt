package io.github.mrcoder20.ioclens.core.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import io.github.mrcoder20.ioclens.core.localization.AppTheme
import io.github.mrcoder20.ioclens.core.theme.ElectricIndigo
import io.github.mrcoder20.ioclens.core.theme.LocalAccentColor
import io.github.mrcoder20.ioclens.core.theme.LocalAppTheme
import io.github.mrcoder20.ioclens.core.theme.SecondaryNeon

/**
 * Signature Cyber Liquid Glow Background ported directly from PortX.
 *
 * Visual Features:
 * - Primary ambient radial cyber beacon in PrimaryNeon (Cyan) with breathing cycle
 * - Secondary aurora radial glow in SecondaryNeon (Purple) / ElectricIndigo
 * - Cybersecurity coordinate grid (64.dp spacing, micro-alpha)
 * - Deep ambient pointer illumination follow and cyber reticle rendered natively in background
 * - Self-contained pointer tracking preventing root recomposition overhead
 */
@Composable
fun LiquidGlowBackground(
    modifier: Modifier = Modifier,
    isDark: Boolean = true,
    accent: Color = LocalAccentColor.current,
    content: @Composable () -> Unit = {}
) {
    val appTheme = LocalAppTheme.current
    var pointerOffset by remember { mutableStateOf(Offset.Zero) }
    var isPointerActive by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "LiquidGlowInfinite")

    val breathAnimState = infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathAnim"
    )

    val auroraShiftState = infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = 50f,
        animationSpec = infiniteRepeatable(
            animation = tween(11000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auroraShift"
    )

    val ringScaleState = animateFloatAsState(
        targetValue = if (isPointerActive) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "pointerRingScale"
    )

    val ringAlphaState = animateFloatAsState(
        targetValue = if (isPointerActive) 0.35f else 0f,
        animationSpec = tween(300),
        label = "pointerRingAlpha"
    )

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        isPointerActive = event.type != PointerEventType.Exit
                        if (event.changes.isNotEmpty()) {
                            pointerOffset = event.changes.first().position
                        }
                    }
                }
            }
    ) {
        // 1. Background Grid & Ambient Breathing Beacon Glows
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val breathAnim = breathAnimState.value
            val auroraShift = auroraShiftState.value
            val canvasSize = size
            val centerOffset = Offset(canvasSize.width * 0.5f, canvasSize.height * 0.45f)
            val secondaryCenter = Offset(canvasSize.width * 0.8f + auroraShift, canvasSize.height * 0.2f)

            // In Light Mode: draw an ambient daylight wash from top to anchor spatial orientation
            if (!isDark) {
                drawRect(
                    brush = Brush.verticalGradient(
                        0.0f to Color.White.copy(alpha = 0.65f),
                        0.4f to Color.White.copy(alpha = 0.20f),
                        1.0f to Color.Transparent
                    ),
                    size = canvasSize
                )
            }

            val gridSpacing = 64.dp.toPx()
            val gridAlpha = if (isDark) {
                if (appTheme == AppTheme.IOC_LENS_CLASSIC) 0.018f else 0.025f
            } else {
                0.035f
            }
            val gridColor = if (isDark) Color.White else Color(0xFF0F172A)

            // Subtle cybersecurity coordinate grid
            val horizontalLines = (canvasSize.width / gridSpacing).toInt()
            for (x in 0..horizontalLines) {
                drawLine(
                    color = gridColor.copy(alpha = gridAlpha),
                    start = Offset(x * gridSpacing, 0f),
                    end = Offset(x * gridSpacing, canvasSize.height),
                    strokeWidth = 1f
                )
            }
            val verticalLines = (canvasSize.height / gridSpacing).toInt()
            for (y in 0..verticalLines) {
                drawLine(
                    color = gridColor.copy(alpha = gridAlpha),
                    start = Offset(0f, y * gridSpacing),
                    end = Offset(canvasSize.width, y * gridSpacing),
                    strokeWidth = 1f
                )
            }

            // Primary ambient radial beacon
            val primaryRadius = (canvasSize.width * 0.65f * breathAnim).coerceAtLeast(10f)
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to accent.copy(alpha = if (isDark) 0.10f else 0.06f),
                    0.6f to accent.copy(alpha = if (isDark) 0.035f else 0.015f),
                    1.0f to Color.Transparent,
                    center = centerOffset,
                    radius = primaryRadius
                )
            )

            // Secondary aurora glow (Purple in Dark, Subtle Indigo in Light)
            val secondaryGlowColor = if (isDark) SecondaryNeon else ElectricIndigo
            val secondaryRadius = (canvasSize.width * 0.45f * breathAnim).coerceAtLeast(10f)
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to secondaryGlowColor.copy(alpha = if (isDark) 0.07f else 0.04f),
                    1.0f to Color.Transparent,
                    center = secondaryCenter,
                    radius = secondaryRadius
                )
            )
        }

        // 2. Main Application Content
        content()

        // 3. Foreground Tactical Cyber Mouse Tracker Reticle (Smooth, Touch-Transparent, Zero Click Interception, Draw-Phase Only)
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val scale = ringScaleState.value
            val alpha = ringAlphaState.value
            val pos = pointerOffset
            if (pos == Offset.Zero || scale <= 0.02f || alpha <= 0.01f) return@Canvas

            val px = pos.x
            val py = pos.y
            val lineLength = 20.dp.toPx()
            val centerGap = 6.dp.toPx()
            val reticleColor = accent.copy(alpha = alpha)

            // A. Crosshair Ticks with Center Clearance (preserves cursor visibility)
            drawLine(reticleColor, Offset(px, py - lineLength), Offset(px, py - centerGap), 1.5f)
            drawLine(reticleColor, Offset(px, py + centerGap), Offset(px, py + lineLength), 1.5f)
            drawLine(reticleColor, Offset(px - lineLength, py), Offset(px - centerGap, py), 1.5f)
            drawLine(reticleColor, Offset(px + centerGap, py), Offset(px + lineLength, py), 1.5f)

            // B. Primary Targeting Ring with smooth spring scale
            val primaryRingRadius = (75.dp.toPx() * scale).coerceAtLeast(1f)
            drawCircle(
                color = accent.copy(alpha = alpha * 0.55f),
                center = pos,
                radius = primaryRingRadius,
                style = Stroke(width = 1.5f)
            )

            // C. Tactical Concentric Perimeter Ring (Solid stroke for zero native nullptr risk)
            val outerRingRadius = (95.dp.toPx() * scale).coerceAtLeast(1f)
            drawCircle(
                color = accent.copy(alpha = alpha * 0.22f),
                center = pos,
                radius = outerRingRadius,
                style = Stroke(width = 1f)
            )

            // D. Subtle Ambient Pointer Halo
            val haloRadius = (45.dp.toPx() * scale).coerceAtLeast(1f)
            drawCircle(
                color = accent.copy(alpha = alpha * 0.06f),
                center = pos,
                radius = haloRadius
            )
        }
    }
}

/**
 * CyberMouseTrackerOverlay compatibility stub:
 * Drawing is now handled natively on the background canvas inside [LiquidGlowBackground]
 * to prevent any click-interception or focus conflicts with interactive controls.
 */
@Composable
fun CyberMouseTrackerOverlay(
    pointerOffset: Offset,
    isPointerActive: Boolean,
    modifier: Modifier = Modifier,
    accent: Color = LocalAccentColor.current
) {
    // Intentionally empty: rendered directly on the background canvas to avoid overlay conflicts.
}

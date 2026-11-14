package io.github.mrcoder20.ioclens.core.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.mrcoder20.ioclens.core.localization.AppTheme
import io.github.mrcoder20.ioclens.core.theme.LocalAccentColor
import io.github.mrcoder20.ioclens.core.theme.LocalAppTheme

/**
 * Minimalist Cyber Technical Background.
 *
 * Visual Features:
 * - Subtle, high-precision cybersecurity coordinate grid (64.dp spacing, micro-alpha)
 * - Clean spatial backdrop without intrusive circular artifacts or custom cursor reticles
 * - Uses native system mouse cursor seamlessly without overhead
 */
@Composable
fun LiquidGlowBackground(
    modifier: Modifier = Modifier,
    isDark: Boolean = true,
    accent: Color = LocalAccentColor.current,
    content: @Composable () -> Unit = {}
) {
    val appTheme = LocalAppTheme.current

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // 1. Background Grid & Clean Spatial Backdrop
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = size

            // In Light Mode: draw an ambient daylight wash from top
            if (!isDark) {
                drawRect(
                    brush = Brush.verticalGradient(
                        0.0f to Color.White.copy(alpha = 0.65f),
                        0.4f to Color.White.copy(alpha = 0.20f),
                        1.0f to Color.Transparent
                    ),
                    size = canvasSize
                )
            } else {
                // Subtle top ambient cyber accent wash
                drawRect(
                    brush = Brush.verticalGradient(
                        0.0f to accent.copy(alpha = 0.04f),
                        0.35f to Color.Transparent
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
        }

        // 2. Main Application Content (Standard OS pointer cursor is preserved)
        content()
    }
}

/**
 * CyberMouseTrackerOverlay compatibility stub:
 * Custom mouse tracking has been removed in favor of native system cursor.
 */
@Composable
fun CyberMouseTrackerOverlay(
    pointerOffset: Offset = Offset.Zero,
    isPointerActive: Boolean = false,
    modifier: Modifier = Modifier,
    accent: Color = LocalAccentColor.current
) {
    // Intentionally empty: native system cursor is active
}

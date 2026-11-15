package io.github.mrcoder20.ioclens.core.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Clean, standard vector icons replacing casual emojis across navigation bars and buttons.
 * Optimized for high-DPI displays with pixel-aligned paths and responsive tinting.
 */

@Composable
fun ScannerVectorIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = 1.8.dp.toPx()

        // Magnifying glass lens / Scanner reticle
        val radius = w * 0.32f
        val center = Offset(w * 0.42f, h * 0.42f)
        drawCircle(
            color = tint,
            radius = radius,
            center = center,
            style = Stroke(width = stroke)
        )

        // Handle
        val handleStart = Offset(w * 0.65f, h * 0.65f)
        val handleEnd = Offset(w * 0.88f, h * 0.88f)
        drawLine(
            color = tint,
            start = handleStart,
            end = handleEnd,
            strokeWidth = stroke * 1.3f,
            cap = StrokeCap.Round
        )

        // Center reticle dot
        drawCircle(
            color = tint,
            radius = stroke * 0.65f,
            center = center
        )
    }
}

@Composable
fun HistoryVectorIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = 1.8.dp.toPx()
        val center = Offset(w * 0.5f, h * 0.5f)

        // Clock circle
        drawCircle(
            color = tint,
            radius = w * 0.42f,
            center = center,
            style = Stroke(width = stroke)
        )

        // Clock hour hand
        drawLine(
            color = tint,
            start = center,
            end = Offset(center.x, h * 0.24f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )

        // Clock minute hand
        drawLine(
            color = tint,
            start = center,
            end = Offset(w * 0.72f, center.y),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun SettingsVectorIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = 1.8.dp.toPx()

        // Sliders/Controls icon: 3 horizontal rails with adjustment pins
        // Rail 1
        drawLine(
            color = tint,
            start = Offset(w * 0.15f, h * 0.26f),
            end = Offset(w * 0.85f, h * 0.26f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = tint,
            radius = stroke * 1.6f,
            center = Offset(w * 0.38f, h * 0.26f)
        )

        // Rail 2
        drawLine(
            color = tint,
            start = Offset(w * 0.15f, h * 0.5f),
            end = Offset(w * 0.85f, h * 0.5f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = tint,
            radius = stroke * 1.6f,
            center = Offset(w * 0.65f, h * 0.5f)
        )

        // Rail 3
        drawLine(
            color = tint,
            start = Offset(w * 0.15f, h * 0.74f),
            end = Offset(w * 0.85f, h * 0.74f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = tint,
            radius = stroke * 1.6f,
            center = Offset(w * 0.3f, h * 0.74f)
        )
    }
}

@Composable
fun BackVectorIcon(
    isRtl: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = 2.2.dp.toPx()

        val path = Path()
        if (isRtl) {
            // Pointing Right (Backwards in RTL)
            drawLine(
                color = tint,
                start = Offset(w * 0.28f, h * 0.5f),
                end = Offset(w * 0.72f, h * 0.5f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            path.moveTo(w * 0.52f, h * 0.28f)
            path.lineTo(w * 0.72f, h * 0.5f)
            path.lineTo(w * 0.52f, h * 0.72f)
        } else {
            // Pointing Left (Backwards in LTR)
            drawLine(
                color = tint,
                start = Offset(w * 0.72f, h * 0.5f),
                end = Offset(w * 0.28f, h * 0.5f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            path.moveTo(w * 0.48f, h * 0.28f)
            path.lineTo(w * 0.28f, h * 0.5f)
            path.lineTo(w * 0.48f, h * 0.72f)
        }

        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun FileDocumentVectorIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = 1.8.dp.toPx()

        // Document body with folded top-right corner
        val fold = w * 0.28f
        val path = Path().apply {
            moveTo(w * 0.22f, h * 0.12f)
            lineTo(w * 0.78f - fold, h * 0.12f)
            lineTo(w * 0.78f, h * 0.12f + fold)
            lineTo(w * 0.78f, h * 0.88f)
            lineTo(w * 0.22f, h * 0.88f)
            close()
        }

        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Corner fold triangle
        val foldPath = Path().apply {
            moveTo(w * 0.78f - fold, h * 0.12f)
            lineTo(w * 0.78f - fold, h * 0.12f + fold)
            lineTo(w * 0.78f, h * 0.12f + fold)
        }
        drawPath(
            path = foldPath,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Document content lines
        drawLine(
            color = tint,
            start = Offset(w * 0.34f, h * 0.44f),
            end = Offset(w * 0.66f, h * 0.44f),
            strokeWidth = stroke * 0.85f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.34f, h * 0.58f),
            end = Offset(w * 0.66f, h * 0.58f),
            strokeWidth = stroke * 0.85f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.34f, h * 0.72f),
            end = Offset(w * 0.52f, h * 0.72f),
            strokeWidth = stroke * 0.85f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun UploadCloudVectorIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 28.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = 2.0.dp.toPx()

        // Upload Arrow
        val arrowHead = Path().apply {
            moveTo(w * 0.34f, h * 0.42f)
            lineTo(w * 0.5f, h * 0.25f)
            lineTo(w * 0.66f, h * 0.42f)
        }
        drawPath(
            path = arrowHead,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.5f, h * 0.25f),
            end = Offset(w * 0.5f, h * 0.65f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )

        // Tray / Base line
        val trayPath = Path().apply {
            moveTo(w * 0.2f, h * 0.62f)
            lineTo(w * 0.2f, h * 0.78f)
            lineTo(w * 0.8f, h * 0.78f)
            lineTo(w * 0.8f, h * 0.62f)
        }
        drawPath(
            path = trayPath,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun ClipboardVectorIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 16.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = 1.6.dp.toPx()

        // Board outline
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.18f, h * 0.22f),
            size = Size(w * 0.64f, h * 0.68f),
            cornerRadius = CornerRadius(2.dp.toPx()),
            style = Stroke(width = stroke)
        )

        // Clip on top
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.35f, h * 0.12f),
            size = Size(w * 0.3f, h * 0.18f),
            cornerRadius = CornerRadius(1.5.dp.toPx()),
            style = Stroke(width = stroke)
        )
    }
}

@Composable
fun EnginesCpuVectorIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = 1.8.dp.toPx()

        // Central processor square
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.28f, h * 0.28f),
            size = Size(w * 0.44f, h * 0.44f),
            cornerRadius = CornerRadius(2.dp.toPx()),
            style = Stroke(width = stroke)
        )

        // Inner core
        drawRect(
            color = tint,
            topLeft = Offset(w * 0.4f, h * 0.4f),
            size = Size(w * 0.2f, h * 0.2f)
        )

        // Pins (Top, Bottom, Left, Right)
        val pinLength = w * 0.14f
        // Top pins
        drawLine(tint, Offset(w * 0.38f, h * 0.28f), Offset(w * 0.38f, h * 0.28f - pinLength), stroke, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.62f, h * 0.28f), Offset(w * 0.62f, h * 0.28f - pinLength), stroke, StrokeCap.Round)
        // Bottom pins
        drawLine(tint, Offset(w * 0.38f, h * 0.72f), Offset(w * 0.38f, h * 0.72f + pinLength), stroke, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.62f, h * 0.72f), Offset(w * 0.62f, h * 0.72f + pinLength), stroke, StrokeCap.Round)
        // Left pins
        drawLine(tint, Offset(w * 0.28f, h * 0.38f), Offset(w * 0.28f - pinLength, h * 0.38f), stroke, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.28f, h * 0.62f), Offset(w * 0.28f - pinLength, h * 0.62f), stroke, StrokeCap.Round)
        // Right pins
        drawLine(tint, Offset(w * 0.72f, h * 0.38f), Offset(w * 0.72f + pinLength, h * 0.38f), stroke, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.72f, h * 0.62f), Offset(w * 0.72f + pinLength, h * 0.62f), stroke, StrokeCap.Round)
    }
}

@Composable
fun CloseCrossVectorIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 18.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = 2.0.dp.toPx()

        drawLine(
            color = tint,
            start = Offset(w * 0.24f, h * 0.24f),
            end = Offset(w * 0.76f, h * 0.76f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.76f, h * 0.24f),
            end = Offset(w * 0.24f, h * 0.76f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun ShieldVerdictVectorIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 22.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = 1.8.dp.toPx()

        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.12f)
            lineTo(w * 0.85f, h * 0.26f)
            lineTo(w * 0.85f, h * 0.58f)
            cubicTo(
                w * 0.85f, h * 0.78f,
                w * 0.65f, h * 0.90f,
                w * 0.5f, h * 0.94f
            )
            cubicTo(
                w * 0.35f, h * 0.90f,
                w * 0.15f, h * 0.78f,
                w * 0.15f, h * 0.58f
            )
            lineTo(w * 0.15f, h * 0.26f)
            close()
        }

        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}



@Composable
fun CameraVectorIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 22.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = 1.8.dp.toPx()

        val bodyPath = Path().apply {
            moveTo(w * 0.12f, h * 0.32f)
            lineTo(w * 0.32f, h * 0.32f)
            lineTo(w * 0.38f, h * 0.20f)
            lineTo(w * 0.62f, h * 0.20f)
            lineTo(w * 0.68f, h * 0.32f)
            lineTo(w * 0.88f, h * 0.32f)
            cubicTo(w * 0.94f, h * 0.32f, w * 0.94f, h * 0.38f, w * 0.94f, h * 0.44f)
            lineTo(w * 0.94f, h * 0.80f)
            cubicTo(w * 0.94f, h * 0.86f, w * 0.88f, h * 0.86f, w * 0.88f, h * 0.86f)
            lineTo(w * 0.12f, h * 0.86f)
            cubicTo(w * 0.06f, h * 0.86f, w * 0.06f, h * 0.80f, w * 0.06f, h * 0.80f)
            lineTo(w * 0.06f, h * 0.44f)
            cubicTo(w * 0.06f, h * 0.38f, w * 0.12f, h * 0.32f, w * 0.12f, h * 0.32f)
            close()
        }

        drawPath(
            path = bodyPath,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        drawCircle(
            color = tint,
            radius = w * 0.19f,
            center = Offset(w * 0.5f, h * 0.57f),
            style = Stroke(width = stroke)
        )

        drawCircle(
            color = tint,
            radius = stroke * 0.6f,
            center = Offset(w * 0.80f, h * 0.42f)
        )
    }
}

@Composable
fun QrCodeVectorIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 22.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = 1.6.dp.toPx()

        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.12f, h * 0.12f),
            size = Size(w * 0.30f, h * 0.30f),
            cornerRadius = CornerRadius(2.dp.toPx()),
            style = Stroke(width = stroke)
        )
        drawRect(
            color = tint,
            topLeft = Offset(w * 0.20f, h * 0.20f),
            size = Size(w * 0.14f, h * 0.14f)
        )

        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.58f, h * 0.12f),
            size = Size(w * 0.30f, h * 0.30f),
            cornerRadius = CornerRadius(2.dp.toPx()),
            style = Stroke(width = stroke)
        )
        drawRect(
            color = tint,
            topLeft = Offset(w * 0.66f, h * 0.20f),
            size = Size(w * 0.14f, h * 0.14f)
        )

        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.12f, h * 0.58f),
            size = Size(w * 0.30f, h * 0.30f),
            cornerRadius = CornerRadius(2.dp.toPx()),
            style = Stroke(width = stroke)
        )
        drawRect(
            color = tint,
            topLeft = Offset(w * 0.20f, h * 0.66f),
            size = Size(w * 0.14f, h * 0.14f)
        )

        drawRect(
            color = tint,
            topLeft = Offset(w * 0.58f, h * 0.58f),
            size = Size(w * 0.12f, h * 0.12f)
        )
        drawRect(
            color = tint,
            topLeft = Offset(w * 0.76f, h * 0.58f),
            size = Size(w * 0.12f, h * 0.12f)
        )
        drawRect(
            color = tint,
            topLeft = Offset(w * 0.58f, h * 0.76f),
            size = Size(w * 0.12f, h * 0.12f)
        )
        drawRect(
            color = tint,
            topLeft = Offset(w * 0.76f, h * 0.76f),
            size = Size(w * 0.12f, h * 0.12f)
        )
    }
}

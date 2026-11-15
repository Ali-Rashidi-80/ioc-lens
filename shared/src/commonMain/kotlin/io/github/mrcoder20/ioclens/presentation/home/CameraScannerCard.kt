package io.github.mrcoder20.ioclens.presentation.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mrcoder20.ioclens.core.components.CameraController
import io.github.mrcoder20.ioclens.core.components.CameraVectorIcon
import io.github.mrcoder20.ioclens.core.components.QrCodeVectorIcon
import io.github.mrcoder20.ioclens.core.components.springPress
import io.github.mrcoder20.ioclens.core.localization.AppLanguage
import io.github.mrcoder20.ioclens.core.theme.CardDark
import io.github.mrcoder20.ioclens.core.theme.PrimaryCyan
import io.github.mrcoder20.ioclens.core.theme.PrimaryNeon
import io.github.mrcoder20.ioclens.core.theme.topBorderHighlightBrush

@Composable
fun CameraScannerCard(
    cameraController: CameraController,
    appLanguage: AppLanguage,
    onLaunchCamera: () -> Unit = { cameraController.launchCamera() },
    onLaunchScreenSnip: () -> Unit = { cameraController.launchScreenSnip() },
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    val isRtl = appLanguage.isRtl
    var isCardHovered by remember { mutableStateOf(false) }
    val viewfinderInteractionSource = remember { MutableInteractionSource() }
    val isViewfinderHovered by viewfinderInteractionSource.collectIsHoveredAsState()
    val isCameraReady = cameraController.isCameraSupported

    val infiniteTransition = rememberInfiniteTransition(label = "CameraCardPulse")
    val laserFraction by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "miniLaserSweep"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        isCardHovered = event.type != PointerEventType.Exit
                    }
                }
            },
        color = CardDark.copy(alpha = if (isCardHovered) 0.95f else 0.85f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (isCardHovered) Brush.linearGradient(listOf(PrimaryCyan, PrimaryCyan.copy(alpha = 0.45f)))
            else topBorderHighlightBrush(PrimaryNeon, isDark = true)
        ),
        shadowElevation = if (isCardHovered) 8.dp else 4.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp)
        ) {
            // Header: Icon + Title + Device Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PrimaryCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CameraVectorIcon(tint = PrimaryCyan, size = 20.dp)
                    }
                    Column {
                        Text(
                            text = when (appLanguage) {
                                AppLanguage.PERSIAN -> "پویشگر دوربین و کدهای تصویری"
                                AppLanguage.RUSSIAN -> "Камера и распознавание QR/IOC"
                                else -> "Camera & QR/IOC Vision"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = when (appLanguage) {
                                AppLanguage.PERSIAN -> "اسکن کدهای QR، بارکد و پی‌لود با پشتیبانی همه دیوایس‌ها"
                                AppLanguage.RUSSIAN -> "Сканирование QR, штрих-кодов и ссылок на всех устройствах"
                                else -> "Live QR, barcode & visual IOC triage • Cross-device support"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                // Device Status Pill
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isCameraReady) Color(0xFF10B981).copy(alpha = 0.12f) else Color(0xFFF59E0B).copy(alpha = 0.12f),
                    border = BorderStroke(0.6.dp, if (isCameraReady) Color(0xFF10B981).copy(alpha = 0.35f) else Color(0xFFF59E0B).copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isCameraReady) Color(0xFF10B981) else Color(0xFFF59E0B))
                        )
                        Text(
                            text = if (isCameraReady) (if (isRtl) "آماده" else "Ready") else (if (isRtl) "فاقد وب‌کم" else "No Camera"),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCameraReady) Color(0xFF10B981) else Color(0xFFF59E0B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Viewfinder Card (Click directly launches native camera)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isCompact) Modifier.heightIn(min = 120.dp)
                        else Modifier.defaultMinSize(minHeight = 115.dp)
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .springPress(interactionSource = viewfinderInteractionSource)
                    .clickable(
                        interactionSource = viewfinderInteractionSource,
                        indication = null,
                        onClick = onLaunchCamera
                    )
                    .pointerHoverIcon(PointerIcon.Hand),
                shape = RoundedCornerShape(12.dp),
                color = if (isViewfinderHovered) PrimaryCyan.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                border = BorderStroke(1.2.dp, if (isViewfinderHovered) PrimaryCyan else PrimaryCyan.copy(alpha = 0.35f))
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Reticle brackets background
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val stroke = 2.dp.toPx()
                        val corner = 18.dp.toPx()
                        val color = PrimaryCyan.copy(alpha = if (isViewfinderHovered) 0.9f else 0.5f)

                        // Top-Left
                        drawLine(color, Offset(0f, 0f), Offset(corner, 0f), stroke, StrokeCap.Round)
                        drawLine(color, Offset(0f, 0f), Offset(0f, corner), stroke, StrokeCap.Round)
                        // Top-Right
                        drawLine(color, Offset(w, 0f), Offset(w - corner, 0f), stroke, StrokeCap.Round)
                        drawLine(color, Offset(w, 0f), Offset(w, corner), stroke, StrokeCap.Round)
                        // Bottom-Left
                        drawLine(color, Offset(0f, h), Offset(corner, h), stroke, StrokeCap.Round)
                        drawLine(color, Offset(0f, h), Offset(0f, h - corner), stroke, StrokeCap.Round)
                        // Bottom-Right
                        drawLine(color, Offset(w, h), Offset(w - corner, h), stroke, StrokeCap.Round)
                        drawLine(color, Offset(w, h), Offset(w, h - corner), stroke, StrokeCap.Round)

                        // Laser sweep line
                        val y = h * laserFraction
                        drawLine(
                            brush = Brush.horizontalGradient(
                                listOf(Color.Transparent, PrimaryCyan, Color(0xFF00FFCC), PrimaryCyan, Color.Transparent)
                            ),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PrimaryCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            QrCodeVectorIcon(tint = PrimaryCyan, size = 20.dp)
                        }

                        Text(
                            text = when (appLanguage) {
                                AppLanguage.PERSIAN -> "بازکردن دوربین نیتیو دستگاه"
                                AppLanguage.RUSSIAN -> "Открыть камеру устройства"
                                else -> "Launch Device Camera"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryCyan,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = when (appLanguage) {
                                AppLanguage.PERSIAN -> "کلیک جهت باز کردن مستقیم دوربین سیستم یا وب‌کم بدون هیچ واسطه"
                                AppLanguage.RUSSIAN -> "Прямой запуск камеры системы без промежуточных окон"
                                else -> "Directly triggers native OS camera or webcam"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Quick Direct Actions (Launch Camera & Screen Snip)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(onClick = onLaunchCamera)
                                    .pointerHoverIcon(PointerIcon.Hand),
                                color = PrimaryCyan.copy(alpha = 0.18f),
                                border = BorderStroke(0.8.dp, PrimaryCyan.copy(alpha = 0.45f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (isRtl) "📷 بازکردن دوربین" else "📷 Open Camera",
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryCyan
                                )
                            }

                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(onClick = onLaunchScreenSnip)
                                    .pointerHoverIcon(PointerIcon.Hand),
                                color = Color.White.copy(alpha = 0.08f),
                                border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.20f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (isRtl) "✂️ اسکرین‌شات / برش" else "✂️ Screen Snip",
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

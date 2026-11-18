package io.github.mrcoder20.ioclens.presentation.vision

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.mrcoder20.ioclens.core.components.CameraLensVectorIcon
import io.github.mrcoder20.ioclens.core.components.rememberClipboardActions
import io.github.mrcoder20.ioclens.core.localization.AppStrings
import io.github.mrcoder20.ioclens.core.theme.LocalIocColors
import io.github.mrcoder20.ioclens.core.theme.rememberMonospaceFontFamily
import io.github.mrcoder20.ioclens.domain.vision.BarcodePayload
import io.github.mrcoder20.ioclens.domain.vision.QuishingAnalyzer
import io.github.mrcoder20.ioclens.domain.vision.QuishingRisk

@Composable
fun CyberLensDialog(
    isOpen: Boolean,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onTriagePayload: (BarcodePayload) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    val palette = LocalIocColors.current
    val clipboardActions = rememberClipboardActions()
    var activePayload by remember { mutableStateOf<BarcodePayload?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (palette.isDark) 0.72f else 0.45f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val screenWidth = maxWidth
            val isCompact = screenWidth < 360.dp
            val isTabletOrDesktop = screenWidth >= 640.dp

            val dialogWidth = if (isTabletOrDesktop) 620.dp else screenWidth.coerceAtMost(460.dp)

            Surface(
                modifier = Modifier
                    .width(dialogWidth)
                    .clipToBounds(),
                shape = RoundedCornerShape(20.dp),
                color = palette.surfaceCard,
                border = BorderStroke(1.dp, Brush.linearGradient(listOf(palette.accentPrimary.copy(alpha = 0.6f), palette.borderStrong)))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CameraLensVectorIcon(tint = palette.accentPrimary, size = 20.dp)
                            Text(
                                text = strings.visionTitle,
                                color = palette.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Close button
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = palette.surfaceInset,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "✕",
                                        color = palette.textPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val currentPayload = activePayload
                    if (currentPayload == null) {
                        // Scanner Viewport with glowing reticle and laser sweep
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (isCompact) 330.dp else 360.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, palette.borderStrong, RoundedCornerShape(16.dp))
                                .clipToBounds()
                        ) {
                            PlatformCameraScanner(
                                onPayloadsDetected = { payloads ->
                                    if (payloads.isNotEmpty()) {
                                        activePayload = payloads.first()
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            // Cyber Reticle Overlay
                            ViewfinderReticleOverlay(
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        // Quishing Triage Card
                        QuishingTriageCard(
                            payload = currentPayload,
                            strings = strings,
                            isCompact = isCompact,
                            onTriage = {
                                onTriagePayload(currentPayload)
                                onDismiss()
                            },
                            onCopy = {
                                clipboardActions.copyText(currentPayload.defangedText)
                            },
                            onScanAgain = {
                                activePayload = null
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewfinderReticleOverlay(modifier: Modifier = Modifier) {
    val palette = LocalIocColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserY"
    )

    Canvas(modifier = modifier.clipToBounds()) {
        val w = size.width
        val h = size.height
        val stroke = 3.dp.toPx()
        val cornerLen = 24.dp.toPx()

        drawLine(palette.accentPrimary, Offset(16f, 16f), Offset(16f + cornerLen, 16f), stroke, StrokeCap.Round)
        drawLine(palette.accentPrimary, Offset(16f, 16f), Offset(16f, 16f + cornerLen), stroke, StrokeCap.Round)

        drawLine(palette.accentPrimary, Offset(w - 16f, 16f), Offset(w - 16f - cornerLen, 16f), stroke, StrokeCap.Round)
        drawLine(palette.accentPrimary, Offset(w - 16f, 16f), Offset(w - 16f, 16f + cornerLen), stroke, StrokeCap.Round)

        drawLine(palette.accentPrimary, Offset(16f, h - 16f), Offset(16f + cornerLen, h - 16f), stroke, StrokeCap.Round)
        drawLine(palette.accentPrimary, Offset(16f, h - 16f), Offset(16f, h - 16f - cornerLen), stroke, StrokeCap.Round)

        drawLine(palette.accentPrimary, Offset(w - 16f, h - 16f), Offset(w - 16f - cornerLen, h - 16f), stroke, StrokeCap.Round)
        drawLine(palette.accentPrimary, Offset(w - 16f, h - 16f), Offset(w - 16f, h - 16f - cornerLen), stroke, StrokeCap.Round)

        val currentLaserY = h * laserY
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    palette.accentPrimary.copy(alpha = 0.85f),
                    palette.accentSecondary,
                    palette.accentPrimary.copy(alpha = 0.85f),
                    Color.Transparent
                )
            ),
            start = Offset(20f, currentLaserY),
            end = Offset(w - 20f, currentLaserY),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun QuishingTriageCard(
    payload: BarcodePayload,
    strings: AppStrings,
    isCompact: Boolean,
    onTriage: () -> Unit,
    onCopy: () -> Unit,
    onScanAgain: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalIocColors.current
    val monoFont = rememberMonospaceFontFamily()
    var copied by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Risk Badge + Target Type
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val (badgeBg, badgeBorder, badgeText, badgeColor) = when (payload.quishingRisk) {
                QuishingRisk.HIGH_RISK -> Quad(palette.verdictMalicious.copy(alpha = 0.15f), palette.verdictMalicious, strings.visionHighRisk, palette.verdictMalicious)
                QuishingRisk.SUSPICIOUS -> Quad(palette.verdictSuspicious.copy(alpha = 0.15f), palette.verdictSuspicious, strings.visionSuspicious, palette.verdictSuspicious)
                else -> Quad(palette.verdictClean.copy(alpha = 0.15f), palette.verdictClean, strings.visionSafe, palette.verdictClean)
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = badgeBg,
                border = BorderStroke(1.dp, badgeBorder.copy(alpha = 0.5f))
            ) {
                Text(
                    text = badgeText,
                    color = badgeColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Text(
                text = "${payload.type.name} • ${payload.detectedIocType ?: "TEXT"}",
                color = palette.textMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Defanged Text Preview Box
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = palette.surfaceInset,
            border = BorderStroke(1.dp, palette.borderSubtle)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = strings.visionDefangedLabel,
                    color = palette.textMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(
                        text = payload.defangedText,
                        color = palette.textPrimary,
                        fontSize = 12.sp,
                        fontFamily = monoFont,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // If unmasked or risk reason exists
        if (payload.riskReason != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = palette.verdictSuspicious.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, palette.verdictSuspicious.copy(alpha = 0.45f))
            ) {
                Text(
                    text = "⚠️ ${payload.riskReason}",
                    color = palette.textPrimary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        if (payload.unmaskedTargetUrl != null) {
            val safeDefangedUnmasked = remember(payload.unmaskedTargetUrl) {
                QuishingAnalyzer.defang(payload.unmaskedTargetUrl)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = palette.accentPrimary.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, palette.accentPrimary.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(strings.visionUnmaskedLabel, color = palette.accentPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(safeDefangedUnmasked, color = palette.textPrimary, fontSize = 11.sp, fontFamily = monoFont)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // CTAs
        Button(
            onClick = onTriage,
            colors = ButtonDefaults.buttonColors(containerColor = palette.accentPrimary),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(40.dp)
        ) {
            Text(strings.visionTriageCta, color = palette.onAccentPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isCompact) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onCopy()
                        copied = true
                    },
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, palette.borderStrong)
                ) {
                    Text(if (copied) strings.copiedFeedback else strings.copyDefangedIoc, color = palette.textPrimary, fontSize = 11.sp)
                }
                TextButton(
                    onClick = onScanAgain,
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                ) {
                    Text(strings.visionScanAgain, color = palette.textSecondary, fontSize = 11.sp)
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onCopy()
                        copied = true
                    },
                    modifier = Modifier.weight(1f).height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, palette.borderStrong)
                ) {
                    Text(if (copied) strings.copiedFeedback else strings.copyDefangedIoc, color = palette.textPrimary, fontSize = 11.sp)
                }
                TextButton(
                    onClick = onScanAgain,
                    modifier = Modifier.weight(1f).height(36.dp)
                ) {
                    Text(strings.visionScanAgain, color = palette.textSecondary, fontSize = 11.sp)
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

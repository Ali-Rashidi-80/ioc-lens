package io.github.mrcoder20.ioclens.presentation.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.mrcoder20.ioclens.core.components.CameraController
import io.github.mrcoder20.ioclens.core.components.CameraVectorIcon
import io.github.mrcoder20.ioclens.core.components.QrCodeVectorIcon
import io.github.mrcoder20.ioclens.core.components.springPress
import io.github.mrcoder20.ioclens.core.localization.AppLanguage
import io.github.mrcoder20.ioclens.core.theme.CardDark
import io.github.mrcoder20.ioclens.core.theme.PrimaryCyan
import io.github.mrcoder20.ioclens.core.theme.PrimaryNeon
import io.github.mrcoder20.ioclens.core.theme.topBorderHighlightBrush
import io.github.mrcoder20.ioclens.domain.parser.IOCParser

@Composable
fun CameraScannerDialog(
    cameraController: CameraController,
    appLanguage: AppLanguage,
    onPayloadDetected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val isRtl = appLanguage.isRtl
    var detectedText by remember { mutableStateOf<String?>(null) }
    val iocParser = remember { IOCParser() }
    val scrollState = rememberScrollState()

    val infiniteTransition = rememberInfiniteTransition(label = "LaserScan")
    val laserFraction by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserSweep"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 680.dp)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(20.dp)),
            color = Color(0xFA091020),
            border = BorderStroke(1.2.dp, topBorderHighlightBrush(PrimaryNeon, isDark = true)),
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header: Title on Left, Top-Right 'X' Close Button on Physical Right
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CameraVectorIcon(tint = PrimaryCyan, size = 22.dp)
                            Text(
                                text = when (appLanguage) {
                                    AppLanguage.PERSIAN -> "اسکنر دوربین و کدهای QR"
                                    AppLanguage.RUSSIAN -> "Сканер камеры и QR-кодов"
                                    else -> "Camera & QR/IOC Vision"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryCyan
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = PrimaryCyan.copy(alpha = 0.12f),
                                border = BorderStroke(0.6.dp, PrimaryCyan.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = if (isRtl) "بلادرنگ • همه دیوایس‌ها" else "Live • Cross-Device",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PrimaryCyan
                                )
                            }
                        }

                        // Physical Top-Right 'X' Close Button (Apple HIG Style)
                        Surface(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .springPress()
                                .clickable(onClick = onDismiss)
                                .pointerHoverIcon(PointerIcon.Hand),
                            color = Color.White.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                            shape = CircleShape
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "✕",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Cyberpunk Animated Viewfinder
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        color = Color(0xFF040812),
                        border = BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.45f))
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Viewfinder Reticle Brackets & Laser Scan Beam
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                val stroke = 2.5.dp.toPx()
                                val cornerLen = 28.dp.toPx()
                                val reticleColor = PrimaryCyan

                                // Top-Left bracket
                                drawLine(reticleColor, Offset(24f, 24f), Offset(24f + cornerLen, 24f), stroke, StrokeCap.Round)
                                drawLine(reticleColor, Offset(24f, 24f), Offset(24f, 24f + cornerLen), stroke, StrokeCap.Round)

                                // Top-Right bracket
                                drawLine(reticleColor, Offset(w - 24f, 24f), Offset(w - 24f - cornerLen, 24f), stroke, StrokeCap.Round)
                                drawLine(reticleColor, Offset(w - 24f, 24f), Offset(w - 24f, 24f + cornerLen), stroke, StrokeCap.Round)

                                // Bottom-Left bracket
                                drawLine(reticleColor, Offset(24f, h - 24f), Offset(24f + cornerLen, h - 24f), stroke, StrokeCap.Round)
                                drawLine(reticleColor, Offset(24f, h - 24f), Offset(24f, h - 24f - cornerLen), stroke, StrokeCap.Round)

                                // Bottom-Right bracket
                                drawLine(reticleColor, Offset(w - 24f, h - 24f), Offset(w - 24f - cornerLen, h - 24f), stroke, StrokeCap.Round)
                                drawLine(reticleColor, Offset(w - 24f, h - 24f), Offset(w - 24f, h - 24f - cornerLen), stroke, StrokeCap.Round)

                                // Sweeping Laser Line
                                val laserY = h * laserFraction
                                drawLine(
                                    brush = Brush.horizontalGradient(
                                        listOf(Color.Transparent, PrimaryCyan, Color(0xFF00FFCC), PrimaryCyan, Color.Transparent)
                                    ),
                                    start = Offset(24f, laserY),
                                    end = Offset(w - 24f, laserY),
                                    strokeWidth = 2.dp.toPx()
                                )
                            }

                            // Viewfinder Center Overlay Info
                            Column(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                QrCodeVectorIcon(tint = PrimaryCyan.copy(alpha = 0.85f), size = 44.dp)
                                Text(
                                    text = when (appLanguage) {
                                        AppLanguage.PERSIAN -> "دوربین را روبروی کد QR، بارکد یا شاخص امنیتی قرار دهید"
                                        AppLanguage.RUSSIAN -> "Наведите камеру на QR-код или индикатор угрозы"
                                        else -> "Point camera at QR code, link or threat payload"
                                    },
                                    fontSize = 12.sp,
                                    color = Color(0xFFCBD5E1),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Device Engine: ${cameraController.platformName}",
                                    fontSize = 10.sp,
                                    color = PrimaryCyan.copy(alpha = 0.7f),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // 2. Hardware Trigger Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { cameraController.launchCamera() },
                            modifier = Modifier.weight(1f).height(42.dp).springPress(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryCyan.copy(alpha = 0.15f),
                                contentColor = PrimaryCyan
                            ),
                            border = BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.45f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CameraVectorIcon(tint = PrimaryCyan, size = 18.dp)
                                Text(
                                    text = if (isRtl) "دوربین دیوایس" else "Device Camera",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Button(
                            onClick = { cameraController.launchScreenSnip() },
                            modifier = Modifier.weight(1f).height(42.dp).springPress(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.08f),
                                contentColor = Color.White
                            ),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = "✂️", fontSize = 14.sp)
                                Text(
                                    text = if (isRtl) "تصویر / اسکرین‌شات" else "Image / Screen Snip",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // 3. Quick Visual Lab Test Samples (Simulated QR / Visual Ingestion)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = CardDark.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(0.8.dp, PrimaryCyan.copy(alpha = 0.25f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (isRtl) "نمونه‌های آماده تست بصری (Lab Samples)" else "Simulated QR / Visual IOC Samples",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryCyan
                            )
                            Text(
                                text = if (isRtl)
                                    "برای تست سریع قابلیت بدون نیاز به اسکن فیزیکی، یکی از نمونه‌های زیر را انتخاب کنید:"
                                else
                                    "Select any sample below to verify visual decode & triage pipeline instantly:",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )

                            val samples = listOf(
                                Triple("WannaCry SHA-256 Hash", "eceb2f25bac4cbca1da5f4e390124912cd91f541ad1ccada2ae2b46f4aceb414", "MALICIOUS HASH"),
                                Triple("URLhaus Malware Dropper", "http://61.92.220.176:37567/bin.sh", "MALICIOUS URL"),
                                Triple("Feodo Botnet C2 IP", "195.123.245.11", "BOTNET IP"),
                                Triple("Clean ICANN Domain", "github.com", "CLEAN DOMAIN")
                            )

                            samples.forEach { (label, value, tag) ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { detectedText = value }
                                        .pointerHoverIcon(PointerIcon.Hand),
                                    color = if (detectedText == value) PrimaryCyan.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f),
                                    border = BorderStroke(
                                        0.8.dp,
                                        if (detectedText == value) PrimaryCyan else Color.White.copy(alpha = 0.10f)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                            Text(text = label, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = Color.White)
                                            Text(text = value, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF94A3B8), maxLines = 1)
                                        }
                                        Surface(
                                            color = PrimaryCyan.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(text = tag, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = PrimaryCyan)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. Decoded Result Banner & Instant Triage Trigger
                    detectedText?.let { payload ->
                        val parsed = iocParser.parse(payload)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF071426),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, PrimaryNeon)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isRtl) "شاخص استخراج شده از دوربین / تصویر:" else "Decoded Visual Indicator:",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryNeon
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = PrimaryNeon.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = parsed.toIndicator().type.name,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryNeon
                                        )
                                    }
                                }

                                Text(
                                    text = payload,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White,
                                    maxLines = 2
                                )

                                Button(
                                    onClick = {
                                        onPayloadDetected(payload)
                                        onDismiss()
                                    },
                                    modifier = Modifier.fillMaxWidth().height(40.dp).springPress(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PrimaryNeon,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (isRtl) "⚡ تحلیل و تریاژ فوری این شاخص" else "⚡ Instant Threat Triage",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

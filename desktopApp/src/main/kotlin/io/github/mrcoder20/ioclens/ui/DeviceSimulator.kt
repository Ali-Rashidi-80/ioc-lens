package io.github.mrcoder20.ioclens.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mrcoder20.ioclens.core.theme.PrimaryCyan
import io.github.mrcoder20.ioclens.core.theme.VerdictClean
import java.awt.BasicStroke
import java.awt.Point
import java.awt.RenderingHints
import java.awt.Toolkit
import java.awt.image.BufferedImage

enum class DeviceCategory {
    RESPONSIVE,
    PHONE,
    TABLET,
    FOLDABLE,
    DESKTOP
}

data class DevicePreset(
    val id: String,
    val name: String,
    val width: Int, // dp
    val height: Int, // dp
    val category: DeviceCategory
) {
    companion object {
        val RESPONSIVE = DevicePreset("responsive", "Responsive", 0, 0, DeviceCategory.RESPONSIVE)

        val ALL_PRESETS = listOf(
            RESPONSIVE,
            // Apple Phones
            DevicePreset("iphone_se", "iPhone SE", 375, 667, DeviceCategory.PHONE),
            DevicePreset("iphone_xr", "iPhone XR", 414, 896, DeviceCategory.PHONE),
            DevicePreset("iphone_12_pro", "iPhone 12 Pro", 390, 844, DeviceCategory.PHONE),
            DevicePreset("iphone_14_pro_max", "iPhone 14 Pro Max", 430, 932, DeviceCategory.PHONE),
            DevicePreset("iphone_15_pro_max", "iPhone 15 Pro Max", 430, 932, DeviceCategory.PHONE),
            DevicePreset("iphone_16_pro_max", "iPhone 16 Pro Max", 440, 956, DeviceCategory.PHONE),

            // Google Pixel Phones
            DevicePreset("pixel_7", "Pixel 7", 412, 915, DeviceCategory.PHONE),
            DevicePreset("pixel_8", "Pixel 8", 412, 915, DeviceCategory.PHONE),
            DevicePreset("pixel_9", "Pixel 9", 412, 923, DeviceCategory.PHONE),
            DevicePreset("pixel_10", "Pixel 10", 412, 930, DeviceCategory.PHONE),

            // Samsung Phones
            DevicePreset("samsung_s8", "Samsung Galaxy S8+", 360, 740, DeviceCategory.PHONE),
            DevicePreset("samsung_s20_ultra", "Samsung Galaxy S20 Ultra", 412, 915, DeviceCategory.PHONE),
            DevicePreset("samsung_a51", "Samsung Galaxy A51/71", 412, 914, DeviceCategory.PHONE),

            // Foldable Devices
            DevicePreset("galaxy_z_fold_5", "Galaxy Z Fold 5", 344, 882, DeviceCategory.FOLDABLE),
            DevicePreset("surface_duo", "Surface Duo", 540, 720, DeviceCategory.FOLDABLE),
            DevicePreset("asus_zenbook_fold", "Asus Zenbook Fold", 853, 1280, DeviceCategory.FOLDABLE),

            // Tablets & Hubs
            DevicePreset("ipad_mini", "iPad Mini", 768, 1024, DeviceCategory.TABLET),
            DevicePreset("ipad_air", "iPad Air", 820, 1180, DeviceCategory.TABLET),
            DevicePreset("ipad_pro", "iPad Pro", 1024, 1366, DeviceCategory.TABLET),
            DevicePreset("surface_pro_7", "Surface Pro 7", 912, 1368, DeviceCategory.TABLET),
            DevicePreset("nest_hub", "Nest Hub", 1024, 600, DeviceCategory.TABLET),
            DevicePreset("nest_hub_max", "Nest Hub Max", 1280, 800, DeviceCategory.TABLET),

            // Desktop & Laptop Viewports
            DevicePreset("laptop_hd", "Laptop (1366×768)", 1366, 768, DeviceCategory.DESKTOP),
            DevicePreset("desktop_fhd", "Desktop (1920×1080)", 1920, 1080, DeviceCategory.DESKTOP),
            DevicePreset("macbook_air", "MacBook Air (1440×900)", 1440, 900, DeviceCategory.DESKTOP)
        )
    }
}

fun getDeviceEmulationPointerIcon(): PointerIcon {
    return try {
        val size = 26
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g2d = image.createGraphics()
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        // DevTools-style semi-transparent touch disk
        g2d.color = java.awt.Color(0, 245, 255, 60)
        g2d.fillOval(2, 2, size - 4, size - 4)

        // High-contrast smooth border ring
        g2d.color = java.awt.Color(0, 245, 255, 230)
        g2d.stroke = BasicStroke(1.5f)
        g2d.drawOval(2, 2, size - 4, size - 4)

        // Center contact pip
        g2d.color = java.awt.Color(0, 245, 255, 255)
        g2d.fillOval(size / 2 - 2, size / 2 - 2, 4, 4)

        g2d.dispose()
        val cursor = Toolkit.getDefaultToolkit().createCustomCursor(
            image,
            Point(size / 2, size / 2),
            "IOCLensTouchEmulationCursor"
        )
        PointerIcon(cursor)
    } catch (_: Throwable) {
        PointerIcon.Hand
    }
}

@Composable
fun DeviceSimulatorToolbar(
    selectedPreset: DevicePreset,
    onSelectPreset: (DevicePreset) -> Unit,
    isLandscape: Boolean,
    onToggleOrientation: () -> Unit,
    showFrame: Boolean,
    onToggleFrame: () -> Unit,
    onClose: () -> Unit,
    onSnapWindow: ((Int, Int) -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(42.dp),
            color = Color(0xFF090E17),
            border = BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Close Button + Preset Selector Dropdown
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Close Device Mode Button (✕)
                    Surface(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .clickable { onClose() }
                            .pointerHoverIcon(PointerIcon.Hand),
                        color = Color(0xFFEF4444).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.45f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("✕", color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(18.dp)
                            .background(Color(0xFF334155))
                    )

                    // Device Category Icon Indicator
                    Text(
                        text = when (selectedPreset.category) {
                            DeviceCategory.TABLET -> "📱"
                            DeviceCategory.FOLDABLE -> "📖"
                            DeviceCategory.RESPONSIVE -> "🖥️"
                            DeviceCategory.DESKTOP -> "💻"
                            else -> "📱"
                        },
                        fontSize = 14.sp
                    )

                    // Preset Dropdown Trigger
                    Box {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { showMenu = true }
                                .pointerHoverIcon(PointerIcon.Hand),
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF131F33),
                            border = BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (selectedPreset.category == DeviceCategory.RESPONSIVE) "Responsive" else selectedPreset.name,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text("▾", color = PrimaryCyan, fontSize = 11.sp)
                            }
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier
                                .heightIn(max = 420.dp)
                                .background(Color(0xFF0C1424))
                                .border(1.dp, PrimaryCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        ) {
                            DevicePreset.ALL_PRESETS.forEach { preset ->
                                val isSelected = preset.id == selectedPreset.id
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = preset.name,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) PrimaryCyan else Color.White,
                                                fontSize = 12.sp
                                            )
                                            if (preset.category != DeviceCategory.RESPONSIVE) {
                                                Spacer(Modifier.width(12.dp))
                                                Text(
                                                    text = "${preset.width} × ${preset.height}",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF64748B)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        onSelectPreset(preset)
                                        showMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Dimension Badge
                    if (selectedPreset.category != DeviceCategory.RESPONSIVE) {
                        val effectiveW = if (isLandscape) selectedPreset.height else selectedPreset.width
                        val effectiveH = if (isLandscape) selectedPreset.width else selectedPreset.height
                        Surface(
                            color = PrimaryCyan.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = "$effectiveW × $effectiveH dp",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = PrimaryCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Right: Controls (Touch Emulation, Rotate, Frame Toggle, Snap Window)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (selectedPreset.category != DeviceCategory.DESKTOP && selectedPreset.category != DeviceCategory.RESPONSIVE) {
                        Surface(
                            color = VerdictClean.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, VerdictClean.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = "● Touch Emulation",
                                color = VerdictClean,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    if (selectedPreset.category != DeviceCategory.RESPONSIVE) {
                        // Rotate Orientation Button
                        Surface(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onToggleOrientation() }
                                .pointerHoverIcon(PointerIcon.Hand),
                            shape = RoundedCornerShape(6.dp),
                            color = if (isLandscape) PrimaryCyan.copy(alpha = 0.2f) else Color(0xFF131F33),
                            border = BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.35f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🔄", fontSize = 12.sp)
                            }
                        }

                        // Bezel Frame Toggle Button
                        Surface(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onToggleFrame() }
                                .pointerHoverIcon(PointerIcon.Hand),
                            shape = RoundedCornerShape(6.dp),
                            color = if (showFrame) PrimaryCyan.copy(alpha = 0.2f) else Color(0xFF131F33),
                            border = BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.35f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(if (showFrame) "📱" else "🔲", fontSize = 12.sp)
                            }
                        }

                        // Snap Window Button
                        if (onSnapWindow != null) {
                            val effectiveW = if (isLandscape) selectedPreset.height else selectedPreset.width
                            val effectiveH = if (isLandscape) selectedPreset.width else selectedPreset.height
                            Surface(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onSnapWindow(effectiveW, effectiveH) }
                                    .pointerHoverIcon(PointerIcon.Hand),
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF131F33),
                                border = BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.35f))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("⛶", color = PrimaryCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceChassis(
    preset: DevicePreset,
    isLandscape: Boolean,
    showFrame: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (preset.category == DeviceCategory.RESPONSIVE) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .pointerHoverIcon(PointerIcon.Default)
        ) {
            content()
        }
        return
    }

    val targetW = if (isLandscape) preset.height.dp else preset.width.dp
    val targetH = if (isLandscape) preset.width.dp else preset.height.dp
    val cornerRadius = when (preset.category) {
        DeviceCategory.TABLET -> 24.dp
        DeviceCategory.FOLDABLE -> 20.dp
        DeviceCategory.DESKTOP -> 12.dp
        else -> 36.dp
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF030712)),
        contentAlignment = Alignment.Center
    ) {
        val availableW = maxWidth
        val availableH = maxHeight
        val margin = if (showFrame) 36.dp else 16.dp
        val maxAllowedW = (availableW - margin).coerceAtLeast(100.dp)
        val maxAllowedH = (availableH - margin).coerceAtLeast(100.dp)

        val computedScale = if (targetW > maxAllowedW || targetH > maxAllowedH) {
            val scaleW = maxAllowedW / targetW
            val scaleH = maxAllowedH / targetH
            minOf(scaleW, scaleH).coerceIn(0.2f, 1f)
        } else {
            1f
        }

        Surface(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = computedScale
                    scaleY = computedScale
                    transformOrigin = TransformOrigin.Center
                }
                .width(targetW)
                .height(targetH)
                .shadow(16.dp, RoundedCornerShape(if (showFrame) cornerRadius else 0.dp))
                .clip(RoundedCornerShape(if (showFrame) cornerRadius else 0.dp))
                .border(
                    width = if (showFrame) 4.dp else 1.dp,
                    color = if (showFrame) Color(0xFF1E293B) else PrimaryCyan.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(if (showFrame) cornerRadius else 0.dp)
                ),
            color = Color(0xFF0A0E17)
        ) {
            val isTouchEmulated = preset.category != DeviceCategory.DESKTOP
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (isTouchEmulated) Modifier.pointerHoverIcon(getDeviceEmulationPointerIcon()) else Modifier)
            ) {
                // Application Content
                content()

                // Realistic Simulated Bezel Details (Dynamic Island / Speaker Pill & Home Indicator)
                if (showFrame && preset.category == DeviceCategory.PHONE && !isLandscape) {
                    // Top Dynamic Island Pill
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                            .width(88.dp)
                            .height(20.dp)
                            .background(Color.Black, CircleShape)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Box(modifier = Modifier.size(8.dp).background(Color(0xFF1F2937), CircleShape))
                    }

                    // Bottom Home Indicator Line
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 6.dp)
                            .width(110.dp)
                            .height(4.dp)
                            .background(Color.White.copy(alpha = 0.35f), CircleShape)
                    )
                }
            }
        }
    }
}

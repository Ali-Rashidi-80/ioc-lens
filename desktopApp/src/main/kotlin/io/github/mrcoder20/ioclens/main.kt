package io.github.mrcoder20.ioclens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.mrcoder20.ioclens.core.theme.IOCLensTheme
import io.github.mrcoder20.ioclens.core.theme.PrimaryCyan
import io.github.mrcoder20.ioclens.domain.security.SharePayloadReceiver
import io.github.mrcoder20.ioclens.domain.security.SharedPayload
import io.github.mrcoder20.ioclens.ui.DeviceChassis
import io.github.mrcoder20.ioclens.ui.DevicePreset
import io.github.mrcoder20.ioclens.ui.DeviceSimulatorToolbar
import java.awt.Cursor
import java.awt.Dimension
import java.awt.EventQueue
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import java.io.File

fun main(args: Array<String>) {
    // High-performance desktop networking and encoding optimizations
    System.setProperty("java.net.preferIPv6Addresses", "false")
    System.setProperty("sun.net.useExclusiveBind", "false")
    System.setProperty("file.encoding", "UTF-8")

    application {
        remember {
            val arg = args.firstOrNull()?.trim()
            if (!arg.isNullOrBlank()) {
                val file = File(arg)
                if (file.exists() && file.isFile) {
                    SharePayloadReceiver.dispatch(SharedPayload(filePath = file.absolutePath, fileName = file.name))
                } else {
                    SharePayloadReceiver.dispatch(SharedPayload(textOrUrl = arg))
                }
            }
        }
        val windowState = rememberWindowState(
            placement = WindowPlacement.Floating,
            position = WindowPosition.Aligned(Alignment.Center),
            size = DpSize(1040.dp, 740.dp)
        )

        val iconPainter = remember {
            runCatching {
                val relative1 = File("docs/assets/logo/icon-512.png")
                val relative2 = File("../docs/assets/logo/icon-512.png")
                when {
                    relative1.exists() -> relative1
                    relative2.exists() -> relative2
                    else -> null
                }
            }.getOrNull()?.let { file ->
                runCatching {
                    file.inputStream().use { stream ->
                        BitmapPainter(stream.readAllBytes().decodeToImageBitmap())
                    }
                }.getOrNull()
            }
        }

        Window(
            onCloseRequest = ::exitApplication,
            title = "IOC Lens - Copy it. Know in a second.",
            state = windowState,
            icon = iconPainter,
            undecorated = true
        ) {
            var isMaximized by remember { mutableStateOf(false) }
            var floatingBounds by remember { mutableStateOf<Rectangle?>(null) }
            var deviceModeActive by remember { mutableStateOf(false) }
            var selectedDevicePreset by remember { mutableStateOf(DevicePreset.RESPONSIVE) }
            var isLandscapeOrientation by remember { mutableStateOf(false) }
            var showDeviceFrameBezel by remember { mutableStateOf(true) }

            // Ensure system cursor is always valid, visible, and window enforces minimum size
            DisposableEffect(window) {
                fun ensureCursor() {
                    EventQueue.invokeLater {
                        window.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
                        window.rootPane.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
                        window.contentPane.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
                    }
                }
                ensureCursor()

                val focusListener = object : WindowFocusListener {
                    override fun windowGainedFocus(e: WindowEvent?) = ensureCursor()
                    override fun windowLostFocus(e: WindowEvent?) {}
                }
                window.addWindowFocusListener(focusListener)
                window.minimumSize = Dimension(380, 520)

                onDispose {
                    window.removeWindowFocusListener(focusListener)
                }
            }

            LaunchedEffect(windowState.placement) {
                EventQueue.invokeLater {
                    window.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
                    window.rootPane.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
                    window.contentPane.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
                }
            }

            // Taskbar-respecting custom maximize toggle for undecorated Compose Desktop window
            val toggleMaximize = {
                EventQueue.invokeLater {
                    if (!isMaximized) {
                        floatingBounds = window.bounds
                        val gc = window.graphicsConfiguration
                        val screenBounds = gc.bounds
                        val insets = Toolkit.getDefaultToolkit().getScreenInsets(gc)

                        val targetX = screenBounds.x + insets.left
                        val targetY = screenBounds.y + insets.top
                        val targetWidth = screenBounds.width - insets.left - insets.right
                        val targetHeight = screenBounds.height - insets.top - insets.bottom

                        window.setBounds(targetX, targetY, targetWidth, targetHeight)
                        isMaximized = true
                    } else {
                        val prev = floatingBounds
                        if (prev != null && prev.width >= 400 && prev.height >= 300) {
                            window.bounds = prev
                        } else {
                            val gc = window.graphicsConfiguration
                            val screenBounds = gc.bounds
                            val defaultW = 1040
                            val defaultH = 740
                            val targetX = screenBounds.x + (screenBounds.width - defaultW) / 2
                            val targetY = screenBounds.y + (screenBounds.height - defaultH) / 2
                            window.setBounds(targetX, targetY, defaultW, defaultH)
                        }
                        isMaximized = false
                    }
                    window.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
                    window.rootPane.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
                    window.contentPane.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
                }
            }

            LaunchedEffect(window) {
                window.dropTarget = object : java.awt.dnd.DropTarget() {
                    override fun drop(evt: java.awt.dnd.DropTargetDropEvent) {
                        try {
                            evt.acceptDrop(java.awt.dnd.DnDConstants.ACTION_COPY)
                            val transferable = evt.transferable
                            if (transferable.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.javaFileListFlavor)) {
                                val files = transferable.getTransferData(java.awt.datatransfer.DataFlavor.javaFileListFlavor) as? List<*>
                                val firstFile = files?.firstOrNull() as? File
                                if (firstFile != null && firstFile.exists()) {
                                    SharePayloadReceiver.dispatch(
                                        SharedPayload(
                                            filePath = firstFile.absolutePath,
                                            fileName = firstFile.name
                                        )
                                    )
                                    evt.dropComplete(true)
                                    return
                                }
                            } else if (transferable.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.stringFlavor)) {
                                val text = transferable.getTransferData(java.awt.datatransfer.DataFlavor.stringFlavor) as? String
                                if (!text.isNullOrBlank()) {
                                    SharePayloadReceiver.dispatch(SharedPayload(textOrUrl = text.trim()))
                                    evt.dropComplete(true)
                                    return
                                }
                            }
                            evt.dropComplete(false)
                        } catch (_: Exception) {
                            evt.dropComplete(false)
                        }
                    }
                }
            }

            IOCLensTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF041C24))
                        .pointerHoverIcon(PointerIcon.Default)
                ) {
                    // Custom Dead-Centered Window Title Bar with Permanently Anchored Right Controls (Enforce LTR for Windows OS invariants)
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        WindowDraggableArea {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                                    .background(Color(0xFF041C24))
                                    .padding(horizontal = 12.dp)
                            ) {
                                // Left Slot: Device Mode Toggle Button (Anchored to Far-Left)
                                Row(
                                    modifier = Modifier.align(Alignment.CenterStart),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { deviceModeActive = !deviceModeActive }
                                            .pointerHoverIcon(PointerIcon.Hand),
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (deviceModeActive) PrimaryCyan else Color.White.copy(alpha = 0.12f),
                                        border = androidx.compose.foundation.BorderStroke(0.8.dp, if (deviceModeActive) PrimaryCyan else Color.White.copy(alpha = 0.2f))
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "📱",
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }

                                // Center Slot: Dead-Centered Title Text with Safe Horizontal Insets and Ellipsis
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .padding(horizontal = 96.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (windowState.size.width < 520.dp) "IOC Lens" else "IOC Lens - Copy it. Know in a second.",
                                        color = PrimaryCyan,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Right Slot: Window Controls (Minimize, Maximize, Close Permanently Anchored to Far-Right)
                                Row(
                                    modifier = Modifier.align(Alignment.CenterEnd),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Minimize button (-)
                                    Surface(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .clickable { windowState.isMinimized = true }
                                            .pointerHoverIcon(PointerIcon.Hand),
                                        color = Color.White.copy(alpha = 0.12f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(text = "─", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    // Maximize / Restore button
                                    Surface(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .clickable { toggleMaximize() }
                                            .pointerHoverIcon(PointerIcon.Hand),
                                        color = Color.White.copy(alpha = 0.12f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = if (isMaximized) "❐" else "□",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    // Close button (✕) at the Far Right
                                    Surface(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .clickable { exitApplication() }
                                            .pointerHoverIcon(PointerIcon.Hand),
                                        color = Color(0xFFEF4444).copy(alpha = 0.85f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(text = "✕", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Device Simulation Toolbar (Visible on Desktop when Device Mode is active)
                    AnimatedVisibility(
                        visible = deviceModeActive,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        DeviceSimulatorToolbar(
                            selectedPreset = selectedDevicePreset,
                            onSelectPreset = { selectedDevicePreset = it },
                            isLandscape = isLandscapeOrientation,
                            onToggleOrientation = { isLandscapeOrientation = !isLandscapeOrientation },
                            showFrame = showDeviceFrameBezel,
                            onToggleFrame = { showDeviceFrameBezel = !showDeviceFrameBezel },
                            onClose = { deviceModeActive = false },
                            onSnapWindow = { targetW, targetH ->
                                val titleAndToolbarOffset = 38 + 42
                                val framePadding = 48
                                val newW = (targetW + framePadding).coerceAtLeast(420)
                                val newH = (targetH + titleAndToolbarOffset + framePadding).coerceAtLeast(500)
                                val gc = window.graphicsConfiguration
                                val screenBounds = gc.bounds
                                val targetX = (screenBounds.x + (screenBounds.width - newW) / 2).coerceAtLeast(screenBounds.x)
                                val targetY = (screenBounds.y + (screenBounds.height - newH) / 2).coerceAtLeast(screenBounds.y)
                                window.setBounds(targetX, targetY, newW, newH)
                            }
                        )
                    }

                    // Main Application Content (Normal or inside Device Chassis)
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (deviceModeActive) {
                            DeviceChassis(
                                preset = selectedDevicePreset,
                                isLandscape = isLandscapeOrientation,
                                showFrame = showDeviceFrameBezel
                            ) {
                                App()
                            }
                        } else {
                            App()
                        }
                    }
                }
            }
        }
    }
}

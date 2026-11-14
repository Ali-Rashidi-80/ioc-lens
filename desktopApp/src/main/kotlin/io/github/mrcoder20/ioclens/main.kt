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
import androidx.compose.foundation.layout.Spacer
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
import io.github.mrcoder20.ioclens.core.components.AppLogoImage
import io.github.mrcoder20.ioclens.core.components.rememberAppLogoPainter
import io.github.mrcoder20.ioclens.core.theme.BackgroundDark
import io.github.mrcoder20.ioclens.core.theme.IOCLensTheme
import io.github.mrcoder20.ioclens.core.theme.PrimaryCyan
import io.github.mrcoder20.ioclens.domain.security.SharePayloadReceiver
import io.github.mrcoder20.ioclens.domain.security.SharedPayload
import java.awt.AWTEvent
import java.awt.Cursor
import java.awt.Dimension
import java.awt.EventQueue
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import java.io.File

/**
 * Intercepts all unhandled exceptions on the AWT Event Dispatch Thread (EDT).
 * This completely prevents jpackage's native launcher from displaying
 * raw modal crash popups ("Error: Can't wrap nullptr") and terminating the app process.
 */
class SafeEventQueue : EventQueue() {
    override fun dispatchEvent(event: AWTEvent) {
        try {
            super.dispatchEvent(event)
        } catch (t: Throwable) {
            val msg = t.message ?: ""
            if (msg.contains("Can't wrap nullptr") || msg.contains("nullptr")) {
                System.err.println("[IOCLens] Gracefully suppressed Skiko native nullptr on EDT: $msg")
            } else {
                System.err.println("[IOCLens] Uncaught EventQueue error: $msg")
                t.printStackTrace()
            }
        }
    }

    companion object {
        fun install() {
            try {
                Toolkit.getDefaultToolkit().systemEventQueue.push(SafeEventQueue())
            } catch (e: Throwable) {
                System.err.println("[IOCLens] Failed to install SafeEventQueue: ${e.message}")
            }
        }
    }
}

fun main(args: Array<String>) {
    // 1. Install EDT exception guard to prevent jpackage fatal native crash popups
    SafeEventQueue.install()

    // 2. High-performance desktop networking and encoding optimizations
    System.setProperty("java.net.preferIPv6Addresses", "false")
    System.setProperty("sun.net.useExclusiveBind", "false")
    System.setProperty("file.encoding", "UTF-8")

    // 3. Global Uncaught Exception Handler
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        val msg = throwable.message ?: ""
        if (msg.contains("Can't wrap nullptr") || msg.contains("nullptr")) {
            System.err.println("[IOCLens] Suppressed Skiko nullptr on thread ${thread.name}")
        } else {
            System.err.println("[IOCLens] Handled uncaught exception on thread ${thread.name}: $msg")
            throwable.printStackTrace()
        }
    }

    application {
        remember {
            val arg = args.firstOrNull()?.trim()
            if (!arg.isNullOrBlank()) {
                DesktopDropHandler.processStartupArg(arg) { payload ->
                    SharePayloadReceiver.dispatch(payload)
                }
            }
        }
        val initialBounds = remember { WindowBoundsClamper.resolveInitialBounds() }
        val windowState = rememberWindowState(
            placement = WindowPlacement.Floating,
            position = initialBounds.position,
            size = initialBounds.size
        )

        val appLogoPainter = rememberAppLogoPainter()

        Window(
            onCloseRequest = ::exitApplication,
            title = "IOC Lens - Copy it. Know in a second.",
            state = windowState,
            icon = appLogoPainter,
            undecorated = true
        ) {
            var isMaximized by remember { mutableStateOf(false) }
            var floatingBounds by remember { mutableStateOf<Rectangle?>(null) }

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
                            window.bounds = WindowBoundsClamper.resolveRestoreBounds(window.graphicsConfiguration)
                        }
                        isMaximized = false
                    }
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
                                val handled = DesktopDropHandler.processDroppedFiles(files) { payload ->
                                    SharePayloadReceiver.dispatch(payload)
                                }
                                if (handled) {
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
                        .background(BackgroundDark)
                        .pointerHoverIcon(PointerIcon.Default)
                ) {
                    // Custom Dead-Centered Window Title Bar with Permanently Anchored Right Controls (Enforce LTR for Windows OS invariants)
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        WindowDraggableArea {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                                    .background(BackgroundDark)
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left Slot: App Brand & Logo Icon
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(start = 4.dp)
                                ) {
                                    AppLogoImage(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                    )
                                    Text(
                                        text = "IOC Lens",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                // Center Slot: Full Draggable Space (Clean, Minimal, Modern)
                                Spacer(modifier = Modifier.weight(1f))

                                // Right Slot: Window Controls (Minimize, Maximize, Close - Permanently Anchored to Far-Right)
                                Row(
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

                    // Main Application Content
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        App()
                    }
                }
            }
        }
    }
}

package io.github.mrcoder20.ioclens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.mrcoder20.ioclens.core.components.AppLogoImage
import io.github.mrcoder20.ioclens.core.components.rememberAppLogoPainter
import io.github.mrcoder20.ioclens.core.localization.LocalizationProvider
import io.github.mrcoder20.ioclens.core.theme.IOCLensTheme
import io.github.mrcoder20.ioclens.core.theme.LocalIocColors
import io.github.mrcoder20.ioclens.domain.security.SharePayloadReceiver
import io.github.mrcoder20.ioclens.domain.security.SharedPayload
import io.github.mrcoder20.ioclens.presentation.vision.DesktopDragAndDropCoordinator
import java.awt.AWTEvent
import java.awt.Cursor
import java.awt.Dimension
import java.awt.EventQueue
import java.awt.Frame
import java.awt.MouseInfo
import java.awt.Point
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener

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
                logDiagnostic("SafeEventQueue", "Suppressed Skiko native nullptr on EDT: $msg")
            } else {
                logDiagnostic("SafeEventQueue", "Uncaught EventQueue error", t)
            }
        }
    }

    companion object {
        fun install() {
            try {
                Toolkit.getDefaultToolkit().systemEventQueue.push(SafeEventQueue())
            } catch (e: Throwable) {
                logDiagnostic("SafeEventQueue", "Failed to install SafeEventQueue", e)
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
            logDiagnostic("DesktopApp", "Suppressed Skiko nullptr on thread ${thread.name}")
        } else {
            logDiagnostic("DesktopApp", "Handled uncaught exception on thread ${thread.name}", throwable)
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
        val desktopSettings by AppDependencies.settingsRepository.settingsState.collectAsState()
        val strings = LocalizationProvider.getStrings(desktopSettings.appLanguage)
        val initialBounds = remember { WindowBoundsClamper.resolveInitialBounds() }
        val windowState = rememberWindowState(
            placement = WindowPlacement.Floating,
            position = initialBounds.position,
            size = initialBounds.size
        )

        val appLogoPainter = rememberAppLogoPainter()

        Window(
            onCloseRequest = ::exitApplication,
            title = "${strings.appTitle} - ${strings.appTagline}",
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
                window.minimumSize = Dimension(420, 520)

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

            androidx.compose.runtime.LaunchedEffect(window) {
                val mainWindowDropListener = DesktopDragAndDropCoordinator.createSafeDropListener(
                    onFilesDropped = { files ->
                        if (DesktopDragAndDropCoordinator.isVisionActive() && files.size == 1 && DesktopDragAndDropCoordinator.isImageFile(files.first())) {
                            DesktopDragAndDropCoordinator.visionDropHandler?.invoke(files.first())
                        } else {
                            DesktopDropHandler.processDroppedFiles(files) { payload ->
                                SharePayloadReceiver.dispatch(payload)
                            }
                        }
                    },
                    onImageDropped = { img ->
                        if (DesktopDragAndDropCoordinator.isVisionActive()) {
                            DesktopDragAndDropCoordinator.visionImageDropHandler?.invoke(img)
                        }
                    },
                    onTextDropped = { text ->
                        SharePayloadReceiver.dispatch(SharedPayload(textOrUrl = text.trim()))
                    }
                )

                // Attach safe DropTarget only to window contentPane to eliminate COM/OLE thread deadlocks
                try {
                    window.contentPane.dropTarget = DropTarget(
                        window.contentPane,
                        DnDConstants.ACTION_COPY_OR_MOVE,
                        mainWindowDropListener,
                        true
                    )
                } catch (primaryErr: Throwable) {
                    logDiagnostic("DesktopApp", "ContentPane DropTarget registration failed, falling back to window", primaryErr)
                    try {
                        window.dropTarget = DropTarget(
                            window,
                            DnDConstants.ACTION_COPY_OR_MOVE,
                            mainWindowDropListener,
                            true
                        )
                    } catch (fallbackErr: Throwable) {
                        logDiagnostic("DesktopApp", "Fallback DropTarget registration failed", fallbackErr)
                    }
                }
            }

            IOCLensTheme(
                appTheme = desktopSettings.appTheme,
                appLanguage = desktopSettings.appLanguage
            ) {
                val palette = LocalIocColors.current
                var lastTitlePressNanos by remember { mutableStateOf(0L) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(palette.backgroundCanvas)
                        .border(if (isMaximized) 0.dp else 1.dp, palette.borderSubtle)
                        .pointerHoverIcon(PointerIcon.Default)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Custom Window Title Bar with Permanently Anchored Right Controls (Enforce LTR for OS invariants)
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            WindowDraggableArea(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp)
                                        .background(palette.surfaceElevated)
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
                                            text = strings.appTitle,
                                            color = palette.textSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    // Center Draggable Area with non-consuming Double-Click Maximize/Restore
                                    Spacer(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .pointerInput(Unit) {
                                                awaitPointerEventScope {
                                                    while (true) {
                                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                                        if (event.type == PointerEventType.Press &&
                                                            event.changes.any { it.pressed && !it.previousPressed }
                                                        ) {
                                                            val now = System.nanoTime()
                                                            if (lastTitlePressNanos != 0L && (now - lastTitlePressNanos) <= 350_000_000L) {
                                                                lastTitlePressNanos = 0L
                                                                toggleMaximize()
                                                            } else {
                                                                lastTitlePressNanos = now
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                    )

                                    // Right Slot: Window Controls (Minimize, Maximize, Close)
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .clickable { windowState.isMinimized = true }
                                                .pointerHoverIcon(PointerIcon.Hand),
                                            color = palette.surfaceInset
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(text = "─", color = palette.textPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Surface(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .clickable { toggleMaximize() }
                                                .pointerHoverIcon(PointerIcon.Hand),
                                            color = palette.surfaceInset
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = if (isMaximized) "❐" else "□",
                                                    color = palette.textPrimary,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Surface(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .clickable { exitApplication() }
                                                .pointerHoverIcon(PointerIcon.Hand),
                                            color = palette.verdictMalicious
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(text = "✕", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(thickness = 1.dp, color = palette.borderSubtle)

                        // Main Application Content
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            App()
                        }
                    }

                    if (!isMaximized) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            WindowResizeHandlesOverlay(window = window)
                        }
                    }
                }
            }
        }
    }
}

private enum class ResizeDirection(val cursorType: Int, val left: Boolean, val top: Boolean, val right: Boolean, val bottom: Boolean) {
    TOP(Cursor.N_RESIZE_CURSOR, false, true, false, false),
    BOTTOM(Cursor.S_RESIZE_CURSOR, false, false, false, true),
    LEFT(Cursor.W_RESIZE_CURSOR, true, false, false, false),
    RIGHT(Cursor.E_RESIZE_CURSOR, false, false, true, false),
    TOP_LEFT(Cursor.NW_RESIZE_CURSOR, true, true, false, false),
    TOP_RIGHT(Cursor.NE_RESIZE_CURSOR, false, true, true, false),
    BOTTOM_LEFT(Cursor.SW_RESIZE_CURSOR, true, false, false, true),
    BOTTOM_RIGHT(Cursor.SE_RESIZE_CURSOR, false, false, true, true)
}

@Composable
private fun BoxScope.WindowResizeHandlesOverlay(window: Frame) {
    val handleThickness = 6.dp
    val cornerSize = 12.dp
    val handles = listOf(
        ResizeDirection.TOP to Modifier.align(Alignment.TopCenter).fillMaxWidth().height(handleThickness),
        ResizeDirection.BOTTOM to Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(handleThickness),
        ResizeDirection.LEFT to Modifier.align(Alignment.CenterStart).fillMaxHeight().width(handleThickness),
        ResizeDirection.RIGHT to Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(handleThickness),
        ResizeDirection.TOP_LEFT to Modifier.align(Alignment.TopStart).size(cornerSize),
        ResizeDirection.TOP_RIGHT to Modifier.align(Alignment.TopEnd).size(cornerSize),
        ResizeDirection.BOTTOM_LEFT to Modifier.align(Alignment.BottomStart).size(cornerSize),
        ResizeDirection.BOTTOM_RIGHT to Modifier.align(Alignment.BottomEnd).size(cornerSize)
    )

    handles.forEach { (direction, alignModifier) ->
        var dragStartPointer by remember { mutableStateOf<Point?>(null) }
        var dragStartBounds by remember { mutableStateOf<Rectangle?>(null) }

        Box(
            modifier = alignModifier
                .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(direction.cursorType)))
                .pointerInput(direction) {
                    detectDragGestures(
                        onDragStart = {
                            dragStartPointer = MouseInfo.getPointerInfo()?.location
                            dragStartBounds = window.bounds
                        },
                        onDragEnd = {
                            dragStartPointer = null
                            dragStartBounds = null
                        },
                        onDragCancel = {
                            dragStartPointer = null
                            dragStartBounds = null
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val startPt = dragStartPointer ?: return@detectDragGestures
                            val startRect = dragStartBounds ?: return@detectDragGestures
                            val currentPt = MouseInfo.getPointerInfo()?.location ?: return@detectDragGestures
                            val dx = currentPt.x - startPt.x
                            val dy = currentPt.y - startPt.y

                            val minWidth = 420
                            val minHeight = 520
                            var newX = startRect.x
                            var newY = startRect.y
                            var newW = startRect.width
                            var newH = startRect.height

                            if (direction.left) {
                                val nextW = (startRect.width - dx).coerceAtLeast(minWidth)
                                newX = startRect.x + (startRect.width - nextW)
                                newW = nextW
                            }
                            if (direction.right) {
                                newW = (startRect.width + dx).coerceAtLeast(minWidth)
                            }
                            if (direction.top) {
                                val nextH = (startRect.height - dy).coerceAtLeast(minHeight)
                                newY = startRect.y + (startRect.height - nextH)
                                newH = nextH
                            }
                            if (direction.bottom) {
                                newH = (startRect.height + dy).coerceAtLeast(minHeight)
                            }

                            EventQueue.invokeLater {
                                window.setBounds(newX, newY, newW, newH)
                            }
                        }
                    )
                }
        )
    }
}

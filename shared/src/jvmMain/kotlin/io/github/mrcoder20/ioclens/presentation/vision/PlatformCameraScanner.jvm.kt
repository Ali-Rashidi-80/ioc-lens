package io.github.mrcoder20.ioclens.presentation.vision

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mrcoder20.ioclens.core.components.CameraLensVectorIcon
import io.github.mrcoder20.ioclens.core.components.ClipboardVectorIcon
import io.github.mrcoder20.ioclens.core.components.QrMatrixVectorIcon
import io.github.mrcoder20.ioclens.core.components.rememberFilePickerLauncher
import io.github.mrcoder20.ioclens.core.theme.PrimaryCyan
import io.github.mrcoder20.ioclens.core.theme.PrimaryNeon
import io.github.mrcoder20.ioclens.core.theme.SlateBorder
import io.github.mrcoder20.ioclens.domain.vision.BarcodePayload
import io.github.mrcoder20.ioclens.domain.vision.PlatformVisualDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Component
import java.awt.Container
import java.awt.Window
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.awt.image.BufferedImage
import java.io.File
import javax.swing.RootPaneContainer

@Composable
actual fun PlatformCameraScanner(
    onPayloadsDetected: (List<BarcodePayload>) -> Unit,
    modifier: Modifier
) {
    val decoder = remember { PlatformVisualDecoder() }
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isDecoding by remember { mutableStateOf(false) }
    var dismissJob by remember { mutableStateOf<Job?>(null) }

    fun showSnackbar(msg: String, durationMs: Long = 3500L) {
        statusMessage = msg
        dismissJob?.cancel()
        dismissJob = scope.launch {
            delay(durationMs)
            statusMessage = null
        }
    }

    fun processClipboard(silent: Boolean = false) {
        if (isDecoding) return
        isDecoding = true
        if (!silent) {
            showSnackbar("Analyzing clipboard screenshot...", 2000L)
        }
        scope.launch(Dispatchers.IO) {
            val payloads = decoder.decodeFromClipboard()
            withContext(Dispatchers.Main) {
                isDecoding = false
                if (payloads.isNotEmpty()) {
                    statusMessage = null
                    onPayloadsDetected(payloads)
                } else if (!silent) {
                    showSnackbar("No valid 1D/2D barcode found in clipboard image.")
                }
            }
        }
    }

    val filePickerLauncher = rememberFilePickerLauncher { filePath, _ ->
        if (filePath.isNotBlank()) {
            isDecoding = true
            showSnackbar("Analyzing selected image file...", 2000L)
            scope.launch(Dispatchers.IO) {
                val file = File(filePath)
                val payloads = if (file.exists() && file.isFile) {
                    val bytes = runCatching { file.readBytes() }.getOrDefault(byteArrayOf())
                    decoder.decodeBarcodeFromBytes(bytes)
                } else {
                    emptyList()
                }
                withContext(Dispatchers.Main) {
                    isDecoding = false
                    if (payloads.isNotEmpty()) {
                        statusMessage = null
                        onPayloadsDetected(payloads)
                    } else {
                        showSnackbar("No barcode detected in selected image.")
                    }
                }
            }
        }
    }

    // Unified AWT DropTarget supporting files and images with copy cursor negotiation
    val dropTarget = remember {
        object : DropTarget() {
            override fun dragEnter(dtde: DropTargetDragEvent) {
                if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor) ||
                    dtde.isDataFlavorSupported(DataFlavor.imageFlavor) ||
                    dtde.isDataFlavorSupported(DataFlavor.stringFlavor)
                ) {
                    dtde.acceptDrag(DnDConstants.ACTION_COPY)
                } else {
                    dtde.rejectDrag()
                }
            }

            override fun dragOver(dtde: DropTargetDragEvent) {
                if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor) ||
                    dtde.isDataFlavorSupported(DataFlavor.imageFlavor) ||
                    dtde.isDataFlavorSupported(DataFlavor.stringFlavor)
                ) {
                    dtde.acceptDrag(DnDConstants.ACTION_COPY)
                } else {
                    dtde.rejectDrag()
                }
            }

            override fun drop(evt: DropTargetDropEvent) {
                try {
                    evt.acceptDrop(DnDConstants.ACTION_COPY)
                    val transferable = evt.transferable
                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>
                        val validFile = files?.filterIsInstance<File>()?.firstOrNull { it.isFile && it.exists() }
                        if (validFile != null) {
                            isDecoding = true
                            showSnackbar("Decoding ${validFile.name}...", 2000L)
                            scope.launch(Dispatchers.IO) {
                                val bytes = runCatching { validFile.readBytes() }.getOrDefault(byteArrayOf())
                                val payloads = decoder.decodeBarcodeFromBytes(bytes)
                                withContext(Dispatchers.Main) {
                                    isDecoding = false
                                    if (payloads.isNotEmpty()) {
                                        statusMessage = null
                                        onPayloadsDetected(payloads)
                                    } else {
                                        showSnackbar("No barcode detected in ${validFile.name}")
                                    }
                                }
                            }
                            evt.dropComplete(true)
                            return
                        }
                    } else if (transferable.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                        val img = transferable.getTransferData(DataFlavor.imageFlavor) as? java.awt.Image
                        val bufferedImg: BufferedImage? = when (img) {
                            is BufferedImage -> img
                            null -> null
                            else -> {
                                val w = img.getWidth(null).coerceAtLeast(1)
                                val h = img.getHeight(null).coerceAtLeast(1)
                                val b = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
                                val g = b.createGraphics()
                                g.drawImage(img, 0, 0, null)
                                g.dispose()
                                b
                            }
                        }
                        if (bufferedImg != null) {
                            isDecoding = true
                            showSnackbar("Decoding dropped image...", 2000L)
                            scope.launch(Dispatchers.IO) {
                                val payloads = decoder.decodeBufferedImage(bufferedImg)
                                withContext(Dispatchers.Main) {
                                    isDecoding = false
                                    if (payloads.isNotEmpty()) {
                                        statusMessage = null
                                        onPayloadsDetected(payloads)
                                    } else {
                                        showSnackbar("No barcode detected in dropped image.")
                                    }
                                }
                            }
                            evt.dropComplete(true)
                            return
                        }
                    } else if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                        val text = (transferable.getTransferData(DataFlavor.stringFlavor) as? String)?.trim().orEmpty()
                        if (text.isNotBlank()) {
                            val cleanPath = text.removePrefix("file://").removePrefix("file:///").trim()
                            val candidateFile = File(cleanPath)
                            if (candidateFile.isFile && candidateFile.exists()) {
                                isDecoding = true
                                showSnackbar("Decoding ${candidateFile.name}...", 2000L)
                                scope.launch(Dispatchers.IO) {
                                    val bytes = runCatching { candidateFile.readBytes() }.getOrDefault(byteArrayOf())
                                    val payloads = decoder.decodeBarcodeFromBytes(bytes)
                                    withContext(Dispatchers.Main) {
                                        isDecoding = false
                                        if (payloads.isNotEmpty()) {
                                            statusMessage = null
                                            onPayloadsDetected(payloads)
                                        } else {
                                            showSnackbar("No barcode detected in ${candidateFile.name}")
                                        }
                                    }
                                }
                                evt.dropComplete(true)
                                return
                            }
                        }
                    }
                    evt.dropComplete(false)
                } catch (_: Exception) {
                    evt.dropComplete(false)
                }
            }
        }
    }

    val trackedComponents = remember { mutableMapOf<Component, DropTarget?>() }

    fun attachRecursively(comp: Component) {
        if (!trackedComponents.containsKey(comp)) {
            trackedComponents[comp] = comp.dropTarget
        }
        comp.dropTarget = dropTarget
        if (comp is Container) {
            for (child in comp.components) {
                attachRecursively(child)
            }
        }
        if (comp is RootPaneContainer) {
            comp.rootPane?.let { attachRecursively(it) }
            comp.contentPane?.let { attachRecursively(it) }
            comp.glassPane?.let { attachRecursively(it) }
            comp.layeredPane?.let { attachRecursively(it) }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            dismissJob?.cancel()
            // Restore original drop targets
            trackedComponents.forEach { (comp, oldDt) ->
                try {
                    comp.dropTarget = oldDt
                } catch (_: Exception) {}
            }
            trackedComponents.clear()
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()

        // Silent clipboard check on open: if user already has a QR code in clipboard, decode it!
        // If not, DO NOT display any error message (remain silent).
        processClipboard(silent = true)

        // Continuously attach drop target to all showing windows, dialogs, and component hierarchies
        while (isActive) {
            val visibleWindows = Window.getWindows().filter { it.isShowing && it.isVisible }
            for (win in visibleWindows) {
                attachRecursively(win)
            }
            delay(300)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                if ((event.isCtrlPressed || event.isMetaPressed) && event.key == Key.V) {
                    processClipboard(silent = false)
                    true
                } else {
                    false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Cyber Scanner Graphic Box - Click opens file picker or pastes
            Surface(
                modifier = Modifier
                    .width(170.dp)
                    .height(115.dp)
                    .border(
                        BorderStroke(1.5.dp, Brush.linearGradient(listOf(PrimaryCyan, PrimaryNeon.copy(alpha = 0.5f)))),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { filePickerLauncher() },
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.85f)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        QrMatrixVectorIcon(tint = PrimaryCyan, size = 42.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "DROP / BROWSE",
                            color = PrimaryCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Drag & Drop QR image or Snip with Win+Shift+S",
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Supports QR, Data Matrix, Aztec, PDF417 & 1D Barcodes",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { processClipboard(silent = false) },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ClipboardVectorIcon(tint = Color.Black, size = 16.dp)
                        Text(
                            text = "Paste (Ctrl+V)",
                            color = Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                OutlinedButton(
                    onClick = { filePickerLauncher() },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, SlateBorder),
                    modifier = Modifier.height(38.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CameraLensVectorIcon(tint = Color.White, size = 16.dp)
                        Text(
                            text = "Browse File",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Floating Cyber Snackbar / Notification Toast (Never shifts or squishes layout)
        AnimatedVisibility(
            visible = statusMessage != null,
            enter = fadeIn(tween(200)) + slideInVertically(tween(250)) { it },
            exit = fadeOut(tween(200)) + slideOutVertically(tween(250)) { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp, start = 16.dp, end = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1E1B2E).copy(alpha = 0.96f),
                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.65f)),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("⚠️", fontSize = 12.sp)
                    Text(
                        text = statusMessage.orEmpty(),
                        color = Color(0xFFFCA5A5),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "✕",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { statusMessage = null }
                            .padding(2.dp)
                    )
                }
            }
        }
    }
}

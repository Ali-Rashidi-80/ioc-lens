package io.github.mrcoder20.ioclens.presentation.vision

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Component
import java.awt.Image
import java.awt.Window
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.image.BufferedImage
import java.io.File
import javax.swing.JDialog

@Composable
actual fun PlatformCameraScanner(
    onPayloadsDetected: (List<BarcodePayload>) -> Unit,
    modifier: Modifier
) {
    val decoder = remember { PlatformVisualDecoder() }
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isStatusError by remember { mutableStateOf(false) }
    var isDecoding by remember { mutableStateOf(false) }
    var dismissJob by remember { mutableStateOf<Job?>(null) }

    fun showSnackbar(msg: String, isError: Boolean = false, durationMs: Long = 3200L) {
        statusMessage = msg
        isStatusError = isError
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
            showSnackbar("Analyzing clipboard image...", isError = false, durationMs = 2000L)
        }
        scope.launch(Dispatchers.IO) {
            val payloads = decoder.decodeFromClipboard()
            withContext(Dispatchers.Main) {
                isDecoding = false
                if (payloads.isNotEmpty()) {
                    statusMessage = null
                    onPayloadsDetected(payloads)
                } else if (!silent) {
                    showSnackbar("No barcode found in clipboard", isError = true)
                }
            }
        }
    }

    val filePickerLauncher = rememberFilePickerLauncher { filePath, _ ->
        if (filePath.isNotBlank()) {
            isDecoding = true
            showSnackbar("Analyzing selected image file...", isError = false, durationMs = 2000L)
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
                        showSnackbar("No barcode detected in selected image.", isError = true)
                    }
                }
            }
        }
    }

    fun processDroppedFile(file: File) {
        if (!file.exists() || !file.isFile) return
        isDecoding = true
        showSnackbar("Decoding ${file.name}...", isError = false, durationMs = 2000L)
        scope.launch(Dispatchers.IO) {
            val bytes = runCatching { file.readBytes() }.getOrDefault(byteArrayOf())
            val payloads = if (bytes.isNotEmpty()) decoder.decodeBarcodeFromBytes(bytes) else emptyList()
            withContext(Dispatchers.Main) {
                isDecoding = false
                if (payloads.isNotEmpty()) {
                    statusMessage = null
                    onPayloadsDetected(payloads)
                } else {
                    showSnackbar("No barcode detected in ${file.name}", isError = true)
                }
            }
        }
    }

    fun processDroppedImage(img: Image) {
        isDecoding = true
        showSnackbar("Decoding dropped image...", isError = false, durationMs = 2000L)
        scope.launch(Dispatchers.IO) {
            val w = img.getWidth(null).coerceAtLeast(1)
            val h = img.getHeight(null).coerceAtLeast(1)
            val buffered = if (img is BufferedImage) img else {
                val b = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
                val g = b.createGraphics()
                g.drawImage(img, 0, 0, null)
                g.dispose()
                b
            }
            val payloads = decoder.decodeBufferedImage(buffered)
            withContext(Dispatchers.Main) {
                isDecoding = false
                if (payloads.isNotEmpty()) {
                    statusMessage = null
                    onPayloadsDetected(payloads)
                } else {
                    showSnackbar("No barcode detected in dropped image.", isError = true)
                }
            }
        }
    }

    // Register active Vision drop handlers and attach a single DropTarget to the Dialog window
    DisposableEffect(Unit) {
        DesktopDragAndDropCoordinator.visionDropHandler = { file -> processDroppedFile(file) }
        DesktopDragAndDropCoordinator.visionImageDropHandler = { img -> processDroppedImage(img) }

        val activeDialog = Window.getWindows()
            .filterIsInstance<JDialog>()
            .firstOrNull { it.isShowing && it.isVisible }

        val targetComp: Component? = activeDialog?.contentPane ?: activeDialog
        val oldDt = targetComp?.dropTarget

        if (targetComp != null) {
            val dialogDropListener = DesktopDragAndDropCoordinator.createSafeDropListener(
                onFilesDropped = { files ->
                    val candidate = files.firstOrNull { it.isFile && it.exists() }
                    if (candidate != null) {
                        processDroppedFile(candidate)
                    }
                },
                onImageDropped = { img ->
                    processDroppedImage(img)
                },
                onTextDropped = { text ->
                    val file = DesktopDragAndDropCoordinator.parseDroppedTextToFile(text)
                    if (file != null) {
                        processDroppedFile(file)
                    }
                }
            )
            try {
                targetComp.dropTarget = DropTarget(targetComp, DnDConstants.ACTION_COPY_OR_MOVE, dialogDropListener, true)
            } catch (_: Throwable) {}
        }

        onDispose {
            dismissJob?.cancel()
            DesktopDragAndDropCoordinator.visionDropHandler = null
            DesktopDragAndDropCoordinator.visionImageDropHandler = null
            if (targetComp != null) {
                try {
                    targetComp.dropTarget = oldDt
                } catch (_: Throwable) {}
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        // Silent check on open: if user already snipped a QR code, auto-detect it without any error banner!
        processClipboard(silent = true)
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Cyber Scanner Graphic Box - Click opens file picker
            Surface(
                modifier = Modifier
                    .width(160.dp)
                    .height(105.dp)
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
                        QrMatrixVectorIcon(tint = PrimaryCyan, size = 40.dp)
                        Spacer(modifier = Modifier.height(6.dp))
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

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Drag & Drop QR image or Snip with Win+Shift+S",
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 12.5.sp,
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

            Spacer(modifier = Modifier.height(14.dp))

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

        // Floating Cyber Snackbar / Notification Toast (Only visible when statusMessage is not blank, zero layout shift)
        AnimatedVisibility(
            visible = !statusMessage.isNullOrBlank(),
            enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { it / 2 },
            exit = fadeOut(tween(180)) + slideOutVertically(tween(220)) { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp, start = 12.dp, end = 12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.96f),
                border = BorderStroke(1.dp, if (isStatusError) Color(0xFFEF4444).copy(alpha = 0.7f) else PrimaryCyan.copy(alpha = 0.7f)),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(if (isStatusError) "⚠️" else "🔍", fontSize = 11.sp)
                    Text(
                        text = statusMessage.orEmpty(),
                        color = if (isStatusError) Color(0xFFFCA5A5) else PrimaryCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "✕",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
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

package io.github.mrcoder20.ioclens.presentation.vision

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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

    fun processClipboard() {
        if (isDecoding) return
        isDecoding = true
        statusMessage = "Analyzing clipboard screenshot..."
        scope.launch(Dispatchers.IO) {
            val payloads = decoder.decodeFromClipboard()
            withContext(Dispatchers.Main) {
                isDecoding = false
                if (payloads.isNotEmpty()) {
                    statusMessage = null
                    onPayloadsDetected(payloads)
                } else {
                    statusMessage = "No valid 1D/2D barcode found in clipboard image."
                }
            }
        }
    }

    val filePickerLauncher = rememberFilePickerLauncher { filePath, _ ->
        if (filePath.isNotBlank()) {
            isDecoding = true
            statusMessage = "Analyzing selected image file..."
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
                        statusMessage = "No barcode detected in selected image."
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        // Automatically attempt reading clipboard once on open
        processClipboard()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                if ((event.isCtrlPressed || event.isMetaPressed) && event.key == Key.V) {
                    processClipboard()
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Cyber Scanner Graphic Box
            Surface(
                modifier = Modifier
                    .size(160.dp)
                    .border(
                        BorderStroke(1.5.dp, Brush.linearGradient(listOf(PrimaryCyan, PrimaryNeon.copy(alpha = 0.5f)))),
                        RoundedCornerShape(20.dp)
                    )
                    .clickable { processClipboard() },
                shape = RoundedCornerShape(20.dp),
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
                        QrMatrixVectorIcon(tint = PrimaryCyan, size = 48.dp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "DROP / PASTE",
                            color = PrimaryCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Snip any QR with Win+Shift+S and Paste here",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Supports QR, Data Matrix, Aztec, PDF417 & 1D Barcodes",
                color = Color.Gray,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )

            if (statusMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = Color(0xFFEF4444).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f))
                ) {
                    Text(
                        text = statusMessage!!,
                        color = Color(0xFFFCA5A5),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { processClipboard() },
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
    }
}

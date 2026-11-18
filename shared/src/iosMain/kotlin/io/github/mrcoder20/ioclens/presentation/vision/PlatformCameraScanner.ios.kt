package io.github.mrcoder20.ioclens.presentation.vision

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mrcoder20.ioclens.core.components.CameraLensVectorIcon
import io.github.mrcoder20.ioclens.core.components.QrMatrixVectorIcon
import io.github.mrcoder20.ioclens.core.components.rememberFilePickerLauncher
import io.github.mrcoder20.ioclens.core.localization.LocaleManager
import io.github.mrcoder20.ioclens.core.localization.LocalizationProvider
import io.github.mrcoder20.ioclens.core.theme.LocalIocColors
import io.github.mrcoder20.ioclens.domain.vision.BarcodePayload
import io.github.mrcoder20.ioclens.domain.vision.IosDecodedBarcode
import io.github.mrcoder20.ioclens.domain.vision.PlatformVisualDecoder
import io.github.mrcoder20.ioclens.domain.vision.QuishingAnalyzer
import io.github.mrcoder20.ioclens.domain.vision.resolveBarcodeTypeFromHint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.concurrent.Volatile

interface IosCameraScannerDelegate {
    fun openNativeBarcodeScanner(onResult: (List<IosDecodedBarcode>) -> Unit)
}

object IosCameraScannerBridge {
    @Volatile
    var delegate: IosCameraScannerDelegate? = null
}

@Composable
actual fun PlatformCameraScanner(
    onPayloadsDetected: (List<BarcodePayload>) -> Unit,
    modifier: Modifier
) {
    val appLanguage by LocaleManager.currentLanguage.collectAsState()
    val strings = LocalizationProvider.getStrings(appLanguage)
    val palette = LocalIocColors.current
    val decoder = remember { PlatformVisualDecoder() }
    val scope = rememberCoroutineScope()
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val filePicker = rememberFilePickerLauncher { filePath, _ ->
        scope.launch(Dispatchers.IO) {
            val payloads = decoder.decodeFileAtPath(filePath)
            withContext(Dispatchers.Main) {
                if (payloads.isNotEmpty()) {
                    statusMessage = null
                    onPayloadsDetected(payloads)
                } else {
                    statusMessage = strings.visionNoBarcodeFound
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.surfaceCard),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            QrMatrixVectorIcon(tint = palette.accentPrimary, size = 44.dp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = strings.visionTitle,
                color = palette.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = strings.visionCameraPermDesc,
                color = palette.textSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (IosCameraScannerBridge.delegate != null) {
                    Button(
                        onClick = {
                            IosCameraScannerBridge.delegate?.openNativeBarcodeScanner { items ->
                                val analyzed = items.filter { it.text.isNotBlank() }
                                    .map { QuishingAnalyzer.analyze(it.text, resolveBarcodeTypeFromHint(it.formatHint)) }
                                if (analyzed.isNotEmpty()) {
                                    statusMessage = null
                                    onPayloadsDetected(analyzed)
                                } else {
                                    statusMessage = strings.visionNoBarcodeFound
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accentPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        CameraLensVectorIcon(tint = palette.onAccentPrimary, size = 16.dp)
                        Spacer(modifier = Modifier.padding(horizontal = 3.dp))
                        Text(strings.visionTitle, color = palette.onAccentPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                OutlinedButton(
                    onClick = filePicker,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, palette.borderStrong)
                ) {
                    Text(strings.visionBrowseImage, color = palette.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            AnimatedVisibility(visible = !statusMessage.isNullOrBlank()) {
                Surface(
                    modifier = Modifier.padding(top = 12.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = palette.verdictMalicious.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, palette.verdictMalicious.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = statusMessage.orEmpty(),
                        color = palette.verdictMalicious,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

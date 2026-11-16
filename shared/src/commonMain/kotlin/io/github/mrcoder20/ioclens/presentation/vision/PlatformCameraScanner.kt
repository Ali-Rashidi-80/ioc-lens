package io.github.mrcoder20.ioclens.presentation.vision

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.mrcoder20.ioclens.domain.vision.BarcodePayload

@Composable
expect fun PlatformCameraScanner(
    onPayloadsDetected: (List<BarcodePayload>) -> Unit,
    modifier: Modifier = Modifier
)

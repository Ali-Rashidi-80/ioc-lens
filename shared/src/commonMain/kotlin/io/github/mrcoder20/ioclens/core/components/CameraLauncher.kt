package io.github.mrcoder20.ioclens.core.components

import androidx.compose.runtime.Composable

interface CameraController {
    val isCameraSupported: Boolean
    val platformName: String
    fun launchCamera()
    fun launchScreenSnip()
}

@Composable
expect fun rememberCameraLauncher(
    onPayloadCaptured: (scannedText: String?, imagePath: String?) -> Unit
): CameraController

package io.github.mrcoder20.ioclens.core.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberCameraLauncher(
    onPayloadCaptured: (scannedText: String?, imagePath: String?) -> Unit
): CameraController {
    return remember {
        object : CameraController {
            override val isCameraSupported: Boolean = true
            override val platformName: String = "iOS Native Camera"
            override fun launchCamera() {}
            override fun launchScreenSnip() {}
        }
    }
}

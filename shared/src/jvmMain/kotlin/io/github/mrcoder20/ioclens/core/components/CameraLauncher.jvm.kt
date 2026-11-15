package io.github.mrcoder20.ioclens.core.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.Desktop
import java.net.URI

@Composable
actual fun rememberCameraLauncher(
    onPayloadCaptured: (scannedText: String?, imagePath: String?) -> Unit
): CameraController {
    return remember {
        object : CameraController {
            override val isCameraSupported: Boolean = true
            override val platformName: String = "Desktop Camera & Screen Vision"

            override fun launchCamera() {
                val os = System.getProperty("os.name")?.lowercase() ?: ""
                try {
                    if (os.contains("win")) {
                        // Open native Windows Camera app
                        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                            Desktop.getDesktop().browse(URI("microsoft.windows.camera:"))
                        } else {
                            Runtime.getRuntime().exec("cmd /c start microsoft.windows.camera:")
                        }
                    } else if (os.contains("mac")) {
                        Runtime.getRuntime().exec("open -a Photo\\ Booth")
                    } else {
                        // Linux / generic
                        Runtime.getRuntime().exec("cheese")
                    }
                } catch (t: Throwable) {
                    System.err.println("Launch desktop camera failed: ${t.message}")
                }
            }

            override fun launchScreenSnip() {
                val os = System.getProperty("os.name")?.lowercase() ?: ""
                try {
                    if (os.contains("win")) {
                        // Open Windows Snipping Tool / Screen Snip overlay
                        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                            Desktop.getDesktop().browse(URI("ms-screenclip:"))
                        } else {
                            Runtime.getRuntime().exec("cmd /c start ms-screenclip:")
                        }
                    } else if (os.contains("mac")) {
                        Runtime.getRuntime().exec("screencapture -i -c")
                    }
                } catch (t: Throwable) {
                    System.err.println("Launch desktop screen snip failed: ${t.message}")
                }
            }
        }
    }
}

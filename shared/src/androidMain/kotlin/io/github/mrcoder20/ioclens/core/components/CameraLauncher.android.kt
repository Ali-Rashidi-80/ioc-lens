package io.github.mrcoder20.ioclens.core.components

import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.io.FileOutputStream

@Composable
actual fun rememberCameraLauncher(
    onPayloadCaptured: (scannedText: String?, imagePath: String?) -> Unit
): CameraController {
    val context = LocalContext.current

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            try {
                val tempFile = File(context.cacheDir, "camera_capture_${System.currentTimeMillis()}.png")
                FileOutputStream(tempFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
                }
                onPayloadCaptured(null, tempFile.absolutePath)
            } catch (t: Throwable) {
                System.err.println("Failed to save camera bitmap: ${t.message}")
            }
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val tempFile = File(context.cacheDir, "picked_qr_${System.currentTimeMillis()}.png")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                onPayloadCaptured(null, tempFile.absolutePath)
            } catch (t: Throwable) {
                System.err.println("Failed to save picked image: ${t.message}")
            }
        }
    }

    val hasCamera = remember(context) {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    return remember(hasCamera) {
        object : CameraController {
            override val isCameraSupported: Boolean = hasCamera
            override val platformName: String = "Android Hardware Camera"

            override fun launchCamera() {
                try {
                    takePictureLauncher.launch(null)
                } catch (t: Throwable) {
                    System.err.println("Launch Android camera failed: ${t.message}")
                }
            }

            override fun launchScreenSnip() {
                try {
                    pickImageLauncher.launch("image/*")
                } catch (t: Throwable) {
                    System.err.println("Launch Android gallery failed: ${t.message}")
                }
            }
        }
    }
}

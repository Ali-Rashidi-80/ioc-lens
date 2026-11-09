package io.github.mrcoder20.ioclens.core.components

import androidx.compose.runtime.Composable
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
actual fun rememberFilePickerLauncher(
    onFileSelected: (filePath: String, fileName: String) -> Unit
): () -> Unit {
    return {
        try {
            val dialog = FileDialog(null as Frame?, "Select File to Hash", FileDialog.LOAD)
            dialog.isVisible = true
            val dir = dialog.directory
            val file = dialog.file
            if (dir != null && file != null) {
                val fullPath = File(dir, file).absolutePath
                onFileSelected(fullPath, file)
            }
        } catch (_: Throwable) {}
    }
}


package io.github.mrcoder20.ioclens.core.components

import androidx.compose.runtime.Composable
import io.github.mrcoder20.ioclens.logDiagnostic
import kotlin.concurrent.Volatile

interface IosFilePickerDelegate {
    fun pickFiles(allowMultiple: Boolean, onResult: (List<PickedFile>) -> Unit)
}

object IosFilePickerBridge {
    @Volatile
    var delegate: IosFilePickerDelegate? = null
}

@Composable
actual fun rememberFilePickerLauncher(
    onFileSelected: (filePath: String, fileName: String) -> Unit
): () -> Unit {
    return {
        val activeDelegate = IosFilePickerBridge.delegate
        if (activeDelegate == null) {
            logDiagnostic("IosFilePicker", "No IosFilePickerDelegate registered")
        } else {
            activeDelegate.pickFiles(allowMultiple = false) { picked ->
                val first = picked.firstOrNull()
                if (first != null) {
                    onFileSelected(first.path, first.name)
                }
            }
        }
    }
}

@Composable
actual fun rememberBatchFilePickerLauncher(
    onFilesSelected: (List<PickedFile>) -> Unit
): () -> Unit {
    return {
        val activeDelegate = IosFilePickerBridge.delegate
        if (activeDelegate == null) {
            logDiagnostic("IosFilePicker", "No IosFilePickerDelegate registered for batch selection")
        } else {
            activeDelegate.pickFiles(allowMultiple = true) { picked ->
                if (picked.isNotEmpty()) {
                    onFilesSelected(picked)
                }
            }
        }
    }
}

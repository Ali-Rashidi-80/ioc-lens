package io.github.mrcoder20.ioclens.core.components

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
actual fun rememberFilePickerLauncher(
    onFileSelected: (filePath: String, fileName: String) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        var fileName = "payload.bin"
                        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (nameIndex != -1 && cursor.moveToFirst()) {
                                fileName = cursor.getString(nameIndex) ?: "payload.bin"
                            }
                        }
                        val tempFile = File(context.cacheDir, "picked_${System.currentTimeMillis()}_$fileName")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            FileOutputStream(tempFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        withContext(Dispatchers.Main) {
                            onFileSelected(tempFile.absolutePath, fileName)
                        }
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    return {
        launcher.launch("*/*")
    }
}

@Composable
actual fun rememberBatchFilePickerLauncher(
    onFilesSelected: (List<PickedFile>) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            scope.launch {
                withContext(Dispatchers.IO) {
                    val list = mutableListOf<PickedFile>()
                    uris.forEachIndexed { idx, uri ->
                        try {
                            var fileName = "payload_$idx.bin"
                            var fileSize = 0L
                            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                                if (cursor.moveToFirst()) {
                                    if (nameIndex != -1) {
                                        fileName = cursor.getString(nameIndex) ?: fileName
                                    }
                                    if (sizeIndex != -1) {
                                        fileSize = cursor.getLong(sizeIndex)
                                    }
                                }
                            }
                            val tempFile = File(context.cacheDir, "batch_${System.currentTimeMillis()}_${idx}_$fileName")
                            context.contentResolver.openInputStream(uri)?.use { input ->
                                FileOutputStream(tempFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            list.add(
                                PickedFile(
                                    path = tempFile.absolutePath,
                                    name = fileName,
                                    sizeBytes = if (fileSize > 0L) fileSize else tempFile.length()
                                )
                            )
                        } catch (_: Exception) {
                        }
                    }
                    if (list.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            onFilesSelected(list)
                        }
                    }
                }
            }
        }
    }

    return {
        launcher.launch("*/*")
    }
}

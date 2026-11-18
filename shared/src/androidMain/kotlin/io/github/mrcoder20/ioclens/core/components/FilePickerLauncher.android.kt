package io.github.mrcoder20.ioclens.core.components

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import io.github.mrcoder20.ioclens.logDiagnostic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private const val MAX_PICKED_FILE_BYTES = 256L * 1024L * 1024L

private fun sanitizePickedName(raw: String?, fallback: String): String {
    val sanitized = raw.orEmpty()
        .replace(Regex("[^a-zA-Z0-9._-]"), "_")
        .trim('.')
        .take(100)
    return sanitized.ifBlank { fallback }
}

private fun copyUriToManagedTemp(
    context: Context,
    uri: Uri,
    prefix: String,
    fallbackName: String
): PickedFile {
    var rawName: String? = null
    var reportedSize = 0L
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (cursor.moveToFirst()) {
            if (nameIndex != -1) {
                rawName = cursor.getString(nameIndex)
            }
            if (sizeIndex != -1) {
                reportedSize = cursor.getLong(sizeIndex)
            }
        }
    }
    if (reportedSize > MAX_PICKED_FILE_BYTES) {
        throw IllegalArgumentException("Selected file exceeds $MAX_PICKED_FILE_BYTES bytes")
    }
    val displayName = sanitizePickedName(rawName ?: uri.lastPathSegment, fallbackName)
    val tempFile = File.createTempFile(prefix, ".tmp", context.cacheDir)
    var totalWritten = 0L
    try {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("ContentResolver returned null stream for picked file")
        input.use { stream ->
            FileOutputStream(tempFile).use { output ->
                val buffer = ByteArray(64 * 1024)
                var read: Int
                while (stream.read(buffer).also { read = it } != -1) {
                    totalWritten += read
                    if (totalWritten > MAX_PICKED_FILE_BYTES) {
                        throw IllegalArgumentException("Selected file exceeds $MAX_PICKED_FILE_BYTES bytes")
                    }
                    output.write(buffer, 0, read)
                }
            }
        }
        return PickedFile(
            path = tempFile.absolutePath,
            name = displayName,
            sizeBytes = if (reportedSize > 0L) reportedSize else totalWritten
        )
    } catch (t: Throwable) {
        tempFile.delete()
        throw t
    }
}

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
                try {
                    val picked = withContext(Dispatchers.IO) {
                        copyUriToManagedTemp(context, uri, prefix = "ioc_pick_", fallbackName = "payload.bin")
                    }
                    onFileSelected(picked.path, picked.name)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logDiagnostic("AndroidFilePicker", "Failed to read single picked file", e)
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
                val list = mutableListOf<PickedFile>()
                withContext(Dispatchers.IO) {
                    uris.forEachIndexed { idx, uri ->
                        try {
                            list.add(
                                copyUriToManagedTemp(
                                    context = context,
                                    uri = uri,
                                    prefix = "batch_",
                                    fallbackName = "payload_$idx.bin"
                                )
                            )
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            logDiagnostic("AndroidFilePicker", "Skipping unreadable batch file #$idx", e)
                        }
                    }
                }
                if (list.isNotEmpty()) {
                    onFilesSelected(list)
                }
            }
        }
    }

    return {
        launcher.launch("*/*")
    }
}

package io.github.mrcoder20.ioclens.core.components

import androidx.compose.runtime.Composable

/**
 * Model representing a user-selected file from platform file picker or drag-and-drop.
 */
data class PickedFile(
    val path: String,
    val name: String,
    val sizeBytes: Long = 0L
)

@Composable
expect fun rememberFilePickerLauncher(
    onFileSelected: (filePath: String, fileName: String) -> Unit
): () -> Unit

@Composable
expect fun rememberBatchFilePickerLauncher(
    onFilesSelected: (List<PickedFile>) -> Unit
): () -> Unit

package io.github.mrcoder20.ioclens.core.components

import androidx.compose.runtime.Composable

@Composable
actual fun rememberFilePickerLauncher(
    onFileSelected: (filePath: String, fileName: String) -> Unit
): () -> Unit {
    return {}
}


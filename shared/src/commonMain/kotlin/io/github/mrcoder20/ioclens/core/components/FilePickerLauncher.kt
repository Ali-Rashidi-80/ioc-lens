package io.github.mrcoder20.ioclens.core.components

import androidx.compose.runtime.Composable

@Composable
expect fun rememberFilePickerLauncher(
    onFileSelected: (filePath: String, fileName: String) -> Unit
): () -> Unit


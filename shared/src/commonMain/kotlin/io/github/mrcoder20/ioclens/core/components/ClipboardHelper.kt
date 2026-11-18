package io.github.mrcoder20.ioclens.core.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString

data class ClipboardActions(
    val copyText: (String) -> Unit,
    val readText: () -> String?
)

@Suppress("DEPRECATION")
@Composable
fun rememberClipboardActions(): ClipboardActions {
    val manager = LocalClipboardManager.current
    return ClipboardActions(
        copyText = { text -> manager.setText(AnnotatedString(text)) },
        readText = { manager.getText()?.text }
    )
}

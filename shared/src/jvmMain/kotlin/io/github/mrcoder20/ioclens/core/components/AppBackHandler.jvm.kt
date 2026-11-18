package io.github.mrcoder20.ioclens.core.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import java.awt.KeyEventDispatcher
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent

@Composable
actual fun AppBackHandler(enabled: Boolean, onBack: () -> Unit) {
    val currentOnBack by rememberUpdatedState(onBack)
    val isEnabled by rememberUpdatedState(enabled)

    DisposableEffect(Unit) {
        val dispatcher = KeyEventDispatcher { event ->
            if (isEnabled &&
                event.id == KeyEvent.KEY_PRESSED &&
                event.keyCode == KeyEvent.VK_ESCAPE
            ) {
                currentOnBack()
                true
            } else {
                false
            }
        }
        val manager = KeyboardFocusManager.getCurrentKeyboardFocusManager()
        manager.addKeyEventDispatcher(dispatcher)
        onDispose {
            manager.removeKeyEventDispatcher(dispatcher)
        }
    }
}

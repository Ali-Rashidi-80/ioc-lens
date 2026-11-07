package io.github.mrcoder20.ioclens.core.components

import androidx.compose.runtime.Composable

@Composable
actual fun AppBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Desktop does not have system back button
}

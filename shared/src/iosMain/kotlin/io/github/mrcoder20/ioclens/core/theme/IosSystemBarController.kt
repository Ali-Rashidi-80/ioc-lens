package io.github.mrcoder20.ioclens.core.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import kotlin.concurrent.Volatile

object IosSystemBarBridge {
    @Volatile
    var onAppearanceChanged: ((Boolean) -> Unit)? = null
}

@Composable
actual fun SetSystemBarIconsColor(isDark: Boolean) {
    SideEffect {
        IosSystemBarBridge.onAppearanceChanged?.invoke(isDark)
    }
}

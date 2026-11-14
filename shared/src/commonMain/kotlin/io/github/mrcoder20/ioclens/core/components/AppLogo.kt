package io.github.mrcoder20.ioclens.core.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import ioclens.shared.generated.resources.Res
import ioclens.shared.generated.resources.app_logo_mark
import org.jetbrains.compose.resources.painterResource

/**
 * Provides the official high-resolution bundled app logo painter.
 * Usable across both common shared UI and desktop window decorations.
 */
@Composable
fun rememberAppLogoPainter(): Painter {
    return painterResource(Res.drawable.app_logo_mark)
}

/**
 * Standard composable for rendering the official IOC Lens app logo.
 */
@Composable
fun AppLogoImage(
    modifier: Modifier = Modifier,
    contentDescription: String? = "IOC Lens Logo"
) {
    Image(
        painter = painterResource(Res.drawable.app_logo_mark),
        contentDescription = contentDescription,
        modifier = modifier
    )
}

package io.github.mrcoder20.ioclens.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import io.github.mrcoder20.ioclens.core.localization.AppLanguage
import io.github.mrcoder20.ioclens.core.localization.AppTheme

val LocalAccentColor = compositionLocalOf { PrimaryNeon }
val LocalRawAccentColor = compositionLocalOf { PrimaryNeon }
val LocalAppTheme = compositionLocalOf { AppTheme.DARK }

/**
 * PortX Cyber Dark Theme:
 * Midnight Navy, Electric Cyber Cyan (#00E5FF), and Vivid Cyber Purple (#A855F7).
 */
private val PortXDarkColorScheme = darkColorScheme(
    primary = PrimaryNeon,
    onPrimary = Color.Black,
    primaryContainer = SurfaceDark,
    onPrimaryContainer = TextPrimary,
    secondary = SecondaryNeon,
    onSecondary = Color.Black,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = CardDark,
    onSurfaceVariant = TextSecondary,
    outline = TextMutedLight
)

/**
 * Official IOC Lens Original Signature Theme (extracted directly from icon.svg and git history):
 * Logo Midnight Blue (#0A1226), Electric Cyan (#22D3EE), and Royal Purple (#7C5CFF).
 */
private val IocLensClassicColorScheme = darkColorScheme(
    primary = IocLensPrimary,
    onPrimary = IocLensBackground,
    primaryContainer = IocLensPrimaryVariant,
    onPrimaryContainer = IocLensTextPrimary,
    secondary = IocLensSecondary,
    onSecondary = IocLensBackground,
    background = IocLensBackground,
    onBackground = IocLensTextPrimary,
    surface = IocLensSurface,
    onSurface = IocLensTextPrimary,
    surfaceVariant = IocLensSurfaceVariant,
    onSurfaceVariant = IocLensTextSecondary,
    outline = IocLensTextMuted
)

/**
 * Clean Light Theme Palette (Linear / GitHub Slate aesthetics).
 */
private val PortXLightColorScheme = lightColorScheme(
    primary = Color(0xFF0284C7),
    secondary = Color(0xFF7C3AED),
    background = BackgroundLight,
    surface = SurfaceLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = SurfaceInsetLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderLight
)

@Composable
fun IOCLensTheme(
    appTheme: AppTheme = AppTheme.IOC_LENS_CLASSIC,
    appLanguage: AppLanguage = AppLanguage.ENGLISH,
    content: @Composable () -> Unit
) {
    val layoutDirection = if (appLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    val isSystemDark = isSystemInDarkTheme()

    val activeColorScheme = when (appTheme) {
        AppTheme.IOC_LENS_CLASSIC, AppTheme.DARK -> IocLensClassicColorScheme
        AppTheme.SYSTEM -> if (isSystemDark) IocLensClassicColorScheme else PortXLightColorScheme
    }

    val activeAccent = when (appTheme) {
        AppTheme.IOC_LENS_CLASSIC, AppTheme.DARK -> IocLensPrimary
        AppTheme.SYSTEM -> if (isSystemDark) IocLensPrimary else Color(0xFF0284C7)
    }

    CompositionLocalProvider(
        LocalAccentColor provides activeAccent,
        LocalRawAccentColor provides activeAccent,
        LocalAppTheme provides appTheme,
        LocalLayoutDirection provides layoutDirection
    ) {
        MaterialTheme(
            colorScheme = activeColorScheme,
            typography = AppTypography,
            content = content
        )
    }
}

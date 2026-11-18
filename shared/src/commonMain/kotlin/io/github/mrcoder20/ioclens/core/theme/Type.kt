package io.github.mrcoder20.ioclens.core.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.mrcoder20.ioclens.core.localization.AppLanguage
import ioclens.shared.generated.resources.Res
import ioclens.shared.generated.resources.cascadia_mono_regular
import ioclens.shared.generated.resources.noto_sans_arabic_bold
import ioclens.shared.generated.resources.noto_sans_arabic_regular
import ioclens.shared.generated.resources.noto_sans_bold
import ioclens.shared.generated.resources.noto_sans_regular
import org.jetbrains.compose.resources.Font

@Composable
fun rememberAppFontFamily(appLanguage: AppLanguage): FontFamily {
    val bodyFont = if (appLanguage == AppLanguage.PERSIAN) {
        Font(Res.font.noto_sans_arabic_regular, FontWeight.Normal)
    } else {
        Font(Res.font.noto_sans_regular, FontWeight.Normal)
    }
    val boldFont = if (appLanguage == AppLanguage.PERSIAN) {
        Font(Res.font.noto_sans_arabic_bold, FontWeight.Bold)
    } else {
        Font(Res.font.noto_sans_bold, FontWeight.Bold)
    }
    val semiBoldFont = if (appLanguage == AppLanguage.PERSIAN) {
        Font(Res.font.noto_sans_arabic_bold, FontWeight.SemiBold)
    } else {
        Font(Res.font.noto_sans_bold, FontWeight.SemiBold)
    }
    val mediumFont = if (appLanguage == AppLanguage.PERSIAN) {
        Font(Res.font.noto_sans_arabic_regular, FontWeight.Medium)
    } else {
        Font(Res.font.noto_sans_regular, FontWeight.Medium)
    }
    return remember(appLanguage) {
        FontFamily(bodyFont, mediumFont, semiBoldFont, boldFont)
    }
}

@Composable
fun rememberMonospaceFontFamily(): FontFamily {
    val monoRegular = Font(Res.font.cascadia_mono_regular, FontWeight.Normal)
    val monoBold = Font(Res.font.cascadia_mono_regular, FontWeight.Bold)
    return remember { FontFamily(monoRegular, monoBold) }
}

@Composable
fun getAppTypography(appLanguage: AppLanguage): Typography {
    val family = rememberAppFontFamily(appLanguage)
    val isRtl = appLanguage == AppLanguage.PERSIAN
    val lsWide = if (isRtl) 0.sp else 0.5.sp
    val lsMed = if (isRtl) 0.sp else 0.25.sp
    val lsSmall = if (isRtl) 0.sp else 0.15.sp
    val lsLabel = if (isRtl) 0.sp else 0.1.sp

    return remember(family, isRtl) {
        Typography(
            displayLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = 0.sp),
            displayMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 45.sp, lineHeight = 52.sp, letterSpacing = 0.sp),
            displaySmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = 0.sp),
            headlineLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = 0.sp),
            headlineMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp),
            headlineSmall = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = 0.sp),
            titleLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = lsSmall),
            titleMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = lsSmall),
            titleSmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = lsLabel),
            bodyLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp, letterSpacing = lsWide),
            bodyMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp, letterSpacing = lsMed),
            bodySmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp, letterSpacing = lsMed),
            labelLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = lsLabel),
            labelMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = lsLabel),
            labelSmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = lsLabel)
        )
    }
}

val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.25.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )
)

package com.sadam.thumbtype.mobile

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val ThumbIndigo = Color(0xFF5657F5)
val ThumbIndigoDark = Color(0xFF9EA7FF)
val ThumbMint = Color(0xFF20CFA3)
val ThumbAmber = Color(0xFFFFB84D)
val ThumbRose = Color(0xFFFF647C)
val ThumbSky = Color(0xFF4BB7FF)
val Ink = Color(0xFF151A2A)
val Night = Color(0xFF080D18)

private val LightScheme = lightColorScheme(
    primary = ThumbIndigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E9FF),
    onPrimaryContainer = Color(0xFF20246D),
    inversePrimary = ThumbIndigoDark,
    secondary = Color(0xFF087F69),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5F8ED),
    onSecondaryContainer = Color(0xFF064A3E),
    tertiary = Color(0xFF946000),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE8BC),
    onTertiaryContainer = Color(0xFF4A3200),
    background = Color(0xFFF7F8FC),
    onBackground = Ink,
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF0F2F8),
    surfaceTint = ThumbIndigo,
    onSurface = Ink,
    onSurfaceVariant = Color(0xFF626B7D),
    outline = Color(0xFFD2D7E2),
    outlineVariant = Color(0xFFE7EAF1),
    error = Color(0xFFC93350),
    onError = Color.White,
    errorContainer = Color(0xFFFFE5EA),
    onErrorContainer = Color(0xFF6E1026),
    inverseSurface = Color(0xFF252A39),
    inverseOnSurface = Color(0xFFF4F5FA),
    scrim = Color(0xFF000000)
)

private val DarkScheme = darkColorScheme(
    primary = ThumbIndigoDark,
    onPrimary = Color(0xFF15194D),
    primaryContainer = Color(0xFF2C347C),
    onPrimaryContainer = Color(0xFFE9EAFF),
    inversePrimary = ThumbIndigo,
    secondary = Color(0xFF6EE7C7),
    onSecondary = Color(0xFF04392E),
    secondaryContainer = Color(0xFF0B4E40),
    onSecondaryContainer = Color(0xFFD5FFF2),
    tertiary = Color(0xFFFFCA73),
    onTertiary = Color(0xFF4B3200),
    tertiaryContainer = Color(0xFF5B3C00),
    onTertiaryContainer = Color(0xFFFFE7B7),
    background = Night,
    onBackground = Color(0xFFF5F7FC),
    surface = Color(0xFF111827),
    surfaceVariant = Color(0xFF1A2334),
    surfaceTint = ThumbIndigoDark,
    onSurface = Color(0xFFF6F7FB),
    onSurfaceVariant = Color(0xFFB2BAC9),
    outline = Color(0xFF414B5F),
    outlineVariant = Color(0xFF293347),
    error = Color(0xFFFF8DA1),
    onError = Color(0xFF5A1021),
    errorContainer = Color(0xFF612131),
    onErrorContainer = Color(0xFFFFD9E0),
    inverseSurface = Color(0xFFE7E9F0),
    inverseOnSurface = Color(0xFF202633),
    scrim = Color.Black
)

private val ThumbTypeShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

private fun typography(scale: Float): Typography {
    val family = FontFamily.SansSerif
    return Typography(
        displayLarge = TextStyle(family, FontWeight.Black, (48 * scale).sp, lineHeight = (52 * scale).sp, letterSpacing = (-1.1 * scale).sp),
        displayMedium = TextStyle(family, FontWeight.Black, (40 * scale).sp, lineHeight = (45 * scale).sp, letterSpacing = (-0.8 * scale).sp),
        headlineLarge = TextStyle(family, FontWeight.Black, (31 * scale).sp, lineHeight = (36 * scale).sp, letterSpacing = (-0.45 * scale).sp),
        headlineMedium = TextStyle(family, FontWeight.ExtraBold, (25 * scale).sp, lineHeight = (30 * scale).sp, letterSpacing = (-0.25 * scale).sp),
        headlineSmall = TextStyle(family, FontWeight.ExtraBold, (22 * scale).sp, lineHeight = (27 * scale).sp),
        titleLarge = TextStyle(family, FontWeight.Bold, (20 * scale).sp, lineHeight = (26 * scale).sp),
        titleMedium = TextStyle(family, FontWeight.Bold, (17 * scale).sp, lineHeight = (23 * scale).sp),
        titleSmall = TextStyle(family, FontWeight.SemiBold, (15 * scale).sp, lineHeight = (20 * scale).sp),
        bodyLarge = TextStyle(family, FontWeight.Normal, (16 * scale).sp, lineHeight = (24 * scale).sp),
        bodyMedium = TextStyle(family, FontWeight.Normal, (14 * scale).sp, lineHeight = (21 * scale).sp),
        bodySmall = TextStyle(family, FontWeight.Normal, (12 * scale).sp, lineHeight = (18 * scale).sp),
        labelLarge = TextStyle(family, FontWeight.Bold, (14 * scale).sp, lineHeight = (19 * scale).sp),
        labelMedium = TextStyle(family, FontWeight.SemiBold, (12 * scale).sp, lineHeight = (17 * scale).sp),
        labelSmall = TextStyle(family, FontWeight.SemiBold, (10 * scale).sp, lineHeight = (14 * scale).sp, letterSpacing = (0.2 * scale).sp)
    )
}

@Composable
fun ThumbTypeTheme(settings: AppSettings, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalThumbTypeSpacing provides ThumbTypeSpacing(),
        LocalThumbTypeMotion provides ThumbTypeMotion(reduced = settings.reducedMotion)
    ) {
        MaterialTheme(
            colorScheme = if (settings.darkMode) DarkScheme else LightScheme,
            typography = typography(if (settings.largeText) 1.10f else 1f),
            shapes = ThumbTypeShapes,
            content = content
        )
    }
}

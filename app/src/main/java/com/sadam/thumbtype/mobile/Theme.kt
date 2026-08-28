package com.sadam.thumbtype.mobile

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val ThumbIndigo = Color(0xFF5865F2)
val ThumbIndigoDark = Color(0xFF8E98FF)
val ThumbMint = Color(0xFF2DD4A8)
val ThumbAmber = Color(0xFFFFB84D)
val ThumbRose = Color(0xFFFF6B86)
val Ink = Color(0xFF151B2B)
val Night = Color(0xFF090E1A)

private val LightScheme = lightColorScheme(
    primary = ThumbIndigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9EBFF),
    onPrimaryContainer = Color(0xFF1D276F),
    secondary = Color(0xFF078B70),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD8F8EE),
    onSecondaryContainer = Color(0xFF064D3F),
    tertiary = Color(0xFF9C6400),
    tertiaryContainer = Color(0xFFFFE6B8),
    background = Color(0xFFF7F8FC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF0F2F7),
    onSurface = Ink,
    onSurfaceVariant = Color(0xFF667085),
    outline = Color(0xFFD5D9E3),
    outlineVariant = Color(0xFFE7E9EF),
    error = Color(0xFFCB3452),
    errorContainer = Color(0xFFFFE7EC)
)

private val DarkScheme = darkColorScheme(
    primary = ThumbIndigoDark,
    onPrimary = Color(0xFF11184A),
    primaryContainer = Color(0xFF29317A),
    onPrimaryContainer = Color(0xFFE8E9FF),
    secondary = Color(0xFF6EE7C7),
    onSecondary = Color(0xFF05392F),
    secondaryContainer = Color(0xFF0D4F42),
    onSecondaryContainer = Color(0xFFD6FFF2),
    tertiary = Color(0xFFFFC970),
    tertiaryContainer = Color(0xFF5A3B00),
    background = Night,
    surface = Color(0xFF111827),
    surfaceVariant = Color(0xFF192234),
    onSurface = Color(0xFFF5F7FC),
    onSurfaceVariant = Color(0xFFAAB3C4),
    outline = Color(0xFF3C465A),
    outlineVariant = Color(0xFF273246),
    error = Color(0xFFFF8EA3),
    errorContainer = Color(0xFF5F2130)
)

private fun typography(scale: Float): Typography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black, fontSize = (48 * scale).sp, lineHeight = (52 * scale).sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black, fontSize = (32 * scale).sp, lineHeight = (37 * scale).sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = (26 * scale).sp, lineHeight = (31 * scale).sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = (21 * scale).sp, lineHeight = (27 * scale).sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = (17 * scale).sp, lineHeight = (23 * scale).sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = (16 * scale).sp, lineHeight = (24 * scale).sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = (14 * scale).sp, lineHeight = (21 * scale).sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = (14 * scale).sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = (12 * scale).sp)
)

@Composable
fun ThumbTypeTheme(settings: AppSettings, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (settings.darkMode) DarkScheme else LightScheme,
        typography = typography(if (settings.largeText) 1.10f else 1f),
        content = content
    )
}

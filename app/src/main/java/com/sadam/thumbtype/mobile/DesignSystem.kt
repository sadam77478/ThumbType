package com.sadam.thumbtype.mobile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * ThumbType's layout and motion tokens.
 *
 * Keeping these values centralized prevents each screen from inventing its own spacing,
 * corner radius and interaction sizing. The design system intentionally uses a compact set
 * of tokens so the app stays visually consistent while remaining easy to maintain.
 */
@Immutable
data class ThumbTypeSpacing(
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 20.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val xxxl: Dp = 40.dp
)

@Immutable
data class ThumbTypeMotion(
    val reduced: Boolean = false,
    val quickMs: Int = 140,
    val standardMs: Int = 220,
    val expressiveMs: Int = 360
) {
    fun duration(preferredMs: Int): Int = if (reduced) 0 else preferredMs
}

internal val LocalThumbTypeSpacing = staticCompositionLocalOf { ThumbTypeSpacing() }
internal val LocalThumbTypeMotion = staticCompositionLocalOf { ThumbTypeMotion() }

object ThumbTypeDesign {
    val spacing: ThumbTypeSpacing
        @Composable get() = LocalThumbTypeSpacing.current

    val motion: ThumbTypeMotion
        @Composable get() = LocalThumbTypeMotion.current
}

object ThumbTypeSizes {
    val minimumTouchTarget = 48.dp
    val compactIcon = 18.dp
    val standardIcon = 22.dp
    val heroIcon = 48.dp
    val cardElevation = 2.dp
}

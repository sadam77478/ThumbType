package com.sadam.thumbtype.mobile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sadam.thumbtype.mobile.app.navigation.ThumbTypeNavigation

enum class ThumbTypeWidthClass { COMPACT, MEDIUM, EXPANDED }

@Immutable
data class ThumbTypeWindowInfo(
    val widthDp: Int,
    val heightDp: Int,
    val widthClass: ThumbTypeWidthClass
) {
    val contentMaxWidth: Dp
        get() = when (widthClass) {
            ThumbTypeWidthClass.COMPACT -> 600.dp
            ThumbTypeWidthClass.MEDIUM -> 760.dp
            ThumbTypeWidthClass.EXPANDED -> 920.dp
        }

    val trainerMaxWidth: Dp
        get() = when (widthClass) {
            ThumbTypeWidthClass.COMPACT -> 600.dp
            ThumbTypeWidthClass.MEDIUM -> 720.dp
            ThumbTypeWidthClass.EXPANDED -> 780.dp
        }
}

fun classifyThumbTypeWidth(widthDp: Int): ThumbTypeWidthClass = when {
    widthDp < 600 -> ThumbTypeWidthClass.COMPACT
    widthDp < 840 -> ThumbTypeWidthClass.MEDIUM
    else -> ThumbTypeWidthClass.EXPANDED
}

@Composable
fun rememberThumbTypeWindowInfo(): ThumbTypeWindowInfo {
    val configuration = LocalConfiguration.current
    return ThumbTypeWindowInfo(
        widthDp = configuration.screenWidthDp,
        heightDp = configuration.screenHeightDp,
        widthClass = classifyThumbTypeWidth(configuration.screenWidthDp)
    )
}

/** Centers phone-first screens and prevents stretched tablet/foldable layouts. */
@Composable
fun ThumbTypeAdaptiveContainer(
    modifier: Modifier = Modifier,
    maxContentWidth: Dp = rememberThumbTypeWindowInfo().contentMaxWidth,
    content: @Composable () -> Unit
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .widthIn(max = maxContentWidth)
        ) {
            content()
        }
    }
}

/**
 * Responsive app chrome: bottom navigation on phones and a navigation rail on expanded
 * tablet/foldable widths. Screen content remains centered and width-bounded.
 */
@Composable
fun ThumbTypeAppShell(
    currentScreen: AppScreen,
    snackbarHostState: SnackbarHostState,
    onNavigate: (AppScreen) -> Unit,
    content: @Composable () -> Unit
) {
    val window = rememberThumbTypeWindowInfo()

    if (window.widthClass == ThumbTypeWidthClass.EXPANDED) {
        Row(Modifier.fillMaxSize()) {
            NavigationRail(
                modifier = Modifier.fillMaxHeight().navigationBarsPadding(),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Spacer(Modifier.height(12.dp))
                ThumbTypeNavigation.bottomDestinations.forEach { destination ->
                    val meta = destinationMeta(destination)
                    NavigationRailItem(
                        selected = currentScreen == destination,
                        onClick = { onNavigate(destination) },
                        icon = { Icon(meta.first, contentDescription = meta.second) },
                        label = { Text(meta.second, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Scaffold(
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { padding ->
                ThumbTypeAdaptiveContainer(Modifier.padding(padding), window.contentMaxWidth, content)
            }
        }
    } else {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    ThumbTypeNavigation.bottomDestinations.forEach { destination ->
                        val meta = destinationMeta(destination)
                        NavigationBarItem(
                            selected = currentScreen == destination,
                            onClick = { onNavigate(destination) },
                            icon = { Icon(meta.first, contentDescription = meta.second) },
                            label = { Text(meta.second, style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        ) { padding ->
            ThumbTypeAdaptiveContainer(Modifier.padding(padding), window.contentMaxWidth, content)
        }
    }
}

private fun destinationMeta(destination: AppScreen): Pair<ImageVector, String> = when (destination) {
    AppScreen.Home -> Icons.Default.Home to "Home"
    AppScreen.Learn -> Icons.Default.School to "Learn"
    AppScreen.Practice -> Icons.Default.Bolt to "Practice"
    AppScreen.Progress -> Icons.Default.AutoGraph to "Progress"
    AppScreen.Profile -> Icons.Default.Person to "Profile"
    else -> Icons.Default.Home to destination.name
}

/** Adds an explicit TalkBack/semantic activation path to custom pointer-driven controls. */
fun Modifier.thumbTypeAccessibleAction(
    label: String,
    stateText: String? = null,
    onActivate: () -> Unit
): Modifier = semantics {
    contentDescription = label
    role = Role.Button
    if (!stateText.isNullOrBlank()) stateDescription = stateText
    onClick(label = label) {
        onActivate()
        true
    }
}

/** Makes compact visual metrics read as one meaningful accessibility phrase. */
fun Modifier.thumbTypeReadout(label: String, value: String): Modifier = semantics(mergeDescendants = true) {
    contentDescription = "$label, $value"
}

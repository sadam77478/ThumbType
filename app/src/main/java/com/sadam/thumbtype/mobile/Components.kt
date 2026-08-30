package com.sadam.thumbtype.mobile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppPageHeader(
    title: String,
    subtitle: String? = null,
    trailing: @Composable () -> Unit = {}
) {
    val spacing = ThumbTypeDesign.spacing
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineLarge)
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(spacing.xs))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(spacing.sm))
        trailing()
    }
}

@Composable
fun PremiumCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val clickableModifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier
    Surface(
        modifier = clickableModifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = ThumbTypeSizes.cardElevation,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .78f))
    ) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun GradientHero(
    eyebrow: String,
    title: String,
    subtitle: String,
    trailing: @Composable BoxScope.() -> Unit = {}
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val shape = MaterialTheme.shapes.extraLarge
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        primary,
                        primary.copy(alpha = .94f),
                        secondary.copy(alpha = .90f)
                    ),
                    start = Offset.Zero,
                    end = Offset.Infinite
                )
            )
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(
                color = Color.White.copy(alpha = .07f),
                radius = size.minDimension * .42f,
                center = Offset(size.width * .92f, size.height * .12f)
            )
            drawCircle(
                color = Color.White.copy(alpha = .045f),
                radius = size.minDimension * .30f,
                center = Offset(size.width * .72f, size.height * .98f)
            )
        }
        Column(Modifier.fillMaxWidth(.76f).padding(24.dp)) {
            Text(
                eyebrow.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = Color.White.copy(.82f),
                letterSpacing = 1.15.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Spacer(Modifier.height(7.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(.88f))
        }
        trailing()
    }
}

@Composable
fun ScoreRing(score: Int, modifier: Modifier = Modifier, label: String = "ThumbScore") {
    val safe = score.coerceIn(0, 1000)
    val progress = safe / 1000f
    val track = MaterialTheme.colorScheme.onSurface.copy(.09f)
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    Box(modifier.size(142.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            val pad = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(pad, pad),
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            if (progress > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(primary, secondary, primary)),
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = Offset(pad, pad),
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(safe.toString(), style = MaterialTheme.typography.headlineLarge)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MetricCard(
    value: String,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .72f))
    ) {
        Column(Modifier.padding(15.dp)) {
            Surface(shape = MaterialTheme.shapes.small, color = accent.copy(.11f)) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp).size(ThumbTypeSizes.compactIcon),
                    tint = accent
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(1.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SectionHeading(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (action != null && onAction != null) {
            TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                Text(action, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun ActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color = MaterialTheme.colorScheme.primary,
    badge: String? = null,
    onClick: () -> Unit
) {
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .72f))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = MaterialTheme.shapes.medium, color = accent.copy(.11f)) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.padding(11.dp).size(ThumbTypeSizes.standardIcon),
                    tint = accent
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (!badge.isNullOrBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Surface(shape = CircleShape, color = accent.copy(.12f)) {
                            Text(
                                badge,
                                Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = accent
                            )
                        }
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun TinyPill(text: String, icon: ImageVector? = null, accent: Color = MaterialTheme.colorScheme.primary) {
    Surface(
        shape = CircleShape,
        color = accent.copy(.10f),
        border = BorderStroke(1.dp, accent.copy(.13f))
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, Modifier.size(14.dp), tint = accent)
                Spacer(Modifier.width(5.dp))
            }
            Text(text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = accent)
        }
    }
}

@Composable
fun ProgressChart(values: List<Int>, modifier: Modifier = Modifier) {
    val line = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val point = MaterialTheme.colorScheme.secondary
    Canvas(modifier.fillMaxWidth().height(165.dp)) {
        val usable = if (values.size < 2) {
            listOf(values.firstOrNull() ?: 0, values.firstOrNull() ?: 0)
        } else values
        val max = (usable.maxOrNull() ?: 1).coerceAtLeast(1).toFloat()
        val min = (usable.minOrNull() ?: 0).toFloat()
        val range = (max - min).coerceAtLeast(1f)

        repeat(4) { index ->
            val y = size.height * index / 3f
            drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }

        usable.zipWithNext().forEachIndexed { index, (a, b) ->
            val denominator = (usable.size - 1).coerceAtLeast(1)
            val x1 = size.width * index / denominator
            val x2 = size.width * (index + 1) / denominator
            val y1 = size.height - (a - min) / range * size.height
            val y2 = size.height - (b - min) / range * size.height
            drawLine(line, Offset(x1, y1), Offset(x2, y2), strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
            drawCircle(line, 3.dp.toPx(), Offset(x1, y1))
            if (index == usable.size - 2) {
                drawCircle(MaterialTheme.colorScheme.surface, 8.dp.toPx(), Offset(x2, y2))
                drawCircle(point, 5.dp.toPx(), Offset(x2, y2))
            }
        }
    }
}

@Composable
fun KeyboardHeatmap(stats: Map<Char, KeyAggregate>, onKeySelected: (Char) -> Unit) {
    val worst = stats.values.maxOfOrNull { it.errorRate * 12 + it.averageReactionMs }?.coerceAtLeast(1) ?: 1
    PremiumCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Keyboard heatmap", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Higher-intensity keys need more attention. Tap a key for details.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TinyPill("Live", Icons.Default.AutoGraph, MaterialTheme.colorScheme.secondary)
        }
        Spacer(Modifier.height(16.dp))
        listOf("qwertyuiop", "asdfghjkl", "zxcvbnm").forEachIndexed { rowIndex, row ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = (rowIndex * 10).dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                row.forEach { char ->
                    val stat = stats[char] ?: KeyAggregate()
                    val load = ((stat.errorRate * 12 + stat.averageReactionMs).toFloat() / worst).coerceIn(0f, 1f)
                    val accent = if (load > .62f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                    Surface(
                        Modifier.weight(1f).height(40.dp).clickable { onKeySelected(char) },
                        shape = MaterialTheme.shapes.small,
                        color = accent.copy(alpha = .055f + .38f * load),
                        border = BorderStroke(1.dp, accent.copy(alpha = .08f + .18f * load))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(char.uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            if (rowIndex != 2) Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
fun KeyDetailDialog(char: Char, stat: KeyAggregate, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Keyboard, contentDescription = null) },
        title = { Text("Key ${char.uppercase()}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailLine("Attempts", stat.attempts.toString())
                DetailLine("Correct", stat.correct.toString())
                DetailLine("Errors", stat.errors.toString())
                DetailLine("Accuracy", "${stat.accuracy}%")
                DetailLine("Average reaction", if (stat.averageReactionMs == 0) "Not enough data" else "${stat.averageReactionMs} ms")
                DetailLine("Natural zone", TrainingEngine.fixedZone(char).name.lowercase().replaceFirstChar { it.uppercase() })
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
    }
}

@Composable
fun SettingSwitchRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .72f))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primary.copy(.10f)) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.padding(9.dp).size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onChecked)
        }
    }
}

@Composable
fun AchievementChip(title: String, unlocked: Boolean, modifier: Modifier = Modifier) {
    val accent = if (unlocked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
    val alpha = if (unlocked) .10f else .055f
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = accent.copy(alpha = alpha),
        border = BorderStroke(1.dp, accent.copy(alpha = if (unlocked) .16f else .10f))
    ) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(if (unlocked) Icons.Default.EmojiEvents else Icons.Default.Lock, contentDescription = null, tint = accent)
            Spacer(Modifier.height(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = if (unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PercentBar(label: String, value: Int, accent: Color = MaterialTheme.colorScheme.primary) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${value.coerceIn(0, 100)}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { value.coerceIn(0, 100) / 100f },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
            color = accent,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

/** Ready-to-use premium card for surfacing the V4 deterministic coaching profile. */
@Composable
fun AdaptiveCoachCard(intelligence: TrainingIntelligenceProfile, modifier: Modifier = Modifier) {
    val accent = when (intelligence.priority) {
        TrainingPriority.ACCURACY -> MaterialTheme.colorScheme.error
        TrainingPriority.SPEED -> ThumbAmber
        TrainingPriority.TRANSITIONS -> ThumbSky
        TrainingPriority.CENTER_REACH -> MaterialTheme.colorScheme.secondary
        TrainingPriority.LEFT_ZONE, TrainingPriority.RIGHT_ZONE -> MaterialTheme.colorScheme.primary
        TrainingPriority.RHYTHM -> MaterialTheme.colorScheme.tertiary
        TrainingPriority.ENDURANCE -> ThumbMint
        TrainingPriority.FOUNDATION -> MaterialTheme.colorScheme.primary
    }
    PremiumCard(modifier = modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = MaterialTheme.shapes.medium, color = accent.copy(.11f)) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, Modifier.padding(11.dp), tint = accent)
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text("Adaptive coach", style = MaterialTheme.typography.labelMedium, color = accent)
                Text(intelligence.headline, style = MaterialTheme.typography.titleMedium)
            }
            TinyPill("L${intelligence.difficultyLevel}", Icons.Default.Tune, accent)
        }
        Spacer(Modifier.height(12.dp))
        Text(intelligence.explanation, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TinyPill("${intelligence.recentWpm} WPM", Icons.Default.Speed, accent)
            TinyPill("${intelligence.recentAccuracy}%", Icons.Default.GpsFixed, MaterialTheme.colorScheme.secondary)
            if (intelligence.trend.plateauDetected) TinyPill("Plateau", Icons.Default.AutoGraph, ThumbAmber)
        }
    }
}

package com.sadam.thumbtype.mobile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineLarge)
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(12.dp))
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
        clickableModifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
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
    Box(
        Modifier.fillMaxWidth()
            .background(
                Brush.linearGradient(listOf(primary, primary.copy(.90f), secondary.copy(.82f))),
                RoundedCornerShape(28.dp)
            )
            .padding(22.dp)
    ) {
        Column(Modifier.fillMaxWidth(.76f)) {
            Text(eyebrow.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White.copy(.82f), letterSpacing = 1.1.sp)
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(.86f))
        }
        trailing()
    }
}

@Composable
fun ScoreRing(score: Int, modifier: Modifier = Modifier, label: String = "ThumbScore") {
    val safe = score.coerceIn(0, 1000)
    val progress = safe / 1000f
    val track = MaterialTheme.colorScheme.onSurface.copy(.10f)
    val accent = MaterialTheme.colorScheme.primary
    Box(modifier.size(138.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            val pad = stroke / 2f
            drawArc(track, -90f, 360f, false, Offset(pad, pad), Size(size.width - stroke, size.height - stroke), style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(accent, -90f, 360f * progress, false, Offset(pad, pad), Size(size.width - stroke, size.height - stroke), style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(safe.toString(), fontSize = 30.sp, fontWeight = FontWeight.Black)
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    Surface(modifier, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(15.dp)) {
            Surface(shape = RoundedCornerShape(12.dp), color = accent.copy(.10f)) {
                Icon(icon, null, Modifier.padding(8.dp).size(18.dp), tint = accent)
            }
            Spacer(Modifier.height(12.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SectionHeading(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (action != null && onAction != null) TextButton(onClick = onAction) { Text(action) }
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
    Surface(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(14.dp), color = accent.copy(.11f)) {
                Icon(icon, null, Modifier.padding(11.dp).size(22.dp), tint = accent)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    if (!badge.isNullOrBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Surface(shape = CircleShape, color = accent.copy(.12f)) {
                            Text(badge, Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 9.sp, fontWeight = FontWeight.Black, color = accent)
                        }
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun TinyPill(text: String, icon: ImageVector? = null, accent: Color = MaterialTheme.colorScheme.primary) {
    Surface(shape = CircleShape, color = accent.copy(.10f)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, Modifier.size(14.dp), tint = accent)
                Spacer(Modifier.width(5.dp))
            }
            Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accent)
        }
    }
}

@Composable
fun ProgressChart(values: List<Int>, modifier: Modifier = Modifier) {
    val line = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val point = MaterialTheme.colorScheme.secondary
    Canvas(modifier.fillMaxWidth().height(165.dp)) {
        val usable = if (values.size < 2) listOf(values.firstOrNull() ?: 0, values.firstOrNull() ?: 0) else values
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
            if (index == usable.size - 2) drawCircle(point, 5.dp.toPx(), Offset(x2, y2))
        }
    }
}

@Composable
fun KeyboardHeatmap(stats: Map<Char, KeyAggregate>, onKeySelected: (Char) -> Unit) {
    val worst = stats.values.maxOfOrNull { it.errorRate * 12 + it.averageReactionMs }?.coerceAtLeast(1) ?: 1
    PremiumCard {
        Text("Keyboard heatmap", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text("Darker keys need more attention. Tap any key for details.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        listOf("qwertyuiop", "asdfghjkl", "zxcvbnm").forEachIndexed { rowIndex, row ->
            Row(Modifier.fillMaxWidth().padding(horizontal = (rowIndex * 10).dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                row.forEach { char ->
                    val stat = stats[char] ?: KeyAggregate()
                    val load = (stat.errorRate * 12 + stat.averageReactionMs).toFloat() / worst
                    Surface(
                        Modifier.weight(1f).height(38.dp).clickable { onKeySelected(char) },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = .06f + .48f * load.coerceIn(0f, 1f))
                    ) {
                        Box(contentAlignment = Alignment.Center) { Text(char.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
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
        icon = { Icon(Icons.Default.Keyboard, null) },
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
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
    }
}

@Composable
fun SettingSwitchRow(title: String, subtitle: String, icon: ImageVector, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked, onCheckedChange = onChecked)
        }
    }
}

@Composable
fun AchievementChip(title: String, unlocked: Boolean, modifier: Modifier = Modifier) {
    val accent = if (unlocked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
    val alpha = if (unlocked) .10f else .06f
    Surface(modifier, shape = RoundedCornerShape(18.dp), color = accent.copy(alpha = alpha)) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(if (unlocked) Icons.Default.EmojiEvents else Icons.Default.Lock, null, tint = accent)
            Spacer(Modifier.height(8.dp))
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = if (unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun PercentBar(label: String, value: Int, accent: Color = MaterialTheme.colorScheme.primary) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${value.coerceIn(0, 100)}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { value.coerceIn(0, 100) / 100f },
            modifier = Modifier.fillMaxWidth().height(7.dp),
            color = accent,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

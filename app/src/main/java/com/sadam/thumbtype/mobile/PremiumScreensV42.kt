package com.sadam.thumbtype.mobile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sadam.thumbtype.mobile.app.HomeUiData
import com.sadam.thumbtype.mobile.app.LearnUiData
import com.sadam.thumbtype.mobile.app.PracticeUiData
import com.sadam.thumbtype.mobile.app.ProfileUiData
import com.sadam.thumbtype.mobile.app.ProgressUiData

private enum class PremiumChartMetric { WPM, ACCURACY, SCORE }

@Composable
fun PremiumOnboardingScreen(
    initialProfile: UserProfile,
    onSaveProfile: (UserProfile) -> Unit,
    onBaseline: () -> Unit,
    onSkip: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var targetWpm by remember { mutableIntStateOf(initialProfile.targetWpm) }
    var targetAccuracy by remember { mutableIntStateOf(initialProfile.targetAccuracy) }
    var dailyMinutes by remember { mutableIntStateOf(initialProfile.dailyGoalMinutes) }
    var focus by remember { mutableStateOf(initialProfile.focus) }
    val spacing = ThumbTypeDesign.spacing
    val total = 5

    Column(
        Modifier.fillMaxSize().padding(horizontal = spacing.lg, vertical = spacing.md)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(.10f)) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PhoneAndroid, contentDescription = null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(7.dp))
                    Text("ThumbType", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.weight(1f))
            TinyPill("${step + 1} / $total")
        }
        Spacer(Modifier.height(spacing.md))
        LinearProgressIndicator(
            progress = { (step + 1) / total.toFloat() },
            modifier = Modifier.fillMaxWidth().height(7.dp),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(Modifier.height(spacing.xl))

        Box(Modifier.weight(1f)) {
            when (step) {
                0 -> OnboardingWelcome()
                1 -> OnboardingGoals(targetWpm, targetAccuracy, { targetWpm = it }, { targetAccuracy = it })
                2 -> OnboardingFocus(focus) { focus = it }
                3 -> OnboardingDailyGoal(dailyMinutes) { dailyMinutes = it }
                else -> OnboardingReady(targetWpm, targetAccuracy, dailyMinutes, focus)
            }
        }

        Spacer(Modifier.height(spacing.md))
        if (step < total - 1) {
            Button(
                onClick = { step++ },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(if (step == 0) "Build my training plan" else "Continue", fontWeight = FontWeight.Bold)
            }
            if (step > 0) {
                TextButton(onClick = { step-- }, modifier = Modifier.fillMaxWidth()) { Text("Back") }
            }
        } else {
            Button(
                onClick = {
                    onSaveProfile(UserProfile(targetWpm, targetAccuracy, dailyMinutes, focus))
                    onBaseline()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Timer, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Take baseline test", fontWeight = FontWeight.Bold)
            }
            TextButton(
                onClick = {
                    onSaveProfile(UserProfile(targetWpm, targetAccuracy, dailyMinutes, focus))
                    onSkip()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Skip test for now") }
        }
    }
}

@Composable
private fun OnboardingWelcome() {
    Column {
        GradientHero(
            eyebrow = "Mobile-first training",
            title = "Build elite two-thumb control",
            subtitle = "Train reach, rhythm, precision and real phone typing with a keyboard designed for practice."
        ) {
            Icon(
                Icons.Default.TouchApp,
                contentDescription = null,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 22.dp).size(56.dp),
                tint = Color.White.copy(.92f)
            )
        }
        Spacer(Modifier.height(24.dp))
        listOf(
            Triple(Icons.Default.AutoGraph, "Adaptive coaching", "Your weak keys and transitions shape the next workout."),
            Triple(Icons.Default.TouchApp, "Screen-zone reach", "Center keys stay adaptive instead of forcing one rigid side."),
            Triple(Icons.Default.Security, "Privacy-first", "Core training and analytics stay local in this build.")
        ).forEach { (icon, title, subtitle) ->
            ActionRow(title, subtitle, icon, onClick = {})
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun OnboardingGoals(wpm: Int, accuracy: Int, onWpm: (Int) -> Unit, onAccuracy: (Int) -> Unit) {
    Column {
        AppPageHeader("Set your target", "ThumbType uses these goals to personalize difficulty and coaching.")
        Spacer(Modifier.height(24.dp))
        PremiumCard {
            Text("Target speed", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            ChoiceChipRows(listOf(30, 40, 50, 60), wpm, { "$it WPM" }, onWpm)
            Spacer(Modifier.height(22.dp))
            Text("Target accuracy", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            ChoiceChipRows(listOf(95, 97, 98, 99), accuracy, { "$it%" }, onAccuracy)
        }
    }
}

@Composable
private fun OnboardingFocus(focus: TrainingFocus, onFocus: (TrainingFocus) -> Unit) {
    Column {
        AppPageHeader("Choose your focus", "This changes training emphasis without locking you out of other practice modes.")
        Spacer(Modifier.height(20.dp))
        val options = listOf(
            Triple(TrainingFocus.BALANCED, "Balanced", "Speed + accuracy + rhythm") to Icons.Default.Tune,
            Triple(TrainingFocus.SPEED, "Speed", "Reduce hesitation safely") to Icons.Default.Bolt,
            Triple(TrainingFocus.ACCURACY, "Accuracy", "Precision before pace") to Icons.Default.GpsFixed,
            Triple(TrainingFocus.RHYTHM, "Rhythm", "Even key-to-key timing") to Icons.Default.GraphicEq
        )
        options.forEach { (meta, icon) ->
            val selected = focus == meta.first
            Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable { onFocus(meta.first) },
                shape = MaterialTheme.shapes.large,
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary.copy(.25f) else MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(meta.second, style = MaterialTheme.typography.titleSmall)
                        Text(meta.third, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (selected) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun OnboardingDailyGoal(minutes: Int, onMinutes: (Int) -> Unit) {
    Column {
        AppPageHeader("Set a sustainable routine", "Short daily practice beats occasional marathon sessions.")
        Spacer(Modifier.height(20.dp))
        listOf(5, 10, 15, 20).forEach { value ->
            val selected = minutes == value
            Surface(
                Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onMinutes(value) },
                shape = MaterialTheme.shapes.large,
                color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.secondary.copy(.25f) else MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(value.toString(), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(8.dp))
                    Text("minutes / day", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.weight(1f))
                    if (selected) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
private fun OnboardingReady(wpm: Int, accuracy: Int, minutes: Int, focus: TrainingFocus) {
    Column {
        GradientHero(
            eyebrow = "Plan ready",
            title = "Start with a clean baseline",
            subtitle = "One short test gives ThumbType a useful starting point for personalized coaching."
        )
        Spacer(Modifier.height(20.dp))
        PremiumCard {
            DetailLine("Speed goal", "$wpm WPM")
            DetailLine("Accuracy goal", "$accuracy%")
            DetailLine("Daily training", "$minutes min")
            DetailLine("Primary focus", focus.name.lowercase().replaceFirstChar { it.uppercase() })
        }
        Spacer(Modifier.height(14.dp))
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.secondaryContainer) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(12.dp))
                Text("The baseline uses ThumbType's own training keyboard. Your personal messages are not required.", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ChoiceChipRows(values: List<Int>, selected: Int, label: (Int) -> String, onSelected: (Int) -> Unit) {
    values.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { value ->
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelected(value) },
                    label = { Text(label(value), fontWeight = FontWeight.Bold) },
                    leadingIcon = if (selected == value) ({ Icon(Icons.Default.Check, contentDescription = null, Modifier.size(16.dp)) }) else null,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
fun PremiumLearnScreen(data: LearnUiData, onStart: (Lesson) -> Unit) {
    val completed = data.completedLessonIds
    val firstUnlocked = LessonRepository.lessons.firstOrNull { it.id !in completed }?.id ?: Int.MAX_VALUE
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AppPageHeader("Learning path", "A structured progression from two-thumb foundations to advanced mobile speed.")
        }
        item {
            PremiumCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(.10f)) {
                        Icon(Icons.Default.School, contentDescription = null, Modifier.padding(12.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Curriculum progress", style = MaterialTheme.typography.titleMedium)
                        Text("${completed.size} of ${LessonRepository.lessons.size} lessons completed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TinyPill("${(completed.size * 100 / LessonRepository.lessons.size.coerceAtLeast(1))}%")
                }
                Spacer(Modifier.height(14.dp))
                LinearProgressIndicator(
                    progress = { completed.size.toFloat() / LessonRepository.lessons.size.coerceAtLeast(1) },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
        LessonRepository.lessons.groupBy { it.stage }.forEach { (stage, lessons) ->
            item {
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Stage $stage", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(LessonRepository.stageNames[stage] ?: "Training", style = MaterialTheme.typography.titleLarge)
                    }
                    TinyPill("${lessons.count { it.id in completed }}/${lessons.size}")
                }
            }
            items(lessons) { lesson ->
                val done = lesson.id in completed
                val unlocked = lesson.id <= firstUnlocked
                Surface(
                    Modifier.fillMaxWidth().clickable(enabled = unlocked) { onStart(lesson) },
                    shape = MaterialTheme.shapes.large,
                    color = when {
                        done -> MaterialTheme.colorScheme.secondary.copy(.055f)
                        unlocked -> MaterialTheme.colorScheme.surface
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(.55f)
                    },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = when {
                                done -> MaterialTheme.colorScheme.secondary.copy(.14f)
                                unlocked -> MaterialTheme.colorScheme.primary.copy(.10f)
                                else -> MaterialTheme.colorScheme.outline.copy(.10f)
                            }
                        ) {
                            Icon(
                                when {
                                    done -> Icons.Default.Check
                                    unlocked -> Icons.Default.PlayArrow
                                    else -> Icons.Default.Lock
                                },
                                contentDescription = null,
                                modifier = Modifier.padding(11.dp),
                                tint = when {
                                    done -> MaterialTheme.colorScheme.secondary
                                    unlocked -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                        Spacer(Modifier.width(13.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${lesson.id}. ${lesson.title}", style = MaterialTheme.typography.titleSmall)
                            Text(lesson.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(5.dp))
                            TinyPill(lesson.skill)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("+${lesson.xp}", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            Text("XP", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
fun PremiumPracticeScreen(data: PracticeUiData, onStart: (Lesson) -> Unit) {
    var custom by remember { mutableStateOf("") }
    val intelligence = data.intelligence
    val modes = remember {
        listOf(
            PracticeMode("15s Sprint", "Short speed burst", Icons.Default.Bolt, Lesson(-101, 0, "15s Sprint", "Short speed burst", "quick mobile typing needs clean rhythm and confident reach", "Speed", 20, 15, true)),
            PracticeMode("Accuracy", "Slow down and remove mistakes", Icons.Default.GpsFixed, Lesson(-102, 0, "Accuracy Lab", "Precision first", "accuracy creates reliable speed when every character is deliberate", "Accuracy", 25, isPractice = true)),
            PracticeMode("Rhythm", "Build even key timing", Icons.Default.GraphicEq, Lesson(-103, 0, "Rhythm Lab", "Even transitions", "read your great new idea then bring the bright thing home", "Rhythm", 25, isPractice = true)),
            PracticeMode("Chat", "Real mobile conversation style", Icons.Default.Chat, Lesson(-104, 0, "Chat Practice", "Natural phone writing", "hey are you free later i will send the details when i get home", "Real-world", 25, isPractice = true)),
            PracticeMode("Work", "Professional mobile messages", Icons.Default.Work, Lesson(-105, 0, "Work Practice", "Professional mobile typing", "thanks for the update i will review the details and send my feedback today", "Real-world", 25, isPractice = true)),
            PracticeMode("Numbers", "Dates, totals and numeric reach", Icons.Default.Numbers, Lesson(-106, 0, "Numbers", "Numeric control", "123 456 789 2026 50 97 15 30 60 100", "Numbers", 25, isPractice = true))
        )
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { AppPageHeader("Practice lab", "Focused sessions for speed, accuracy, rhythm and real-world phone typing.") }
        item { CoachSummaryCard(intelligence) }
        item {
            ActionRow(
                title = "Targeted weakness trainer",
                subtitle = "Generated from your measured weak keys and transitions",
                icon = Icons.Default.AutoFixHigh,
                accent = MaterialTheme.colorScheme.error,
                badge = "Adaptive"
            ) {
                onStart(Lesson(-107, 0, "Weakness Trainer", "Generated from your local performance", data.weakDrill, "Weakness", 35, isPractice = true))
            }
        }
        item { SectionHeading("Practice modes") }
        items(modes) { mode -> ActionRow(mode.title, mode.subtitle, mode.icon) { onStart(mode.lesson) } }
        item { SectionHeading("Custom practice") }
        item {
            PremiumCard {
                OutlinedTextField(
                    value = custom,
                    onValueChange = { custom = it.take(600) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                    label = { Text("Paste or write practice text") },
                    supportingText = { Text("${custom.length}/600 • stays local") }
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        val text = custom.trim()
                        if (text.isNotBlank()) onStart(Lesson(-108, 0, "Custom Practice", "Your temporary text", text, "Custom", 0, isPractice = true))
                    },
                    enabled = custom.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text("Start custom session", fontWeight = FontWeight.Bold) }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

private data class PracticeMode(val title: String, val subtitle: String, val icon: ImageVector, val lesson: Lesson)

@Composable
private fun CoachSummaryCard(intelligence: TrainingIntelligenceProfile) {
    PremiumCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(.10f)) {
                Icon(Icons.Default.Psychology, contentDescription = null, Modifier.padding(12.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text("Adaptive coach", style = MaterialTheme.typography.titleMedium)
                Text(intelligence.headline, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TinyPill("Level ${intelligence.difficultyLevel}/10")
        }
        Spacer(Modifier.height(12.dp))
        Text(intelligence.explanation, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TinyPill(intelligence.priority.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }, Icons.Default.GpsFixed)
            if (intelligence.trend.plateauDetected) TinyPill("Plateau detected", Icons.Default.Timeline, ThumbAmber)
        }
    }
}

@Composable
fun PremiumProgressScreen(data: ProgressUiData) {
    val history = data.history
    val stats = data.keyStats
    val transitions = data.weakTransitions
    val profile = data.profile
    var metric by remember { mutableStateOf(PremiumChartMetric.WPM) }
    var selectedKey by remember { mutableStateOf<Char?>(null) }
    val values = when (metric) {
        PremiumChartMetric.WPM -> history.map { it.wpm }
        PremiumChartMetric.ACCURACY -> history.map { it.accuracy }
        PremiumChartMetric.SCORE -> history.map { it.thumbScore }
    }
    val delta = if (history.size >= 2) history.last().wpm - history.first().wpm else 0

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { AppPageHeader("Progress", "See your performance trend, skill weaknesses and mastery signals.") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                MetricCard("${data.lastWpm}", "Current WPM", Icons.Default.Speed, Modifier.weight(1f))
                MetricCard("${data.bestWpm}", "Best WPM", Icons.Default.EmojiEvents, Modifier.weight(1f), ThumbAmber)
                MetricCard("${data.thumbScore}", "ThumbScore", Icons.Default.AutoGraph, Modifier.weight(1f), MaterialTheme.colorScheme.secondary)
            }
        }
        item { CoachSummaryCard(data.intelligence) }
        item {
            PremiumCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column {
                        Text("Performance trend", style = MaterialTheme.typography.titleLarge)
                        Text("${history.size} recorded sessions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (history.size >= 2 && metric == PremiumChartMetric.WPM) {
                        TinyPill("${if (delta >= 0) "+" else ""}$delta WPM", Icons.Default.TrendingUp, if (delta >= 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    PremiumChartMetric.entries.forEach { item ->
                        FilterChip(
                            selected = metric == item,
                            onClick = { metric = item },
                            label = { Text(when (item) { PremiumChartMetric.WPM -> "WPM"; PremiumChartMetric.ACCURACY -> "Accuracy"; PremiumChartMetric.SCORE -> "Score" }) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (history.isEmpty()) {
                    EmptyInsight("No trend yet", "Complete a few sessions and your performance graph will appear here.", Icons.Default.AutoGraph)
                } else {
                    ProgressChart(values)
                }
            }
        }
        item {
            PremiumCard {
                Text("Baseline & target", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                DetailLine("Starting speed", if (profile.baselineWpm > 0) "${profile.baselineWpm} WPM" else "Not measured")
                DetailLine("Current speed", "${data.lastWpm} WPM")
                DetailLine("Target", "${profile.targetWpm} WPM @ ${profile.targetAccuracy}%")
            }
        }
        item { KeyboardHeatmap(stats) { selectedKey = it } }
        item { SectionHeading("Weak transitions") }
        if (transitions.isEmpty()) {
            item { EmptyInsight("Not enough transition data", "Practice naturally and ThumbType will surface slow or error-prone pairs.", Icons.Default.SyncAlt) }
        } else {
            items(transitions) { (pair, stat) ->
                PremiumCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.error.copy(.10f)) {
                            Text(pair.uppercase(), Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error)
                        }
                        Spacer(Modifier.width(13.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${stat.averageMs} ms average", style = MaterialTheme.typography.titleSmall)
                            Text("${stat.errors} errors • ${stat.accuracy}% accuracy", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TinyPill("Train", Icons.Default.AutoFixHigh, MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        item { SectionHeading("Achievements") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                data.achievements.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        row.forEach { (title, unlocked) -> AchievementChip(title, unlocked, Modifier.weight(1f)) }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
    selectedKey?.let { char -> KeyDetailDialog(char, stats[char] ?: KeyAggregate()) { selectedKey = null } }
}

@Composable
private fun EmptyInsight(title: String, subtitle: String, icon: ImageVector) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceVariant.copy(.55f)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun PremiumProfileScreen(
    data: ProfileUiData,
    settings: AppSettings,
    profile: UserProfile,
    onSettings: (AppSettings) -> Unit,
    onProfile: (UserProfile) -> Unit,
    onPrivacy: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { AppPageHeader("Profile & settings", "Tune your goals, coaching experience, accessibility and privacy.") }
        item {
            GradientHero(
                eyebrow = "Training profile",
                title = "${profile.targetWpm} WPM • ${profile.targetAccuracy}%",
                subtitle = "${profile.dailyGoalMinutes} min/day • ${profile.focus.name.lowercase().replaceFirstChar { it.uppercase() }} focus"
            ) {
                Surface(Modifier.align(Alignment.CenterEnd).padding(end = 20.dp), shape = CircleShape, color = Color.White.copy(.14f)) {
                    Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(data.xp.toString(), fontWeight = FontWeight.Black, color = Color.White)
                        Text("XP", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(.82f))
                    }
                }
            }
        }
        item { SectionHeading("Goals") }
        item {
            PremiumCard {
                Text("Target speed", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                ChoiceChipRows(listOf(30, 40, 50, 60), profile.targetWpm, { "$it" }) { onProfile(profile.copy(targetWpm = it)) }
                Spacer(Modifier.height(12.dp))
                Text("Daily practice", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                ChoiceChipRows(listOf(5, 10, 15, 20), profile.dailyGoalMinutes, { "${it}m" }) { onProfile(profile.copy(dailyGoalMinutes = it)) }
            }
        }
        item { SectionHeading("Coach") }
        item {
            PremiumCard {
                Text("Guidance level", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    CoachLevel.entries.forEach { level ->
                        FilterChip(
                            selected = settings.coachLevel == level,
                            onClick = { onSettings(settings.copy(coachLevel = level)) },
                            label = { Text(level.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        item { SectionHeading("Experience") }
        item { SettingSwitchRow("Dark mode", "Low-light theme", Icons.Default.DarkMode, settings.darkMode) { onSettings(settings.copy(darkMode = it)) } }
        item { SettingSwitchRow("Haptics", "Subtle feedback on mistakes", Icons.Default.Vibration, settings.haptics) { onSettings(settings.copy(haptics = it)) } }
        item { SettingSwitchRow("Sound feedback", "Optional lightweight tones", Icons.Default.VolumeUp, settings.sounds) { onSettings(settings.copy(sounds = it)) } }
        item { SettingSwitchRow("Reduced motion", "Minimize nonessential motion", Icons.Default.MotionPhotosOff, settings.reducedMotion) { onSettings(settings.copy(reducedMotion = it)) } }
        item { SettingSwitchRow("Larger text", "Increase interface typography", Icons.Default.TextFields, settings.largeText) { onSettings(settings.copy(largeText = it)) } }
        item { SectionHeading("Privacy & security") }
        item { SettingSwitchRow("Protect screen", "Blocks screenshots while enabled", Icons.Default.Security, settings.privacyScreenProtection) { onSettings(settings.copy(privacyScreenProtection = it)) } }
        item { ActionRow("Privacy & data", "Offline storage, backup, restore and deletion", Icons.Default.VerifiedUser, MaterialTheme.colorScheme.secondary, onClick = onPrivacy) }
        item {
            PremiumCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Privacy-first core", style = MaterialTheme.typography.titleSmall)
                        Text("No ads SDK, analytics SDK, WebView or Internet permission in this build.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
fun PremiumPrivacyScreen(onBack: () -> Unit, onExport: () -> Unit, onImport: () -> Unit, onDeleteAll: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                Column {
                    Text("Privacy & data", style = MaterialTheme.typography.headlineMedium)
                    Text("Your training data should remain under your control.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            GradientHero("Security posture", "Local by default", "Core lessons, analytics and coaching work without an account.") {
                Icon(Icons.Default.Shield, contentDescription = null, Modifier.align(Alignment.CenterEnd).padding(end = 20.dp).size(54.dp), tint = Color.White.copy(.92f))
            }
        }
        item {
            PremiumCard {
                Text("Stored locally", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                PremiumPrivacyLine(Icons.Default.Speed, "Performance", "WPM, accuracy, scores and session summaries")
                PremiumPrivacyLine(Icons.Default.Keyboard, "Technique", "Aggregated key and transition statistics")
                PremiumPrivacyLine(Icons.Default.EmojiEvents, "Progress", "XP, streaks and completed lessons")
                PremiumPrivacyLine(Icons.Default.Tune, "Preferences", "Goals, theme and coach settings")
            }
        }
        item {
            PremiumCard {
                Text("Permissions not requested", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                listOf("Internet access", "Location", "Contacts", "Camera", "Microphone", "Broad storage permission").forEach { item ->
                    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(9.dp))
                        Text(item, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        item {
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(11.dp))
                    Text("Touch-side analysis uses screen position. It does not claim to identify your biological thumb from Android touch data.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item { SectionHeading("Your controls") }
        item { ActionRow("Export backup", "Create a user-requested JSON backup", Icons.Default.FileUpload, onClick = onExport) }
        item { ActionRow("Restore backup", "Import a ThumbType backup you choose", Icons.Default.Restore, MaterialTheme.colorScheme.secondary, onClick = onImport) }
        item {
            Surface(
                Modifier.fillMaxWidth().clickable { confirm = true },
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Delete all local data", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
                        Text("Reset this device", style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        item {
            PremiumCard {
                Text("Production security baseline", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text("R8 hardening, cleartext blocking, disabled automatic app-data backup and private FileProvider exports are already part of the local build.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete all ThumbType data?") },
            text = { Text("This removes local profile, settings, progress and analytics. Export first if you need a copy.") },
            confirmButton = {
                TextButton(onClick = { confirm = false; onDeleteAll() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun PremiumPrivacyLine(icon: ImageVector, title: String, subtitle: String) {
    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(11.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun PremiumResultsScreen(
    result: SessionResult,
    profile: UserProfile,
    personalBest: Int,
    onContinue: () -> Unit,
    onRetry: () -> Unit,
    onWeakness: () -> Unit
) {
    val insight = TrainingEngine.coachingInsight(result, profile.targetAccuracy)
    val isBest = result.netWpm >= personalBest && result.netWpm > 0

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            GradientHero(
                eyebrow = if (isBest) "New personal best" else "Session complete",
                title = "${result.netWpm} WPM",
                subtitle = result.title
            ) {
                Surface(Modifier.align(Alignment.CenterEnd).padding(end = 20.dp), shape = CircleShape, color = Color.White.copy(.14f)) {
                    Icon(if (isBest) Icons.Default.EmojiEvents else Icons.Default.DoneAll, contentDescription = null, Modifier.padding(16.dp).size(34.dp), tint = Color.White)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                ScoreRing(result.thumbScore)
                Column(horizontalAlignment = Alignment.Start) {
                    Text("${result.accuracy}%", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.secondary)
                    Text("ACCURACY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(8.dp))
                    TinyPill("${result.mistakes} mistakes", Icons.Default.GpsFixed, if (result.mistakes == 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                MetricCard("${result.rhythm}%", "Rhythm", Icons.Default.GraphicEq, Modifier.weight(1f), ThumbAmber)
                MetricCard("${result.consistency}%", "Consistency", Icons.Default.Timeline, Modifier.weight(1f))
                MetricCard("${result.thumbTechnique}%", "Reach match", Icons.Default.TouchApp, Modifier.weight(1f), MaterialTheme.colorScheme.secondary)
            }
        }
        item {
            PremiumCard {
                Text("Session breakdown", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                PercentBar("Accuracy", result.accuracy, MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(10.dp))
                PercentBar("Rhythm", result.rhythm, ThumbAmber)
                Spacer(Modifier.height(10.dp))
                PercentBar("Consistency", result.consistency)
                Spacer(Modifier.height(12.dp))
                DetailLine("Raw WPM", "${result.rawWpm}")
                DetailLine("Attempts", "${result.attempts}")
                DetailLine("Correct characters", "${result.chars}")
                DetailLine("Recovered mistakes", "${result.correctedErrors}")
                DetailLine("Unresolved mistakes", "${result.uncorrectedErrors}")
            }
        }
        item {
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(11.dp))
                    Column {
                        Text("Coach insight", style = MaterialTheme.typography.titleSmall)
                        Text(insight, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        if (result.weakKey != null || result.weakTransition != null) {
            item {
                PremiumCard {
                    Text("Next improvement target", style = MaterialTheme.typography.titleMedium)
                    result.weakKey?.let { DetailLine("Weak key", it.uppercase()) }
                    result.weakTransition?.let { DetailLine("Weak transition", it.uppercase()) }
                    DetailLine("Touch balance", "L ${result.leftTouches} • R ${result.rightTouches}")
                }
            }
        }
        item {
            Button(onClick = onContinue, Modifier.fillMaxWidth().height(54.dp), shape = MaterialTheme.shapes.medium) {
                Text("Continue", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onRetry, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Replay, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Retry")
                }
                OutlinedButton(onClick = onWeakness, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Weakness")
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

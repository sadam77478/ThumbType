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
import androidx.compose.ui.unit.dp
import com.sadam.thumbtype.mobile.app.LearnUiData
import com.sadam.thumbtype.mobile.app.PracticeUiData
import com.sadam.thumbtype.mobile.app.ProfileUiData
import com.sadam.thumbtype.mobile.app.ProgressUiData

private enum class PremiumChartMetric { WPM, ACCURACY, SCORE }
private data class PremiumPracticeItem(val title: String, val subtitle: String, val icon: ImageVector, val lesson: Lesson)

@Composable
fun PremiumOnboardingScreen(
    initialProfile: UserProfile,
    onSaveProfile: (UserProfile) -> Unit,
    onBaseline: () -> Unit,
    onSkip: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var wpm by remember { mutableIntStateOf(initialProfile.targetWpm) }
    var accuracy by remember { mutableIntStateOf(initialProfile.targetAccuracy) }
    var minutes by remember { mutableIntStateOf(initialProfile.dailyGoalMinutes) }
    var focus by remember { mutableStateOf(initialProfile.focus) }
    val spacing = ThumbTypeDesign.spacing

    Column(Modifier.fillMaxSize().padding(horizontal = spacing.lg, vertical = spacing.md)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(.10f)) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PhoneAndroid, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(7.dp))
                    Text("ThumbType", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.weight(1f))
            TinyPill("${step + 1}/5")
        }
        Spacer(Modifier.height(spacing.md))
        LinearProgressIndicator(
            progress = { (step + 1) / 5f },
            modifier = Modifier.fillMaxWidth().height(7.dp),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(Modifier.height(spacing.xl))

        Box(Modifier.weight(1f)) {
            when (step) {
                0 -> Column {
                    GradientHero("Mobile-first training", "Build elite two-thumb control", "Train reach, rhythm, precision and real phone typing with a keyboard designed for practice.") {
                        Icon(Icons.Default.TouchApp, null, Modifier.align(Alignment.CenterEnd).padding(end = 20.dp).size(54.dp), tint = Color.White.copy(.92f))
                    }
                    Spacer(Modifier.height(22.dp))
                    PremiumFeature("Adaptive coaching", "Weak keys and transitions shape your next workout.", Icons.Default.AutoGraph)
                    PremiumFeature("Adaptive reach", "Center keys can be assigned to the most efficient screen side.", Icons.Default.TouchApp)
                    PremiumFeature("Privacy-first", "Core training and analytics stay local in this build.", Icons.Default.Security)
                }
                1 -> Column {
                    AppPageHeader("Set your target", "Choose goals that are challenging but realistic.")
                    Spacer(Modifier.height(20.dp))
                    PremiumCard {
                        Text("Target speed", style = MaterialTheme.typography.titleMedium)
                        PremiumChoiceGrid(listOf(30, 40, 50, 60), wpm, { "$it WPM" }) { wpm = it }
                        Spacer(Modifier.height(14.dp))
                        Text("Target accuracy", style = MaterialTheme.typography.titleMedium)
                        PremiumChoiceGrid(listOf(95, 97, 98, 99), accuracy, { "$it%" }) { accuracy = it }
                    }
                }
                2 -> Column {
                    AppPageHeader("Choose your focus", "This changes coaching emphasis without restricting practice modes.")
                    Spacer(Modifier.height(16.dp))
                    listOf(
                        Triple(TrainingFocus.BALANCED, "Balanced", "Speed + accuracy + rhythm") to Icons.Default.Tune,
                        Triple(TrainingFocus.SPEED, "Speed", "Reduce hesitation safely") to Icons.Default.Bolt,
                        Triple(TrainingFocus.ACCURACY, "Accuracy", "Precision before pace") to Icons.Default.GpsFixed,
                        Triple(TrainingFocus.RHYTHM, "Rhythm", "Even key-to-key timing") to Icons.Default.GraphicEq
                    ).forEach { (meta, icon) ->
                        val selected = focus == meta.first
                        Surface(
                            Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable { focus = meta.first },
                            shape = MaterialTheme.shapes.large,
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary.copy(.25f) else MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(icon, null, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(13.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(meta.second, style = MaterialTheme.typography.titleSmall)
                                    Text(meta.third, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (selected) Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                3 -> Column {
                    AppPageHeader("Build a daily routine", "Short daily practice is easier to sustain and measure.")
                    Spacer(Modifier.height(16.dp))
                    listOf(5, 10, 15, 20).forEach { value ->
                        val selected = minutes == value
                        Surface(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { minutes = value },
                            shape = MaterialTheme.shapes.large,
                            color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("$value", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.secondary)
                                Spacer(Modifier.width(8.dp))
                                Text("minutes / day", style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.weight(1f))
                                if (selected) Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
                else -> Column {
                    GradientHero("Plan ready", "Start with a clean baseline", "A short test gives ThumbType a useful starting point for personalized coaching.")
                    Spacer(Modifier.height(18.dp))
                    PremiumCard {
                        DetailLine("Speed goal", "$wpm WPM")
                        DetailLine("Accuracy goal", "$accuracy%")
                        DetailLine("Daily training", "$minutes min")
                        DetailLine("Primary focus", focus.name.lowercase().replaceFirstChar { it.uppercase() })
                    }
                }
            }
        }

        Spacer(Modifier.height(spacing.md))
        if (step < 4) {
            Button(onClick = { step++ }, Modifier.fillMaxWidth().height(56.dp), shape = MaterialTheme.shapes.medium) {
                Text(if (step == 0) "Build my training plan" else "Continue", fontWeight = FontWeight.Bold)
            }
            if (step > 0) TextButton(onClick = { step-- }, Modifier.fillMaxWidth()) { Text("Back") }
        } else {
            Button(
                onClick = {
                    onSaveProfile(UserProfile(wpm, accuracy, minutes, focus))
                    onBaseline()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Timer, null)
                Spacer(Modifier.width(7.dp))
                Text("Take baseline test", fontWeight = FontWeight.Bold)
            }
            TextButton(
                onClick = {
                    onSaveProfile(UserProfile(wpm, accuracy, minutes, focus))
                    onSkip()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Skip test for now") }
        }
    }
}

@Composable
private fun PremiumFeature(title: String, subtitle: String, icon: ImageVector) {
    Surface(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(.10f)) {
                Icon(icon, null, Modifier.padding(9.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PremiumChoiceGrid(values: List<Int>, selected: Int, label: (Int) -> String, onSelected: (Int) -> Unit) {
    Spacer(Modifier.height(8.dp))
    values.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { value ->
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelected(value) },
                    label = { Text(label(value), fontWeight = FontWeight.Bold) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(5.dp))
    }
}

@Composable
fun PremiumLearnScreen(data: LearnUiData, onStart: (Lesson) -> Unit) {
    val completed = data.completedLessonIds
    val nextId = LessonRepository.lessons.firstOrNull { it.id !in completed }?.id ?: Int.MAX_VALUE
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
        item { AppPageHeader("Learning path", "Move from foundations to advanced mobile control one focused lesson at a time.") }
        item {
            PremiumCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.School, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Curriculum progress", style = MaterialTheme.typography.titleMedium)
                        Text("${completed.size} of ${LessonRepository.lessons.size} lessons complete", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TinyPill("${completed.size * 100 / LessonRepository.lessons.size.coerceAtLeast(1)}%")
                }
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { completed.size.toFloat() / LessonRepository.lessons.size.coerceAtLeast(1) },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
        LessonRepository.lessons.groupBy { it.stage }.forEach { (stage, lessons) ->
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("STAGE $stage", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(LessonRepository.stageNames[stage] ?: "Training", style = MaterialTheme.typography.titleLarge)
                    }
                    TinyPill("${lessons.count { it.id in completed }}/${lessons.size}")
                }
            }
            items(lessons) { lesson ->
                val done = lesson.id in completed
                val unlocked = lesson.id <= nextId
                Surface(
                    Modifier.fillMaxWidth().clickable(enabled = unlocked) { onStart(lesson) },
                    shape = MaterialTheme.shapes.large,
                    color = if (done) MaterialTheme.colorScheme.secondary.copy(.055f) else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(.09f)) {
                            Icon(
                                when { done -> Icons.Default.Check; unlocked -> Icons.Default.PlayArrow; else -> Icons.Default.Lock },
                                null,
                                Modifier.padding(10.dp),
                                tint = when { done -> MaterialTheme.colorScheme.secondary; unlocked -> MaterialTheme.colorScheme.primary; else -> MaterialTheme.colorScheme.onSurfaceVariant }
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${lesson.id}. ${lesson.title}", style = MaterialTheme.typography.titleSmall)
                            Text(lesson.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("+${lesson.xp}", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            Text("XP", style = MaterialTheme.typography.labelSmall)
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
    val modes = remember {
        listOf(
            PremiumPracticeItem("15s Sprint", "Short reaction-speed burst", Icons.Default.Bolt, Lesson(-101, 0, "15s Sprint", "Short speed burst", "quick mobile typing needs clean rhythm and confident reach", "Speed", 20, 15, true)),
            PremiumPracticeItem("Accuracy", "Slow down and remove mistakes", Icons.Default.GpsFixed, Lesson(-102, 0, "Accuracy Lab", "Precision first", "accuracy creates reliable speed when every character is deliberate", "Accuracy", 25, isPractice = true)),
            PremiumPracticeItem("Rhythm", "Build even key timing", Icons.Default.GraphicEq, Lesson(-103, 0, "Rhythm Lab", "Even transitions", "read your great new idea then bring the bright thing home", "Rhythm", 25, isPractice = true)),
            PremiumPracticeItem("Chat", "Natural conversation-style typing", Icons.Default.Chat, Lesson(-104, 0, "Chat Practice", "Real phone writing", "hey are you free later i will send the details when i get home", "Chat", 25, isPractice = true)),
            PremiumPracticeItem("Work", "Professional mobile writing", Icons.Default.Work, Lesson(-105, 0, "Work Practice", "Professional phone typing", "thanks for the update i will review the details and send my feedback today", "Work", 25, isPractice = true)),
            PremiumPracticeItem("Numbers", "Dates, totals and numeric reach", Icons.Default.Numbers, Lesson(-106, 0, "Numbers", "Numeric control", "123 456 789 2026 50 97 15 30 60 100", "Numbers", 25, isPractice = true))
        )
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { AppPageHeader("Practice lab", "Choose a focused mode or let ThumbType target your measured weaknesses.") }
        item { PremiumCoachCard(data.intelligence) }
        item {
            ActionRow("Targeted weakness trainer", "Generated from weak keys and transitions", Icons.Default.AutoFixHigh, MaterialTheme.colorScheme.error, "Adaptive") {
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
                    supportingText = { Text("${custom.length}/600 • local only") }
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        val text = custom.trim()
                        if (text.isNotEmpty()) onStart(Lesson(-108, 0, "Custom Practice", "Temporary local text", text, "Custom", 0, isPractice = true))
                    },
                    enabled = custom.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text("Start custom session", fontWeight = FontWeight.Bold) }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun PremiumCoachCard(intelligence: TrainingIntelligenceProfile) {
    PremiumCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(.10f)) {
                Icon(Icons.Default.Psychology, null, Modifier.padding(11.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Adaptive coach", style = MaterialTheme.typography.titleMedium)
                Text(intelligence.headline, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TinyPill("L${intelligence.difficultyLevel}")
        }
        Spacer(Modifier.height(10.dp))
        Text(intelligence.explanation, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            TinyPill(intelligence.priority.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }, Icons.Default.GpsFixed)
            if (intelligence.trend.plateauDetected) TinyPill("Plateau", Icons.Default.Timeline, ThumbAmber)
        }
    }
}

@Composable
fun PremiumProgressScreen(data: ProgressUiData) {
    var metric by remember { mutableStateOf(PremiumChartMetric.WPM) }
    var selectedKey by remember { mutableStateOf<Char?>(null) }
    val values = when (metric) {
        PremiumChartMetric.WPM -> data.history.map { it.wpm }
        PremiumChartMetric.ACCURACY -> data.history.map { it.accuracy }
        PremiumChartMetric.SCORE -> data.history.map { it.thumbScore }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        item { AppPageHeader("Progress", "See your trend, mastery signals and the areas that deserve the next practice block.") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("${data.lastWpm}", "Current", Icons.Default.Speed, Modifier.weight(1f))
                MetricCard("${data.bestWpm}", "Best", Icons.Default.EmojiEvents, Modifier.weight(1f), ThumbAmber)
                MetricCard("${data.thumbScore}", "ThumbScore", Icons.Default.AutoGraph, Modifier.weight(1f), MaterialTheme.colorScheme.secondary)
            }
        }
        item { PremiumCoachCard(data.intelligence) }
        item {
            PremiumCard {
                Text("Performance trend", style = MaterialTheme.typography.titleLarge)
                Text("${data.history.size} recorded sessions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PremiumChartMetric.entries.forEach { value ->
                        FilterChip(
                            selected = metric == value,
                            onClick = { metric = value },
                            label = { Text(value.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                if (data.history.isEmpty()) {
                    Text("Complete a few sessions and your trend will appear here.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else ProgressChart(values)
            }
        }
        item { KeyboardHeatmap(data.keyStats) { selectedKey = it } }
        item { SectionHeading("Weak transitions") }
        if (data.weakTransitions.isEmpty()) {
            item { PremiumFeature("More data needed", "Practice naturally and slow or error-prone pairs will appear here.", Icons.Default.SyncAlt) }
        } else {
            items(data.weakTransitions) { (pair, stat) ->
                PremiumCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.error.copy(.10f)) {
                            Text(pair.uppercase(), Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("${stat.averageMs} ms average", style = MaterialTheme.typography.titleSmall)
                            Text("${stat.errors} errors • ${stat.accuracy}% accuracy", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        item { SectionHeading("Achievements") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                data.achievements.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { (title, unlocked) -> AchievementChip(title, unlocked, Modifier.weight(1f)) }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
    selectedKey?.let { key -> KeyDetailDialog(key, data.keyStats[key] ?: KeyAggregate()) { selectedKey = null } }
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
        item { AppPageHeader("Profile & settings", "Tune goals, coaching, accessibility and privacy.") }
        item {
            GradientHero("Training profile", "${profile.targetWpm} WPM • ${profile.targetAccuracy}%", "${profile.dailyGoalMinutes} min/day • ${profile.focus.name.lowercase().replaceFirstChar { it.uppercase() }} focus") {
                TinyPill("${data.xp} XP", Icons.Default.Stars, ThumbAmber)
            }
        }
        item { SectionHeading("Goals") }
        item {
            PremiumCard {
                Text("Target speed", style = MaterialTheme.typography.titleSmall)
                PremiumChoiceGrid(listOf(30, 40, 50, 60), profile.targetWpm, { "$it" }) { onProfile(profile.copy(targetWpm = it)) }
                Spacer(Modifier.height(8.dp))
                Text("Daily practice", style = MaterialTheme.typography.titleSmall)
                PremiumChoiceGrid(listOf(5, 10, 15, 20), profile.dailyGoalMinutes, { "${it}m" }) { onProfile(profile.copy(dailyGoalMinutes = it)) }
            }
        }
        item { SectionHeading("Coach") }
        item {
            PremiumCard {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
        item { SettingSwitchRow("Haptics", "Feedback on mistakes", Icons.Default.Vibration, settings.haptics) { onSettings(settings.copy(haptics = it)) } }
        item { SettingSwitchRow("Sound feedback", "Optional training tones", Icons.Default.VolumeUp, settings.sounds) { onSettings(settings.copy(sounds = it)) } }
        item { SettingSwitchRow("Reduced motion", "Minimize nonessential motion", Icons.Default.MotionPhotosOff, settings.reducedMotion) { onSettings(settings.copy(reducedMotion = it)) } }
        item { SettingSwitchRow("Larger text", "Increase interface typography", Icons.Default.TextFields, settings.largeText) { onSettings(settings.copy(largeText = it)) } }
        item { SectionHeading("Privacy & security") }
        item { SettingSwitchRow("Protect screen", "Blocks screenshots while enabled", Icons.Default.Security, settings.privacyScreenProtection) { onSettings(settings.copy(privacyScreenProtection = it)) } }
        item { ActionRow("Privacy & data", "Backup, restore, local storage and deletion", Icons.Default.VerifiedUser, MaterialTheme.colorScheme.secondary, onClick = onPrivacy) }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
fun PremiumPrivacyScreen(onBack: () -> Unit, onExport: () -> Unit, onImport: () -> Unit, onDeleteAll: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                Column {
                    Text("Privacy & data", style = MaterialTheme.typography.headlineMedium)
                    Text("Your training data stays under your control.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { GradientHero("Security posture", "Local by default", "Core lessons, analytics and coaching work without an account.") }
        item {
            PremiumCard {
                Text("Stored locally", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                PremiumPrivacyLine(Icons.Default.Speed, "Performance", "WPM, accuracy, scores and session summaries")
                PremiumPrivacyLine(Icons.Default.Keyboard, "Technique", "Aggregated key and transition statistics")
                PremiumPrivacyLine(Icons.Default.EmojiEvents, "Progress", "XP, streaks and completed lessons")
                PremiumPrivacyLine(Icons.Default.Tune, "Preferences", "Goals, theme and coach settings")
            }
        }
        item {
            PremiumCard {
                Text("Permissions not requested", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                listOf("Internet access", "Location", "Contacts", "Camera", "Microphone", "Broad storage permission").forEach { label ->
                    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        item {
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Text("Touch-side analysis uses screen position; it does not identify your biological thumb.", style = MaterialTheme.typography.bodyMedium)
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
                    Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(11.dp))
                    Column {
                        Text("Delete all local data", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
                        Text("Reset this device", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete all ThumbType data?") },
            text = { Text("This removes local profile, settings, progress and analytics. Export first if needed.") },
            confirmButton = { TextButton(onClick = { confirm = false; onDeleteAll() }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun PremiumPrivacyLine(icon: ImageVector, title: String, subtitle: String) {
    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
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
    val best = result.netWpm >= personalBest && result.netWpm > 0
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        item {
            GradientHero(if (best) "New personal best" else "Session complete", "${result.netWpm} WPM", result.title) {
                Icon(if (best) Icons.Default.EmojiEvents else Icons.Default.DoneAll, null, Modifier.align(Alignment.CenterEnd).padding(end = 20.dp).size(46.dp), tint = Color.White)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                ScoreRing(result.thumbScore)
                Column(horizontalAlignment = Alignment.Start) {
                    Text("${result.accuracy}%", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.secondary)
                    Text("ACCURACY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(7.dp))
                    TinyPill("${result.mistakes} mistakes", Icons.Default.GpsFixed, if (result.mistakes == 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("${result.rhythm}%", "Rhythm", Icons.Default.GraphicEq, Modifier.weight(1f), ThumbAmber)
                MetricCard("${result.consistency}%", "Consistency", Icons.Default.Timeline, Modifier.weight(1f))
                MetricCard("${result.thumbTechnique}%", "Reach", Icons.Default.TouchApp, Modifier.weight(1f), MaterialTheme.colorScheme.secondary)
            }
        }
        item {
            PremiumCard {
                Text("Session breakdown", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                PercentBar("Accuracy", result.accuracy, MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(8.dp))
                PercentBar("Rhythm", result.rhythm, ThumbAmber)
                Spacer(Modifier.height(8.dp))
                PercentBar("Consistency", result.consistency)
                Spacer(Modifier.height(10.dp))
                DetailLine("Raw WPM", "${result.rawWpm}")
                DetailLine("Attempts", "${result.attempts}")
                DetailLine("Correct characters", "${result.chars}")
                DetailLine("Recovered mistakes", "${result.correctedErrors}")
                DetailLine("Unresolved mistakes", "${result.uncorrectedErrors}")
            }
        }
        item {
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Psychology, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
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
            Button(onClick = onContinue, Modifier.fillMaxWidth().height(54.dp), shape = MaterialTheme.shapes.medium) { Text("Continue", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onRetry, Modifier.weight(1f)) {
                    Icon(Icons.Default.Replay, null)
                    Spacer(Modifier.width(5.dp))
                    Text("Retry")
                }
                OutlinedButton(onClick = onWeakness, Modifier.weight(1f)) {
                    Icon(Icons.Default.AutoFixHigh, null)
                    Spacer(Modifier.width(5.dp))
                    Text("Weakness")
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

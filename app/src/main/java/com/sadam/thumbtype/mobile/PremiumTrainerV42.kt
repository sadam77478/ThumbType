package com.sadam.thumbtype.mobile

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun PremiumTrainerScreen(
    lesson: Lesson,
    settings: AppSettings,
    profile: UserProfile,
    onExit: () -> Unit,
    onComplete: (SessionResult) -> Unit
) {
    val target = remember(lesson.id) {
        val base = lesson.text.trim().ifBlank { "type with calm accurate two thumb rhythm" }
        if (lesson.timeLimitSeconds != null) List(5) { base }.joinToString(" ") else base
    }
    var index by remember { mutableIntStateOf(0) }
    val events = remember { mutableStateListOf<PressEvent>() }
    var started by remember { mutableLongStateOf(0L) }
    var lastAt by remember { mutableLongStateOf(0L) }
    var previousRecommendation by remember { mutableStateOf<ThumbSide?>(null) }
    var layer by remember { mutableStateOf(KeyboardLayer.LETTERS) }
    var shift by remember { mutableStateOf(false) }
    var wrong by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val haptic = LocalHapticFeedback.current
    val tone = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 25) }
    val spacing = ThumbTypeDesign.spacing
    val windowInfo = rememberThumbTypeWindowInfo()

    DisposableEffect(Unit) { onDispose { tone.release() } }

    val expected = target.getOrNull(index)
    val recommendation = expected?.let {
        TrainingEngine.recommendedThumb(
            it,
            target.getOrNull(index - 1),
            target.getOrNull(index + 1),
            previousRecommendation
        )
    } ?: ThumbSide.FLEX

    fun finish(duration: Long? = null) {
        if (finished) return
        finished = true
        val elapsedDuration = duration ?: if (started == 0L) 1L else System.currentTimeMillis() - started
        onComplete(TrainingEngine.calculateResult(lesson.title, events.toList(), elapsedDuration, profile.targetWpm))
    }

    LaunchedEffect(started, finished) {
        while (started > 0 && !finished) {
            now = System.currentTimeMillis()
            val limit = lesson.timeLimitSeconds
            if (limit != null && now - started >= limit * 1000L) {
                finish(limit * 1000L)
                break
            }
            delay(150)
        }
    }

    val elapsed = if (started == 0L) 0L else (now - started).coerceAtLeast(1L)
    val wpm = if (elapsed < 800L) 0 else (((index / 5.0) / (elapsed / 60_000.0))).roundToInt().coerceAtMost(250)
    val accuracy = if (events.isEmpty()) 100 else (events.count { it.correct } * 100.0 / events.size).roundToInt()
    val rhythm = TrainingEngine.rhythmScore(events.filter { it.correct }.drop(1).map { it.elapsedFromPreviousMs })
    val errors = events.count { !it.correct }
    val progress = if (lesson.timeLimitSeconds != null && started > 0L) {
        (elapsed / (lesson.timeLimitSeconds * 1000f)).coerceIn(0f, 1f)
    } else {
        index / target.length.toFloat().coerceAtLeast(1f)
    }
    val remaining = lesson.timeLimitSeconds?.let { (it - elapsed / 1000L).coerceAtLeast(0L) }

    Column(Modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(.7f))
        ) {
            Column(Modifier.padding(horizontal = spacing.sm, vertical = spacing.xs)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onExit, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Exit training")
                    }
                    Column(Modifier.weight(1f)) {
                        Text(lesson.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                        Text(
                            "${lesson.skill} • focused training",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TinyPill(
                        remaining?.let { "${it}s" } ?: "${(progress * 100).toInt()}%",
                        if (remaining != null) Icons.Default.Timer else Icons.Default.Timeline
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(5.dp),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            PremiumTrainerMetrics(
                wpm = wpm,
                accuracy = accuracy,
                rhythm = rhythm,
                errors = errors,
                stacked = settings.largeText || windowInfo.widthDp < 380
            )

            PremiumCard(contentPadding = PaddingValues(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("TARGET", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                        Text(
                            if (index >= target.length) "Complete" else "Character ${index + 1} of ${target.length}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    TinyPill(if (wrong) "Correct the key" else "Stay relaxed", if (wrong) Icons.Default.ErrorOutline else Icons.Default.Spa, if (wrong) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary)
                }
                Spacer(Modifier.height(12.dp))
                PremiumTargetText(target, index, wrong, settings.largeText)
            }

            if (settings.coachLevel != CoachLevel.OFF && expected != null) {
                PremiumThumbCoach(
                    expected = expected,
                    side = recommendation,
                    layer = layer,
                    shift = shift,
                    minimal = settings.coachLevel == CoachLevel.MINIMAL
                )
            }
            Spacer(Modifier.height(2.dp))
        }

        PremiumTrainingKeyboard(
            expected = expected,
            recommended = recommendation,
            layer = layer,
            shift = shift,
            settings = settings,
            onLayer = {
                layer = if (layer == KeyboardLayer.LETTERS) KeyboardLayer.NUMBERS else KeyboardLayer.LETTERS
                wrong = false
            },
            onShift = {
                shift = !shift
                wrong = false
            },
            onBackspace = { wrong = false }
        ) { raw, touchSide ->
            if (!finished && expected != null) {
                val time = System.currentTimeMillis()
                if (started == 0L) {
                    started = time
                    lastAt = time
                    now = time
                }
                val entered = if (raw.isLetter() && shift) raw.uppercaseChar() else raw.lowercaseChar()
                val correct = entered == expected
                events += PressEvent(
                    expected = expected,
                    entered = entered,
                    correct = correct,
                    elapsedFromPreviousMs = (time - lastAt).coerceAtLeast(1L),
                    recommended = recommendation,
                    touchSide = touchSide,
                    targetIndex = index
                )
                lastAt = time

                if (correct) {
                    if (settings.sounds) tone.startTone(ToneGenerator.TONE_PROP_BEEP, 15)
                    wrong = false
                    previousRecommendation = recommendation
                    index++
                    shift = false
                    if (index >= target.length) finish()
                } else {
                    if (settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (settings.sounds) tone.startTone(ToneGenerator.TONE_PROP_NACK, 45)
                    wrong = true
                }
            }
        }
    }
}

@Composable
private fun PremiumTrainerMetrics(wpm: Int, accuracy: Int, rhythm: Int, errors: Int, stacked: Boolean) {
    if (stacked) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PremiumLiveMetric("$wpm", "WPM", Icons.Default.Speed, Modifier.weight(1f), MaterialTheme.colorScheme.primary)
                PremiumLiveMetric("$accuracy%", "Accuracy", Icons.Default.GpsFixed, Modifier.weight(1f), MaterialTheme.colorScheme.secondary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PremiumLiveMetric("$rhythm%", "Rhythm", Icons.Default.GraphicEq, Modifier.weight(1f), ThumbAmber)
                PremiumLiveMetric("$errors", "Errors", Icons.Default.Warning, Modifier.weight(1f), MaterialTheme.colorScheme.error)
            }
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PremiumLiveMetric("$wpm", "WPM", Icons.Default.Speed, Modifier.weight(1f), MaterialTheme.colorScheme.primary)
            PremiumLiveMetric("$accuracy%", "Accuracy", Icons.Default.GpsFixed, Modifier.weight(1f), MaterialTheme.colorScheme.secondary)
            PremiumLiveMetric("$rhythm%", "Rhythm", Icons.Default.GraphicEq, Modifier.weight(1f), ThumbAmber)
            PremiumLiveMetric("$errors", "Errors", Icons.Default.Warning, Modifier.weight(1f), MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun PremiumLiveMetric(value: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, accent: Color) {
    Surface(
        modifier = modifier.thumbTypeReadout(label, value),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(horizontal = 6.dp, vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp), tint = accent)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        }
    }
}

@Composable
private fun PremiumTargetText(text: String, index: Int, wrong: Boolean, large: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val error = MaterialTheme.colorScheme.error
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val expected = text.getOrNull(index)
    val expectedLabel = when (expected) {
        null -> "complete"
        ' ' -> "space"
        else -> expected.toString()
    }
    val annotated = buildAnnotatedString {
        text.forEachIndexed { i, char ->
            when {
                i < index -> withStyle(SpanStyle(color = secondary, fontWeight = FontWeight.SemiBold)) { append(char) }
                i == index -> withStyle(
                    SpanStyle(
                        color = if (wrong) error else primary,
                        background = (if (wrong) error else primary).copy(.14f),
                        fontWeight = FontWeight.Black
                    )
                ) { append(char) }
                else -> withStyle(SpanStyle(color = muted)) { append(char) }
            }
        }
    }
    Text(
        annotated,
        modifier = Modifier.semantics { contentDescription = "Typing target. Next character: $expectedLabel" },
        style = if (large) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
        lineHeight = if (large) MaterialTheme.typography.headlineSmall.lineHeight else MaterialTheme.typography.titleLarge.lineHeight
    )
}

@Composable
private fun PremiumThumbCoach(expected: Char, side: ThumbSide, layer: KeyboardLayer, shift: Boolean, minimal: Boolean) {
    val needLayer = TrainingEngine.requiredLayer(expected) != layer
    val needShift = expected.isUpperCase() && !shift
    val instruction = when {
        needLayer -> if (TrainingEngine.requiredLayer(expected) == KeyboardLayer.NUMBERS) "Switch to 123" else "Switch to ABC"
        needShift -> "Tap SHIFT first"
        side == ThumbSide.LEFT -> "Left screen-side reach"
        side == ThumbSide.RIGHT -> "Right screen-side reach"
        else -> "Flexible center reach"
    }
    val accent = if (needLayer || needShift) ThumbAmber else MaterialTheme.colorScheme.primary
    val targetLabel = if (expected == ' ') "space" else expected.uppercase()

    Surface(
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = "Reach coach. $instruction. Target $targetLabel"
        },
        shape = MaterialTheme.shapes.large,
        color = accent.copy(.075f),
        border = BorderStroke(1.dp, accent.copy(.16f))
    ) {
        if (minimal) {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TouchApp, contentDescription = null, tint = accent)
                Spacer(Modifier.width(8.dp))
                Text("$instruction • ${if (expected == ' ') "SPACE" else expected.uppercase()}", style = MaterialTheme.typography.labelLarge, color = accent)
            }
        } else {
            Column(Modifier.padding(13.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("LIVE REACH COACH", style = MaterialTheme.typography.labelSmall, color = accent, fontWeight = FontWeight.Black)
                        Text(instruction, style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(Modifier.width(8.dp))
                    TinyPill(if (expected == ' ') "SPACE" else expected.uppercase(), Icons.Default.Keyboard, accent)
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    PremiumCoachSide("LEFT", side == ThumbSide.LEFT || side == ThumbSide.FLEX, MaterialTheme.colorScheme.primary)
                    Icon(Icons.Default.SyncAlt, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    PremiumCoachSide("RIGHT", side == ThumbSide.RIGHT || side == ThumbSide.FLEX, MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
private fun PremiumCoachSide(label: String, active: Boolean, accent: Color) {
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = "$label screen side, ${if (active) "recommended" else "not currently recommended"}"
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(shape = CircleShape, color = if (active) accent.copy(.15f) else MaterialTheme.colorScheme.surfaceVariant) {
            Icon(
                Icons.Default.TouchApp,
                contentDescription = null,
                modifier = Modifier.padding(9.dp).size(22.dp),
                tint = if (active) accent else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = if (active) accent else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PremiumTrainingKeyboard(
    expected: Char?,
    recommended: ThumbSide,
    layer: KeyboardLayer,
    shift: Boolean,
    settings: AppSettings,
    onLayer: () -> Unit,
    onShift: () -> Unit,
    onBackspace: () -> Unit,
    onKey: (Char, ThumbSide) -> Unit
) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge.copy(bottomStart = androidx.compose.foundation.shape.CornerSize(0.dp), bottomEnd = androidx.compose.foundation.shape.CornerSize(0.dp)),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 10.dp
    ) {
        Column(Modifier.padding(horizontal = 6.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("TRAINING KEYBOARD", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 6.dp))
                TinyPill(if (layer == KeyboardLayer.LETTERS) "ABC" else "123", Icons.Default.Keyboard)
            }
            if (layer == KeyboardLayer.LETTERS) {
                PremiumKeyRow("qwertyuiop", expected, recommended, shift, settings, onKey)
                PremiumKeyRow("asdfghjkl", expected, recommended, shift, settings, onKey, 12)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    PremiumSpecialKey("⇧", "Shift", shift || expected?.isUpperCase() == true, Modifier.weight(1.18f), onShift)
                    "zxcvbnm".forEach { PremiumKey(it, expected, recommended, shift, settings, Modifier.weight(1f), onKey) }
                    PremiumSpecialKey("⌫", "Clear error prompt", false, Modifier.weight(1.18f), onBackspace)
                }
                PremiumBottomRow(expected, recommended, settings, onLayer, onKey, "123")
            } else {
                PremiumKeyRow("1234567890", expected, recommended, false, settings, onKey)
                PremiumKeyRow("@#%&*-+=", expected, recommended, false, settings, onKey, 14)
                PremiumKeyRow("()/:;!?\'", expected, recommended, false, settings, onKey, 20)
                PremiumBottomRow(expected, recommended, settings, onLayer, onKey, "ABC")
            }
            Text(
                "Reach-match uses the side of the screen tapped; accessibility activation is excluded from side scoring.",
                Modifier.fillMaxWidth().padding(top = 1.dp),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PremiumBottomRow(
    expected: Char?,
    recommended: ThumbSide,
    settings: AppSettings,
    onLayer: () -> Unit,
    onKey: (Char, ThumbSide) -> Unit,
    label: String
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        PremiumSpecialKey(label, "Switch keyboard layer", true, Modifier.weight(1.35f), onLayer)
        PremiumKey(',', expected, recommended, false, settings, Modifier.weight(.82f), onKey)
        PremiumKey(' ', expected, recommended, false, settings, Modifier.weight(4.1f), onKey, "space")
        PremiumKey('.', expected, recommended, false, settings, Modifier.weight(.82f), onKey)
        PremiumKey('?', expected, recommended, false, settings, Modifier.weight(1f), onKey)
    }
}

@Composable
private fun PremiumKeyRow(
    chars: String,
    expected: Char?,
    recommended: ThumbSide,
    shift: Boolean,
    settings: AppSettings,
    onKey: (Char, ThumbSide) -> Unit,
    pad: Int = 0
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = pad.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        chars.forEach { char -> PremiumKey(char, expected, recommended, shift, settings, Modifier.weight(1f), onKey) }
    }
}

@Composable
private fun PremiumKey(
    char: Char,
    expected: Char?,
    recommended: ThumbSide,
    shift: Boolean,
    settings: AppSettings,
    modifier: Modifier,
    onKey: (Char, ThumbSide) -> Unit,
    label: String? = null
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val widthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    var x by remember(char) { mutableFloatStateOf(0f) }
    val active = expected?.let { if (char == ' ') it == ' ' else it.lowercaseChar() == char.lowercaseChar() } == true
    val zone = TrainingEngine.fixedZone(char)
    val background = when {
        active -> MaterialTheme.colorScheme.primary
        zone == ThumbSide.LEFT -> MaterialTheme.colorScheme.primary.copy(.07f)
        zone == ThumbSide.RIGHT -> MaterialTheme.colorScheme.secondary.copy(.085f)
        else -> ThumbAmber.copy(.075f)
    }
    val description = when {
        char == ' ' -> "Space"
        else -> (label ?: char.toString()).uppercase()
    }
    val stateText = buildString {
        if (active) append("Expected key")
        if (recommended != ThumbSide.FLEX && active) {
            if (isNotEmpty()) append(", ")
            append(if (recommended == ThumbSide.LEFT) "left screen side recommended" else "right screen side recommended")
        }
    }.ifBlank { null }

    Surface(
        modifier = modifier
            .heightIn(min = ThumbTypeSizes.minimumTouchTarget)
            .thumbTypeAccessibleAction(
                label = "Keyboard key $description",
                stateText = stateText,
                onActivate = { onKey(char, ThumbSide.FLEX) }
            )
            .onGloballyPositioned { x = it.positionInWindow().x }
            .pointerInput(char, widthPx, shift) {
                detectTapGestures { local ->
                    onKey(char, if (x + local.x < widthPx / 2f) ThumbSide.LEFT else ThumbSide.RIGHT)
                }
            },
        shape = MaterialTheme.shapes.small,
        color = background,
        border = BorderStroke(1.dp, if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(.75f)),
        shadowElevation = if (active) 3.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label ?: if (char.isLetter() && shift) char.uppercase() else char.toString(),
                style = if (label != null) MaterialTheme.typography.labelSmall else MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
            if (active && recommended != ThumbSide.FLEX && settings.coachLevel == CoachLevel.FULL) {
                Box(
                    Modifier
                        .align(if (recommended == ThumbSide.LEFT) Alignment.BottomStart else Alignment.BottomEnd)
                        .padding(5.dp)
                        .size(6.dp)
                        .background(MaterialTheme.colorScheme.onPrimary, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun PremiumSpecialKey(label: String, description: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val accent = if (active) ThumbAmber else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier
            .heightIn(min = ThumbTypeSizes.minimumTouchTarget)
            .thumbTypeAccessibleAction(
                label = description,
                stateText = if (active) "Active" else null,
                onActivate = onClick
            )
            .pointerInput(label) { detectTapGestures { onClick() } },
        shape = MaterialTheme.shapes.small,
        color = if (active) ThumbAmber.copy(.16f) else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, if (active) ThumbAmber.copy(.28f) else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = accent)
        }
    }
}

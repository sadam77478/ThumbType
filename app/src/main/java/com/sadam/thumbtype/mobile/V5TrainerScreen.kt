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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * ThumbType V5 trainer rebuild.
 *
 * Key differences from the V4 trainer:
 * - visible text is editor-like: a wrong key advances and remains visible as an error;
 * - backspace really removes the previous visible character and moves the cursor backward;
 * - physical PressEvent history is retained, so correcting text does not erase analytics;
 * - live WPM uses physical attempts, matching final gross/net result semantics;
 * - the keyboard is the visual center of the screen rather than a generic form control.
 */
@Composable
fun V5TrainerScreen(
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
    var editor by remember(lesson.id) { mutableStateOf(V5TrainingSessionState()) }
    val events = remember(lesson.id) { mutableStateListOf<PressEvent>() }
    var started by remember(lesson.id) { mutableLongStateOf(0L) }
    var lastAt by remember(lesson.id) { mutableLongStateOf(0L) }
    var previousRecommendation by remember(lesson.id) { mutableStateOf<ThumbSide?>(null) }
    var layer by remember(lesson.id) { mutableStateOf(KeyboardLayer.LETTERS) }
    var shift by remember(lesson.id) { mutableStateOf(false) }
    var lastInputWrong by remember(lesson.id) { mutableStateOf(false) }
    var finished by remember(lesson.id) { mutableStateOf(false) }
    var now by remember(lesson.id) { mutableLongStateOf(System.currentTimeMillis()) }

    val tone = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 24) }
    val haptic = LocalHapticFeedback.current
    val window = rememberThumbTypeWindowInfo()
    val expected = editor.expected(target)
    val recommendation = expected?.let {
        TrainingEngine.recommendedThumb(
            it,
            target.getOrNull(editor.cursor - 1),
            target.getOrNull(editor.cursor + 1),
            previousRecommendation
        )
    } ?: ThumbSide.FLEX

    DisposableEffect(Unit) { onDispose { tone.release() } }

    fun finish(durationOverride: Long? = null) {
        if (finished) return
        finished = true
        val duration = durationOverride ?: if (started == 0L) 1L else (System.currentTimeMillis() - started).coerceAtLeast(1L)
        onComplete(TrainingEngine.calculateResult(lesson.title, events.toList(), duration, profile.targetWpm))
    }

    LaunchedEffect(started, finished) {
        while (started > 0L && !finished) {
            now = System.currentTimeMillis()
            lesson.timeLimitSeconds?.let { limit ->
                if (now - started >= limit * 1000L) {
                    finish(limit * 1000L)
                    return@LaunchedEffect
                }
            }
            delay(120)
        }
    }

    val elapsed = if (started == 0L) 0L else (now - started).coerceAtLeast(1L)
    val minutes = elapsed.coerceAtLeast(1L) / 60_000.0
    val mistakes = events.count { !it.correct }
    val correctAttempts = events.count { it.correct }
    val rawWpm = if (elapsed < 700L || events.isEmpty()) 0 else ((events.size / 5.0) / minutes).roundToInt().coerceIn(0, 250)
    val netWpm = if (events.isEmpty()) 0 else (rawWpm - (mistakes / minutes).roundToInt()).coerceIn(0, 250)
    val accuracy = if (events.isEmpty()) 100 else (correctAttempts * 100.0 / events.size).roundToInt().coerceIn(0, 100)
    val rhythm = TrainingEngine.rhythmScore(events.filter { it.correct }.drop(1).map { it.elapsedFromPreviousMs })
    val unresolved = editor.unresolvedErrors()
    val progress = if (lesson.timeLimitSeconds != null && started > 0L) {
        (elapsed / (lesson.timeLimitSeconds * 1000f)).coerceIn(0f, 1f)
    } else {
        editor.cursor / target.length.toFloat().coerceAtLeast(1f)
    }
    val remaining = lesson.timeLimitSeconds?.let { (it - elapsed / 1000L).coerceAtLeast(0L) }
    val compact = window.widthDp < 390 || settings.largeText

    Column(
        Modifier
            .fillMaxSize()
            .testTag("v5-trainer-root")
            .background(MaterialTheme.colorScheme.background)
    ) {
        V5TrainerHeader(
            title = lesson.title,
            skill = lesson.skill,
            progress = progress,
            remaining = remaining,
            onExit = onExit
        )

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            V5MetricsStrip(netWpm, rawWpm, accuracy, rhythm, mistakes, compact)

            V5TypingCanvas(
                target = target,
                editor = editor,
                lastInputWrong = lastInputWrong,
                largeText = settings.largeText
            )

            if (settings.coachLevel != CoachLevel.OFF && expected != null) {
                V5ReachCoach(
                    expected = expected,
                    side = recommendation,
                    layer = layer,
                    shift = shift,
                    minimal = settings.coachLevel == CoachLevel.MINIMAL
                )
            }

            if (editor.isComplete(target) && unresolved > 0) {
                V5CorrectionBanner(
                    errors = unresolved,
                    backspaces = editor.backspaceCount,
                    onFinishWithErrors = { finish() }
                )
            }
        }

        V5TrainingKeyboard(
            expected = expected,
            recommended = recommendation,
            layer = layer,
            shift = shift,
            settings = settings,
            canBackspace = editor.cursor > 0,
            onLayer = {
                layer = if (layer == KeyboardLayer.LETTERS) KeyboardLayer.NUMBERS else KeyboardLayer.LETTERS
                lastInputWrong = false
            },
            onShift = {
                shift = !shift
                lastInputWrong = false
            },
            onBackspace = {
                if (editor.cursor > 0) {
                    editor = editor.backspace()
                    lastInputWrong = false
                    shift = false
                    if (settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }
        ) { raw, touchSide ->
            val currentExpected = editor.expected(target)
            if (!finished && currentExpected != null) {
                val time = System.currentTimeMillis()
                if (started == 0L) {
                    started = time
                    lastAt = time
                    now = time
                }

                val entered = if (raw.isLetter() && shift) raw.uppercaseChar() else raw
                val isCorrect = entered == currentExpected
                val targetIndex = editor.cursor
                events += PressEvent(
                    expected = currentExpected,
                    entered = entered,
                    correct = isCorrect,
                    elapsedFromPreviousMs = (time - lastAt).coerceAtLeast(1L),
                    recommended = recommendation,
                    touchSide = touchSide,
                    targetIndex = targetIndex
                )
                lastAt = time
                editor = editor.type(target, entered)
                lastInputWrong = !isCorrect

                if (isCorrect) {
                    previousRecommendation = recommendation
                    if (settings.sounds) tone.startTone(ToneGenerator.TONE_PROP_BEEP, 14)
                } else {
                    if (settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (settings.sounds) tone.startTone(ToneGenerator.TONE_PROP_NACK, 42)
                }
                shift = false

                if (editor.isCleanComplete(target)) finish()
            }
        }
    }
}

@Composable
private fun V5TrainerHeader(
    title: String,
    skill: String,
    progress: Float,
    remaining: Long?,
    onExit: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    Box(
        Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(primary.copy(.11f), tertiary.copy(.08f), Color.Transparent)))
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().padding(start = 6.dp, end = 14.dp, top = 8.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onExit, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Exit training")
                }
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).background(MaterialTheme.colorScheme.secondary, CircleShape))
                        Spacer(Modifier.width(6.dp))
                        Text("LIVE SESSION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Black)
                    }
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(skill, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(.86f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(Modifier.padding(horizontal = 11.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (remaining != null) Icons.Default.Timer else Icons.Default.Bolt, null, Modifier.size(15.dp), tint = primary)
                        Spacer(Modifier.width(5.dp))
                        Text(remaining?.let { "${it}s" } ?: "${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                    }
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun V5MetricsStrip(netWpm: Int, rawWpm: Int, accuracy: Int, rhythm: Int, errors: Int, compact: Boolean) {
    if (compact) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                V5Metric("$netWpm", "NET WPM", "gross $rawWpm", MaterialTheme.colorScheme.primary, Icons.Default.Speed, Modifier.weight(1f))
                V5Metric("$accuracy%", "ACCURACY", "$errors errors", MaterialTheme.colorScheme.secondary, Icons.Default.GpsFixed, Modifier.weight(1f))
            }
            V5Metric("$rhythm%", "RHYTHM", "timing stability", ThumbAmber, Icons.Default.GraphicEq, Modifier.fillMaxWidth())
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            V5Metric("$netWpm", "NET WPM", "gross $rawWpm", MaterialTheme.colorScheme.primary, Icons.Default.Speed, Modifier.weight(1f))
            V5Metric("$accuracy%", "ACCURACY", "$errors errors", MaterialTheme.colorScheme.secondary, Icons.Default.GpsFixed, Modifier.weight(1f))
            V5Metric("$rhythm%", "RHYTHM", "timing stability", ThumbAmber, Icons.Default.GraphicEq, Modifier.weight(1f))
        }
    }
}

@Composable
private fun V5Metric(value: String, label: String, detail: String, accent: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Surface(
        modifier = modifier.thumbTypeReadout(label, value),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(.8f)),
        shadowElevation = 1.dp
    ) {
        Row(Modifier.padding(horizontal = 11.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = accent.copy(.11f)) {
                Icon(icon, null, Modifier.padding(7.dp).size(17.dp), tint = accent)
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(label, style = MaterialTheme.typography.labelSmall, color = accent, fontWeight = FontWeight.Bold)
                Text(detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun V5TypingCanvas(target: String, editor: V5TrainingSessionState, lastInputWrong: Boolean, largeText: Boolean) {
    val unresolved = editor.unresolvedErrors()
    val statusColor = when {
        unresolved > 0 -> MaterialTheme.colorScheme.error
        editor.cursor > 0 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, statusColor.copy(.18f)),
        shadowElevation = 3.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("TYPE THE PASSAGE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                    Text(
                        when {
                            editor.isCleanComplete(target) -> "Clean finish"
                            editor.isComplete(target) -> "Review before finishing"
                            lastInputWrong -> "Mistake recorded — use backspace to repair it"
                            else -> "${editor.cursor.coerceAtMost(target.length)} / ${target.length} characters"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(shape = CircleShape, color = statusColor.copy(.10f)) {
                    Text(
                        if (unresolved == 0) "CLEAN" else "$unresolved OPEN",
                        Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = statusColor
                    )
                }
            }
            Spacer(Modifier.height(13.dp))
            V5TargetWindow(target, editor, largeText)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("⌫ actually edits your text", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (editor.backspaceCount > 0) {
                    Text("${editor.backspaceCount} corrections", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun V5TargetWindow(target: String, editor: V5TrainingSessionState, largeText: Boolean) {
    val cursor = editor.cursor.coerceIn(0, target.length)
    val radius = if (largeText) 28 else 44
    val start = (cursor - radius).coerceAtLeast(0)
    val end = (cursor + radius).coerceAtMost(target.length)
    val success = MaterialTheme.colorScheme.secondary
    val error = MaterialTheme.colorScheme.error
    val active = MaterialTheme.colorScheme.primary
    val future = MaterialTheme.colorScheme.onSurfaceVariant
    val annotated = buildAnnotatedString {
        if (start > 0) withStyle(SpanStyle(color = future.copy(.55f))) { append("…") }
        for (i in start until end) {
            val slot = editor.slotAt(i)
            when {
                slot != null && slot.correct -> withStyle(SpanStyle(color = success, fontWeight = FontWeight.SemiBold)) { append(slot.entered) }
                slot != null -> withStyle(SpanStyle(color = error, background = error.copy(.12f), fontWeight = FontWeight.Black)) { append(slot.entered) }
                i == cursor -> withStyle(SpanStyle(color = active, background = active.copy(.14f), fontWeight = FontWeight.Black)) { append(target[i]) }
                else -> withStyle(SpanStyle(color = future)) { append(target[i]) }
            }
        }
        if (end < target.length) withStyle(SpanStyle(color = future.copy(.55f))) { append("…") }
    }
    val next = target.getOrNull(cursor)?.let { if (it == ' ') "space" else it.toString() } ?: "end of passage"
    Text(
        annotated,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Typing passage. Next target: $next" },
        style = if (largeText) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
        fontFamily = FontFamily.Monospace,
        lineHeight = if (largeText) MaterialTheme.typography.headlineSmall.lineHeight else MaterialTheme.typography.titleLarge.lineHeight
    )
}

@Composable
private fun V5ReachCoach(expected: Char, side: ThumbSide, layer: KeyboardLayer, shift: Boolean, minimal: Boolean) {
    val needLayer = TrainingEngine.requiredLayer(expected) != layer
    val needShift = expected.isUpperCase() && !shift
    val instruction = when {
        needLayer -> if (TrainingEngine.requiredLayer(expected) == KeyboardLayer.NUMBERS) "Switch keyboard to 123" else "Switch keyboard to ABC"
        needShift -> "Use Shift before the next key"
        side == ThumbSide.LEFT -> "Reach from the left screen side"
        side == ThumbSide.RIGHT -> "Reach from the right screen side"
        else -> "Use the more comfortable center reach"
    }
    val accent = if (needLayer || needShift) ThumbAmber else MaterialTheme.colorScheme.primary
    val targetLabel = if (expected == ' ') "SPACE" else expected.uppercase()

    Surface(
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "Reach coach. $instruction. Target $targetLabel" },
        shape = MaterialTheme.shapes.large,
        color = accent.copy(.07f),
        border = BorderStroke(1.dp, accent.copy(.17f))
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = if (minimal) 10.dp else 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = accent.copy(.14f)) {
                Icon(Icons.Default.TouchApp, null, Modifier.padding(8.dp).size(19.dp), tint = accent)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(if (minimal) "REACH" else "LIVE REACH COACH", style = MaterialTheme.typography.labelSmall, color = accent, fontWeight = FontWeight.Black)
                Text(instruction, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(8.dp))
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
                Text(targetLabel, Modifier.padding(horizontal = 12.dp, vertical = 7.dp), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = accent)
            }
        }
    }
}

@Composable
private fun V5CorrectionBanner(errors: Int, backspaces: Int, onFinishWithErrors: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer.copy(.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(.25f))
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EditNote, null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text("$errors unresolved ${if (errors == 1) "mistake" else "mistakes"}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("Backspace to the red characters and retype them, or finish with the mistakes recorded.", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (backspaces > 0) {
                Spacer(Modifier.height(5.dp))
                Text("$backspaces backspace actions used in this session", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onFinishWithErrors, Modifier.fillMaxWidth()) { Text("Finish with recorded mistakes") }
        }
    }
}

@Composable
private fun V5TrainingKeyboard(
    expected: Char?,
    recommended: ThumbSide,
    layer: KeyboardLayer,
    shift: Boolean,
    settings: AppSettings,
    canBackspace: Boolean,
    onLayer: () -> Unit,
    onShift: () -> Unit,
    onBackspace: () -> Unit,
    onKey: (Char, ThumbSide) -> Unit
) {
    Surface(
        Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 4.dp,
        shadowElevation = 14.dp
    ) {
        Column(
            Modifier.padding(start = 5.dp, end = 5.dp, top = 8.dp, bottom = 7.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("THUMBREACH KEYBOARD", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("blue left • amber center • teal right", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(.10f)) {
                    Text(if (layer == KeyboardLayer.LETTERS) "ABC" else "123", Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                }
            }

            if (layer == KeyboardLayer.LETTERS) {
                V5KeyRow("qwertyuiop", expected, recommended, shift, settings, onKey)
                V5KeyRow("asdfghjkl", expected, recommended, shift, settings, onKey, 11)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    V5SpecialKey("⇧", "Shift", shift || expected?.isUpperCase() == true, true, Modifier.weight(1.18f), onShift)
                    "zxcvbnm".forEach { V5Key(it, expected, recommended, shift, settings, Modifier.weight(1f), onKey) }
                    V5SpecialKey("⌫", "Backspace", canBackspace, canBackspace, Modifier.weight(1.18f), onBackspace)
                }
                V5BottomRow(expected, recommended, settings, onLayer, onKey, "123")
            } else {
                V5KeyRow("1234567890", expected, recommended, false, settings, onKey)
                V5KeyRow("@#%&*-+=", expected, recommended, false, settings, onKey, 14)
                V5KeyRow("()/:;!?\'", expected, recommended, false, settings, onKey, 18)
                V5BottomRow(expected, recommended, settings, onLayer, onKey, "ABC")
            }
        }
    }
}

@Composable
private fun V5BottomRow(
    expected: Char?,
    recommended: ThumbSide,
    settings: AppSettings,
    onLayer: () -> Unit,
    onKey: (Char, ThumbSide) -> Unit,
    label: String
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        V5SpecialKey(label, "Switch keyboard layer", true, true, Modifier.weight(1.38f), onLayer)
        V5Key(',', expected, recommended, false, settings, Modifier.weight(.82f), onKey)
        V5Key(' ', expected, recommended, false, settings, Modifier.weight(4.15f), onKey, "space")
        V5Key('.', expected, recommended, false, settings, Modifier.weight(.82f), onKey)
        V5Key('?', expected, recommended, false, settings, Modifier.weight(1f), onKey)
    }
}

@Composable
private fun V5KeyRow(
    chars: String,
    expected: Char?,
    recommended: ThumbSide,
    shift: Boolean,
    settings: AppSettings,
    onKey: (Char, ThumbSide) -> Unit,
    horizontalPadding: Int = 0
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = horizontalPadding.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        chars.forEach { V5Key(it, expected, recommended, shift, settings, Modifier.weight(1f), onKey) }
    }
}

@Composable
private fun V5Key(
    char: Char,
    expected: Char?,
    recommended: ThumbSide,
    shift: Boolean,
    settings: AppSettings,
    modifier: Modifier,
    onKey: (Char, ThumbSide) -> Unit,
    spokenLabel: String? = null
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    var xInWindow by remember(char) { mutableFloatStateOf(0f) }
    var pressed by remember(char) { mutableStateOf(false) }
    val active = expected?.let { if (char == ' ') it == ' ' else it.lowercaseChar() == char.lowercaseChar() } == true
    val zone = TrainingEngine.fixedZone(char)
    val zoneAccent = when (zone) {
        ThumbSide.LEFT -> MaterialTheme.colorScheme.primary
        ThumbSide.RIGHT -> MaterialTheme.colorScheme.secondary
        ThumbSide.FLEX -> ThumbAmber
    }
    val background = when {
        active -> MaterialTheme.colorScheme.primary
        pressed -> zoneAccent.copy(.20f)
        else -> zoneAccent.copy(if (char == ' ') .10f else .075f)
    }
    val label = spokenLabel ?: if (char == ' ') "Space" else char.toString().uppercase()
    val stateText = when {
        active && recommended == ThumbSide.LEFT -> "Expected key, left screen side recommended"
        active && recommended == ThumbSide.RIGHT -> "Expected key, right screen side recommended"
        active -> "Expected key"
        else -> null
    }

    Surface(
        modifier = modifier
            .heightIn(min = 52.dp)
            .thumbTypeAccessibleAction("Keyboard key $label", stateText) { onKey(char, ThumbSide.FLEX) }
            .onGloballyPositioned { xInWindow = it.positionInWindow().x }
            .pointerInput(char, screenWidthPx, shift) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = { local ->
                        val side = if (xInWindow + local.x < screenWidthPx / 2f) ThumbSide.LEFT else ThumbSide.RIGHT
                        onKey(char, side)
                    }
                )
            },
        shape = MaterialTheme.shapes.medium,
        color = background,
        border = BorderStroke(1.dp, if (active) MaterialTheme.colorScheme.primary else zoneAccent.copy(.16f)),
        shadowElevation = if (active || pressed) 4.dp else 1.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                spokenLabel ?: if (char.isLetter() && shift) char.uppercase() else char.toString(),
                style = if (spokenLabel != null) MaterialTheme.typography.labelSmall else MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
            if (active) {
                val cueAlignment = when (recommended) {
                    ThumbSide.LEFT -> Alignment.BottomStart
                    ThumbSide.RIGHT -> Alignment.BottomEnd
                    ThumbSide.FLEX -> Alignment.BottomCenter
                }
                Box(Modifier.align(cueAlignment).padding(5.dp).size(5.dp).background(MaterialTheme.colorScheme.onPrimary, CircleShape))
            }
        }
    }
}

@Composable
private fun V5SpecialKey(
    label: String,
    description: String,
    active: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    var pressed by remember(label) { mutableStateOf(false) }
    val accent = if (active) ThumbAmber else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier
            .heightIn(min = 52.dp)
            .thumbTypeAccessibleAction(description, if (!enabled) "Unavailable" else if (active) "Active" else null) { if (enabled) onClick() }
            .pointerInput(label, enabled) {
                detectTapGestures(
                    onPress = {
                        if (enabled) {
                            pressed = true
                            tryAwaitRelease()
                            pressed = false
                        }
                    },
                    onTap = { if (enabled) onClick() }
                )
            },
        shape = MaterialTheme.shapes.medium,
        color = when {
            !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(.45f)
            pressed -> accent.copy(.22f)
            active -> ThumbAmber.copy(.15f)
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        border = BorderStroke(1.dp, if (active) ThumbAmber.copy(.28f) else MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = if (pressed) 3.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = if (enabled) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(.45f), textAlign = TextAlign.Center)
        }
    }
}

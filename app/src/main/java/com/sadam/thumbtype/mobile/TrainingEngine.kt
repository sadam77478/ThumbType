package com.sadam.thumbtype.mobile

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

object TrainingEngine {
    private const val LEFT_FIXED = "qwerasdfzxcv"
    private const val RIGHT_FIXED = "uiopjklm"
    private const val CENTER = "tyghbn"

    private data class TimedEvent(val event: PressEvent, val atMs: Long)

    fun fixedZone(char: Char): ThumbSide {
        val c = char.lowercaseChar()
        return when {
            c in LEFT_FIXED -> ThumbSide.LEFT
            c in RIGHT_FIXED -> ThumbSide.RIGHT
            c in CENTER || c == ' ' -> ThumbSide.FLEX
            c.isDigit() -> if (c in '0'..'5') ThumbSide.LEFT else ThumbSide.RIGHT
            c in "`~!@#$%^" -> ThumbSide.LEFT
            c in "&*()-_=+[]{}\\|;:'\",.<>/?" -> ThumbSide.RIGHT
            else -> ThumbSide.FLEX
        }
    }

    fun recommendedThumb(
        char: Char,
        previousChar: Char?,
        nextChar: Char?,
        previousRecommended: ThumbSide?
    ): ThumbSide {
        val fixed = fixedZone(char)
        if (fixed != ThumbSide.FLEX) return fixed

        if (char == ' ') {
            return when (previousRecommended) {
                ThumbSide.LEFT -> ThumbSide.RIGHT
                ThumbSide.RIGHT -> ThumbSide.LEFT
                else -> ThumbSide.FLEX
            }
        }

        val natural = when (char.lowercaseChar()) {
            't', 'g', 'b' -> ThumbSide.LEFT
            'y', 'h', 'n' -> ThumbSide.RIGHT
            else -> ThumbSide.FLEX
        }
        if (natural == ThumbSide.FLEX) return ThumbSide.FLEX

        val nextFixed = nextChar?.let(::fixedZone)
        val previousFixed = previousChar?.let(::fixedZone)

        fun cost(candidate: ThumbSide): Double {
            var score = 0.0
            if (candidate != natural) score += .45
            if (candidate == previousRecommended) score += 1.15
            if (candidate == previousFixed && previousFixed != ThumbSide.FLEX) score += .35
            if (candidate == nextFixed && nextFixed != ThumbSide.FLEX) score += .28
            return score
        }

        return if (cost(ThumbSide.LEFT) <= cost(ThumbSide.RIGHT)) ThumbSide.LEFT else ThumbSide.RIGHT
    }

    fun requiredLayer(char: Char): KeyboardLayer {
        val c = char.lowercaseChar()
        return if (c.isLetter() || char == ' ' || char in ",.?!'\"") KeyboardLayer.LETTERS else KeyboardLayer.NUMBERS
    }

    fun rhythmScore(intervals: List<Long>): Int {
        val usable = intervals.filter { it in 35..2500 }
        if (usable.size < 3) return 100
        val mean = usable.average()
        if (mean <= 0.0) return 100
        val deviation = usable.map { abs(it - mean) }.average()
        return (100.0 - deviation / mean * 115.0).roundToInt().coerceIn(20, 100)
    }

    fun consistencyScore(intervals: List<Long>): Int {
        val usable = intervals.filter { it in 35..2500 }
        if (usable.size < 3) return 100
        val mean = usable.average()
        if (mean <= 0.0) return 100
        val variance = usable.map { (it - mean) * (it - mean) }.average()
        return (100.0 - sqrt(variance) / mean * 95.0).roundToInt().coerceIn(15, 100)
    }

    /**
     * Produces session analytics from physical key attempts.
     *
     * Definitions used in V3.5:
     * - raw WPM: every character attempt / 5 / elapsed minutes.
     * - net WPM: raw WPM minus incorrect attempts per minute.
     * - accuracy: successful character attempts / all character attempts.
     * - corrected error: a wrong attempt at a target position that is later completed
     *   correctly before the session ends.
     * - uncorrected error: a wrong attempt whose target position is still unresolved when
     *   the session ends.
     *
     * ThumbScore weighting remains the V3 product weighting in this correctness phase; its
     * component inputs are now calculated from corrected attempt semantics.
     */
    fun calculateResult(
        title: String,
        events: List<PressEvent>,
        durationMs: Long,
        targetWpm: Int
    ): SessionResult {
        val safeDuration = durationMs.coerceAtLeast(1L)
        val minutes = safeDuration / 60_000.0
        val attempts = events.size
        val correctEvents = events.filter { it.correct }
        val correctChars = correctEvents.size
        val mistakes = events.count { !it.correct }

        val successfullyCompletedPositions = correctEvents.mapTo(hashSetOf()) { it.targetIndex }
        val correctedErrors = events.count { !it.correct && it.targetIndex in successfullyCompletedPositions }
        val uncorrectedErrors = (mistakes - correctedErrors).coerceAtLeast(0)

        val rawWpm = if (attempts == 0) 0 else ((attempts / 5.0) / minutes).roundToInt().coerceAtLeast(0)
        val errorPenalty = if (minutes > 0.0) (mistakes / minutes).roundToInt() else 0
        val netWpm = (rawWpm - errorPenalty).coerceAtLeast(0)
        val accuracy = if (attempts == 0) 100 else (correctChars * 100.0 / attempts).roundToInt().coerceIn(0, 100)

        val timed = buildTimedEvents(events)
        val correctTimed = timed.filter { it.event.correct }
        val correctIntervals = correctTimed.zipWithNext { a, b -> (b.atMs - a.atMs).coerceAtLeast(1L) }
        val rhythm = rhythmScore(correctIntervals)
        val consistency = consistencyScore(correctIntervals)

        val techniqueEvents = correctEvents.filter {
            it.recommended != ThumbSide.FLEX && it.touchSide != ThumbSide.FLEX
        }
        val technique = if (techniqueEvents.isEmpty()) {
            100
        } else {
            (techniqueEvents.count { it.recommended == it.touchSide } * 100.0 / techniqueEvents.size)
                .roundToInt()
                .coerceIn(0, 100)
        }

        val left = correctEvents.count { it.touchSide == ThumbSide.LEFT }
        val right = correctEvents.count { it.touchSide == ThumbSide.RIGHT }
        val sideTotal = left + right
        val balance = if (sideTotal == 0) {
            0
        } else {
            (100.0 - abs(left - right) * 100.0 / sideTotal).roundToInt().coerceIn(0, 100)
        }

        val speedScore = (netWpm * 100.0 / targetWpm.coerceAtLeast(20)).roundToInt().coerceIn(0, 100)
        val weightedScore = (
            speedScore * .26 +
                accuracy * .29 +
                rhythm * .16 +
                consistency * .11 +
                technique * .13 +
                balance * .05
            ).roundToInt().coerceIn(0, 100)
        val thumbScore = if (attempts == 0) 0 else weightedScore * 10

        val keyMap = buildKeyAggregates(timed)
        val transitionMap = buildTransitionAggregates(timed)

        val weakKey = keyMap.entries
            .filter { it.key.isLetterOrDigit() && it.value.attempts >= 1 }
            .maxByOrNull { keyWeakness(it.value) }
            ?.key

        val weakTransition = transitionMap.entries
            .filter { it.value.attempts >= 1 }
            .maxByOrNull { transitionWeakness(it.value) }
            ?.key

        return SessionResult(
            title = title,
            rawWpm = rawWpm,
            netWpm = netWpm,
            accuracy = accuracy,
            rhythm = rhythm,
            consistency = consistency,
            thumbTechnique = technique,
            thumbBalance = balance,
            thumbScore = thumbScore,
            mistakes = mistakes,
            correctedErrors = correctedErrors,
            uncorrectedErrors = uncorrectedErrors,
            attempts = attempts,
            chars = correctChars,
            durationMs = safeDuration,
            leftTouches = left,
            rightTouches = right,
            weakKey = weakKey,
            weakTransition = weakTransition,
            keyUpdates = keyMap,
            transitionUpdates = transitionMap
        )
    }

    private fun buildTimedEvents(events: List<PressEvent>): List<TimedEvent> {
        var elapsed = 0L
        return events.map { event ->
            elapsed += event.elapsedFromPreviousMs.coerceAtLeast(1L)
            TimedEvent(event, elapsed)
        }
    }

    private fun buildKeyAggregates(events: List<TimedEvent>): Map<Char, KeyAggregate> {
        val aggregates = linkedMapOf<Char, KeyAggregate>()
        val completedByPosition = mutableMapOf<Int, TimedEvent>()

        events.forEach { timed ->
            val event = timed.event
            val key = event.expected.lowercaseChar()
            val old = aggregates[key] ?: KeyAggregate()

            if (event.correct) {
                val previousCompletion = completedByPosition[event.targetIndex - 1]
                val completionLatency = if (previousCompletion != null) {
                    (timed.atMs - previousCompletion.atMs).coerceAtLeast(1L)
                } else {
                    event.elapsedFromPreviousMs.coerceAtLeast(1L)
                }
                aggregates[key] = old.copy(
                    attempts = old.attempts + 1,
                    correct = old.correct + 1,
                    totalCorrectReactionMs = old.totalCorrectReactionMs + completionLatency
                )
                completedByPosition[event.targetIndex] = timed
            } else {
                aggregates[key] = old.copy(
                    attempts = old.attempts + 1,
                    errors = old.errors + 1
                )
            }
        }
        return aggregates
    }

    private fun buildTransitionAggregates(events: List<TimedEvent>): Map<String, TransitionAggregate> {
        val aggregates = linkedMapOf<String, TransitionAggregate>()
        val completedByPosition = mutableMapOf<Int, TimedEvent>()

        events.forEach { timed ->
            val event = timed.event
            val previousCompletion = completedByPosition[event.targetIndex - 1]

            if (previousCompletion != null) {
                val pair = "${previousCompletion.event.expected.lowercaseChar()}${event.expected.lowercaseChar()}"
                val old = aggregates[pair] ?: TransitionAggregate()
                if (event.correct) {
                    aggregates[pair] = old.copy(
                        attempts = old.attempts + 1,
                        correct = old.correct + 1,
                        totalSuccessfulMs = old.totalSuccessfulMs +
                            (timed.atMs - previousCompletion.atMs).coerceAtLeast(1L)
                    )
                } else {
                    aggregates[pair] = old.copy(
                        attempts = old.attempts + 1,
                        errors = old.errors + 1
                    )
                }
            }

            if (event.correct) {
                completedByPosition[event.targetIndex] = timed
            }
        }
        return aggregates
    }

    private fun keyWeakness(stat: KeyAggregate): Int = stat.errorRate * 12 + stat.averageReactionMs

    private fun transitionWeakness(stat: TransitionAggregate): Int = stat.errorRate * 14 + stat.averageMs

    fun generateWeakDrill(
        keyStats: Map<Char, KeyAggregate>,
        transitions: Map<String, TransitionAggregate>
    ): String {
        val weakKeys = keyStats.entries
            .sortedByDescending { keyWeakness(it.value) }
            .map { it.key }
            .filter { it.isLetter() }
            .take(4)
        val weakPairs = transitions.entries
            .sortedByDescending { transitionWeakness(it.value) }
            .map { it.key }
            .filter { it.length == 2 && it.all(Char::isLetter) }
            .take(4)

        if (weakKeys.isEmpty() && weakPairs.isEmpty()) {
            return "bring the thing tonight then begin another bright thought with steady rhythm"
        }

        val bank = listOf(
            "bring", "thing", "night", "better", "great", "quick", "message", "typing", "rhythm", "today",
            "between", "another", "bright", "right", "going", "home", "train", "focus", "practice", "mobile",
            "smooth", "accurate", "steady", "thumb", "reach", "center"
        )
        val chosen = bank.filter { word ->
            weakKeys.any { it in word } || weakPairs.any { it in word }
        }.take(14)
        return (if (chosen.size >= 6) chosen else chosen + bank.take(10)).distinct().joinToString(" ")
    }

    fun coachingInsight(result: SessionResult, targetAccuracy: Int): String = when {
        result.accuracy < targetAccuracy - 5 -> "Slow down slightly. Accuracy is the fastest path to reliable speed."
        result.uncorrectedErrors > 0 -> "You finished with unresolved mistakes. Prioritize clean completion before pushing speed."
        result.thumbTechnique < 70 -> "Your screen-side reach pattern is inconsistent. Follow the live reach cue for center keys."
        result.rhythm < 72 -> "Your biggest opportunity is rhythm. Aim for smaller, more even gaps between key presses."
        result.consistency < 70 -> "Your pace changes sharply during the session. Keep the same comfortable tempo for longer."
        result.netWpm >= 50 && result.accuracy >= targetAccuracy -> "Excellent control. Increase difficulty rather than forcing more speed on easy text."
        result.weakTransition != null -> "Next focus: ${result.weakTransition.uppercase()} transition. ThumbType will emphasize it in Weakness Trainer."
        else -> "Good foundation. Keep accuracy high and let speed rise from smoother transitions."
    }
}

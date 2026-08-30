package com.sadam.thumbtype.mobile

import kotlin.math.abs
import kotlin.math.roundToInt

enum class TrainingPriority {
    FOUNDATION,
    ACCURACY,
    SPEED,
    TRANSITIONS,
    CENTER_REACH,
    LEFT_ZONE,
    RIGHT_ZONE,
    RHYTHM,
    ENDURANCE
}

data class SkillMastery(
    val label: String = "",
    val score: Int = 0,
    val confidence: Int = 0,
    val sampleSize: Int = 0
)

data class PerformanceTrend(
    val speedDeltaWpm: Int = 0,
    val accuracyDelta: Int = 0,
    val thumbScoreDelta: Int = 0,
    val hasComparison: Boolean = false,
    val plateauDetected: Boolean = false
)

data class TrainingIntelligenceProfile(
    val priority: TrainingPriority = TrainingPriority.FOUNDATION,
    val headline: String = "Build a reliable baseline",
    val explanation: String = "Complete a few sessions so ThumbType can personalize your training.",
    val difficultyLevel: Int = 1,
    val recentWpm: Int = 0,
    val recentAccuracy: Int = 0,
    val recentThumbScore: Int = 0,
    val leftZoneMastery: SkillMastery = SkillMastery("Left zone"),
    val rightZoneMastery: SkillMastery = SkillMastery("Right zone"),
    val centerReachMastery: SkillMastery = SkillMastery("Center reach"),
    val weakestKeys: List<SkillMastery> = emptyList(),
    val weakestTransitions: List<SkillMastery> = emptyList(),
    val trend: PerformanceTrend = PerformanceTrend(),
    val targetedDrill: String = "bring the thing tonight then begin another bright thought with steady rhythm"
)

/**
 * Deterministic personalization engine for ThumbType V4.
 *
 * It deliberately works from measured screen-side/zone performance and never claims to
 * identify a user's biological thumb. Mastery combines correctness, latency and sample
 * confidence so a single lucky press cannot be treated as mastered skill.
 */
object TrainingIntelligenceEngine {
    private const val LEFT_FIXED = "qwerasdfzxcv"
    private const val RIGHT_FIXED = "uiopjklm"
    private const val CENTER = "tyghbn"

    fun analyze(
        profile: UserProfile,
        history: List<HistoryEntry>,
        keyStats: Map<Char, KeyAggregate>,
        transitionStats: Map<String, TransitionAggregate>
    ): TrainingIntelligenceProfile {
        val recent = history.takeLast(5)
        val previous = history.dropLast(recent.size).takeLast(5)
        val recentWpm = averageInt(recent.map { it.wpm })
        val recentAccuracy = if (recent.isEmpty()) profile.baselineAccuracy else averageInt(recent.map { it.accuracy })
        val recentThumbScore = averageInt(recent.map { it.thumbScore })

        val trend = buildTrend(recent, previous, history.size)
        val left = zoneMastery("Left zone", LEFT_FIXED, keyStats)
        val right = zoneMastery("Right zone", RIGHT_FIXED, keyStats)
        val center = zoneMastery("Center reach", CENTER, keyStats)

        val weakKeys = keyStats.entries
            .filter { it.key.isLetterOrDigit() && it.value.attempts >= 2 }
            .map { (key, stat) -> keyMastery(key, stat) }
            .sortedWith(compareBy<SkillMastery> { it.score }.thenByDescending { it.confidence })
            .take(6)

        val weakTransitions = transitionStats.entries
            .filter { it.key.length == 2 && it.value.attempts >= 2 }
            .map { (pair, stat) -> transitionMastery(pair, stat) }
            .sortedWith(compareBy<SkillMastery> { it.score }.thenByDescending { it.confidence })
            .take(6)

        val priority = choosePriority(
            profile = profile,
            hasHistory = history.isNotEmpty(),
            recentWpm = recentWpm,
            recentAccuracy = recentAccuracy,
            left = left,
            right = right,
            center = center,
            weakestTransition = weakTransitions.firstOrNull(),
            trend = trend
        )
        val difficulty = difficultyLevel(recentThumbScore, recentAccuracy, profile.targetAccuracy, history.size)
        val targetedDrill = buildTargetedDrill(weakKeys, weakTransitions)
        val (headline, explanation) = coachingCopy(priority, trend, profile, recentWpm, recentAccuracy, weakTransitions.firstOrNull())

        return TrainingIntelligenceProfile(
            priority = priority,
            headline = headline,
            explanation = explanation,
            difficultyLevel = difficulty,
            recentWpm = recentWpm,
            recentAccuracy = recentAccuracy,
            recentThumbScore = recentThumbScore,
            leftZoneMastery = left,
            rightZoneMastery = right,
            centerReachMastery = center,
            weakestKeys = weakKeys,
            weakestTransitions = weakTransitions,
            trend = trend,
            targetedDrill = targetedDrill
        )
    }

    fun keyMastery(key: Char, stat: KeyAggregate): SkillMastery {
        val latency = latencyScore(stat.averageReactionMs, fastMs = 180, slowMs = 900)
        val confidence = confidence(stat.attempts)
        val measured = (stat.accuracy * 0.72 + latency * 0.28).roundToInt().coerceIn(0, 100)
        return SkillMastery(
            label = key.uppercaseChar().toString(),
            score = confidenceAdjusted(measured, confidence),
            confidence = confidence,
            sampleSize = stat.attempts
        )
    }

    fun transitionMastery(pair: String, stat: TransitionAggregate): SkillMastery {
        val latency = latencyScore(stat.averageMs, fastMs = 190, slowMs = 1100)
        val confidence = confidence(stat.attempts)
        val measured = (stat.accuracy * 0.68 + latency * 0.32).roundToInt().coerceIn(0, 100)
        return SkillMastery(
            label = pair.uppercase(),
            score = confidenceAdjusted(measured, confidence),
            confidence = confidence,
            sampleSize = stat.attempts
        )
    }

    fun buildAdaptiveWorkout(
        profile: UserProfile,
        intelligence: TrainingIntelligenceProfile
    ): List<DailyWorkoutItem> {
        val goal = profile.dailyGoalMinutes.coerceIn(5, 30)
        val minutes = allocateMinutes(goal)
        val focusText = focusText(intelligence.priority, intelligence.targetedDrill)
        val focusName = priorityLabel(intelligence.priority)

        return listOf(
            DailyWorkoutItem(
                title = "Warm-up",
                subtitle = "Settle into a clean alternating rhythm",
                minutes = minutes[0],
                lesson = Lesson(
                    id = -4001,
                    stage = 0,
                    title = "Adaptive Warm-up",
                    subtitle = "Easy movement before targeted work",
                    text = "we type better with calm steady rhythm and clean mobile movement today",
                    skill = "Warm-up",
                    xp = 20,
                    isPractice = true
                )
            ),
            DailyWorkoutItem(
                title = "Weakness trainer",
                subtitle = "Targets your lowest-confidence keys and transitions",
                minutes = minutes[1],
                lesson = Lesson(
                    id = -4002,
                    stage = 0,
                    title = "Targeted Weakness Trainer",
                    subtitle = "Generated from your measured typing data",
                    text = intelligence.targetedDrill,
                    skill = "Weakness",
                    xp = 35,
                    isPractice = true
                )
            ),
            DailyWorkoutItem(
                title = "Adaptive focus",
                subtitle = "$focusName • difficulty ${intelligence.difficultyLevel}/10",
                minutes = minutes[2],
                lesson = Lesson(
                    id = -4003,
                    stage = 0,
                    title = "$focusName Focus",
                    subtitle = intelligence.headline,
                    text = focusText,
                    skill = focusName,
                    xp = 40,
                    isPractice = true
                )
            ),
            DailyWorkoutItem(
                title = "Controlled finish",
                subtitle = if (intelligence.trend.plateauDetected) "Break the plateau with controlled difficulty" else "Finish with accurate speed",
                minutes = minutes[3],
                lesson = Lesson(
                    id = -4004,
                    stage = 0,
                    title = "Controlled Finish",
                    subtitle = "Benchmark speed without sacrificing accuracy",
                    text = "mobile typing becomes fast when accuracy rhythm reach and smooth transitions work together",
                    skill = "Benchmark",
                    xp = 35,
                    timeLimitSeconds = 60,
                    isPractice = true
                )
            )
        )
    }

    fun buildTargetedDrill(
        weakKeys: List<SkillMastery>,
        weakTransitions: List<SkillMastery>
    ): String {
        val keys = weakKeys.mapNotNull { it.label.lowercase().singleOrNull() }.take(4)
        val pairs = weakTransitions.map { it.label.lowercase() }
            .filter { it.length == 2 && it.all(Char::isLetter) }
            .take(4)

        if (keys.isEmpty() && pairs.isEmpty()) {
            return "bring the thing tonight then begin another bright thought with steady rhythm"
        }

        val bank = listOf(
            "bring", "thing", "night", "better", "great", "quick", "message", "typing", "rhythm", "today",
            "between", "another", "bright", "right", "going", "home", "train", "focus", "practice", "mobile",
            "smooth", "accurate", "steady", "thumb", "reach", "center", "gentle", "balance", "control", "repeat"
        )
        val selectedWords = bank.filter { word ->
            pairs.any(word::contains) || keys.any(word::contains)
        }.take(12)
        val pairBursts = pairs.flatMap { pair -> listOf(pair, pair, pair) }
        val keyBursts = keys.flatMap { key -> listOf("$key$key", "$key$key") }
        val material = pairBursts + keyBursts + selectedWords
        return (if (material.size >= 10) material else material + bank.take(10))
            .joinToString(" ")
            .take(420)
    }

    private fun buildTrend(
        recent: List<HistoryEntry>,
        previous: List<HistoryEntry>,
        totalSessions: Int
    ): PerformanceTrend {
        if (recent.size < 3 || previous.size < 3) return PerformanceTrend()
        val speedDelta = averageInt(recent.map { it.wpm }) - averageInt(previous.map { it.wpm })
        val accuracyDelta = averageInt(recent.map { it.accuracy }) - averageInt(previous.map { it.accuracy })
        val scoreDelta = averageInt(recent.map { it.thumbScore }) - averageInt(previous.map { it.thumbScore })
        val plateau = totalSessions >= 8 && abs(speedDelta) <= 2 && abs(accuracyDelta) <= 1 && abs(scoreDelta) <= 25
        return PerformanceTrend(speedDelta, accuracyDelta, scoreDelta, true, plateau)
    }

    private fun zoneMastery(
        label: String,
        zone: String,
        stats: Map<Char, KeyAggregate>
    ): SkillMastery {
        val relevant = stats.entries.filter { it.key.lowercaseChar() in zone && it.value.attempts > 0 }
        if (relevant.isEmpty()) return SkillMastery(label)
        val attempts = relevant.sumOf { it.value.attempts }
        val weighted = relevant.sumOf { (key, stat) -> keyMastery(key, stat).score * stat.attempts }
        return SkillMastery(
            label = label,
            score = (weighted.toDouble() / attempts).roundToInt().coerceIn(0, 100),
            confidence = confidence(attempts),
            sampleSize = attempts
        )
    }

    private fun choosePriority(
        profile: UserProfile,
        hasHistory: Boolean,
        recentWpm: Int,
        recentAccuracy: Int,
        left: SkillMastery,
        right: SkillMastery,
        center: SkillMastery,
        weakestTransition: SkillMastery?,
        trend: PerformanceTrend
    ): TrainingPriority {
        if (!hasHistory) return TrainingPriority.FOUNDATION
        val accuracyFloor = (profile.targetAccuracy - 2).coerceAtLeast(92)
        if (recentAccuracy in 1 until accuracyFloor) return TrainingPriority.ACCURACY

        if (weakestTransition != null && weakestTransition.confidence >= 20 && weakestTransition.score < 68) {
            return TrainingPriority.TRANSITIONS
        }
        if (center.confidence >= 20 && center.score < 68) return TrainingPriority.CENTER_REACH

        if (left.confidence >= 20 && right.confidence >= 20 && abs(left.score - right.score) >= 14) {
            return if (left.score < right.score) TrainingPriority.LEFT_ZONE else TrainingPriority.RIGHT_ZONE
        }

        if (trend.plateauDetected) {
            return when (profile.focus) {
                TrainingFocus.ACCURACY -> TrainingPriority.ACCURACY
                TrainingFocus.RHYTHM -> TrainingPriority.RHYTHM
                TrainingFocus.SPEED -> TrainingPriority.SPEED
                TrainingFocus.BALANCED -> if (weakestTransition != null) TrainingPriority.TRANSITIONS else TrainingPriority.ENDURANCE
            }
        }

        if (recentWpm < (profile.targetWpm * 0.85).roundToInt()) return TrainingPriority.SPEED
        return when (profile.focus) {
            TrainingFocus.ACCURACY -> TrainingPriority.ACCURACY
            TrainingFocus.SPEED -> TrainingPriority.SPEED
            TrainingFocus.RHYTHM -> TrainingPriority.RHYTHM
            TrainingFocus.BALANCED -> TrainingPriority.ENDURANCE
        }
    }

    private fun difficultyLevel(
        thumbScore: Int,
        accuracy: Int,
        targetAccuracy: Int,
        sessionCount: Int
    ): Int {
        if (sessionCount == 0) return 1
        var level = (1 + thumbScore / 120).coerceIn(1, 9)
        if (accuracy >= targetAccuracy && thumbScore >= 800) level += 1
        if (accuracy in 1 until (targetAccuracy - 5).coerceAtLeast(85)) level -= 1
        return level.coerceIn(1, 10)
    }

    private fun coachingCopy(
        priority: TrainingPriority,
        trend: PerformanceTrend,
        profile: UserProfile,
        recentWpm: Int,
        recentAccuracy: Int,
        weakestTransition: SkillMastery?
    ): Pair<String, String> {
        if (trend.plateauDetected) {
            return "Your progress has flattened" to
                "Recent speed and accuracy are stable. Today's plan changes the stimulus instead of asking for reckless speed."
        }
        return when (priority) {
            TrainingPriority.FOUNDATION -> "Build a reliable baseline" to "Complete a few sessions so ThumbType can learn your real strengths and weaknesses."
            TrainingPriority.ACCURACY -> "Accuracy before more speed" to "Your recent accuracy is $recentAccuracy% against a ${profile.targetAccuracy}% target. Clean attempts will unlock safer speed."
            TrainingPriority.SPEED -> "Reduce hesitation safely" to "Your recent pace is $recentWpm WPM. The plan will increase tempo while protecting accuracy."
            TrainingPriority.TRANSITIONS -> "Repair a weak transition" to "${weakestTransition?.label ?: "A key pair"} is currently one of your weakest measured transitions."
            TrainingPriority.CENTER_REACH -> "Strengthen center-key control" to "Center keys need more reliable reach and timing before difficulty increases."
            TrainingPriority.LEFT_ZONE -> "Strengthen the left screen zone" to "Left-zone key mastery trails the right side, so today's material adds measured left-side work."
            TrainingPriority.RIGHT_ZONE -> "Strengthen the right screen zone" to "Right-zone key mastery trails the left side, so today's material adds measured right-side work."
            TrainingPriority.RHYTHM -> "Train a steadier tempo" to "Your selected focus is rhythm, so the plan emphasizes even key-to-key timing rather than raw speed."
            TrainingPriority.ENDURANCE -> "Hold quality for longer" to "Your core metrics are stable enough to extend controlled typing without lowering accuracy."
        }
    }

    private fun focusText(priority: TrainingPriority, targeted: String): String = when (priority) {
        TrainingPriority.FOUNDATION -> "the quick brown fox brings a calm mobile message while both screen sides move with steady control"
        TrainingPriority.ACCURACY -> "slow clean typing builds reliable speed when every letter is deliberate accurate and controlled"
        TrainingPriority.SPEED -> "quick mobile messages become faster through short smooth bursts without giving away clean accuracy"
        TrainingPriority.TRANSITIONS -> targeted
        TrainingPriority.CENTER_REACH -> "the thing you bring tonight can begin with gentle center reach then become another bright rhythm"
        TrainingPriority.LEFT_ZONE -> "we practice clear steady words where careful left side reach supports smooth mobile typing"
        TrainingPriority.RIGHT_ZONE -> "you join quick mobile lines with reliable right side reach and controlled accurate movement"
        TrainingPriority.RHYTHM -> "read your great new idea then bring the bright thing home with one calm even typing rhythm"
        TrainingPriority.ENDURANCE -> "steady accurate mobile typing stays relaxed across longer messages when rhythm reach and control remain consistent"
    }

    private fun priorityLabel(priority: TrainingPriority): String = when (priority) {
        TrainingPriority.FOUNDATION -> "Foundation"
        TrainingPriority.ACCURACY -> "Accuracy"
        TrainingPriority.SPEED -> "Speed"
        TrainingPriority.TRANSITIONS -> "Transitions"
        TrainingPriority.CENTER_REACH -> "Center reach"
        TrainingPriority.LEFT_ZONE -> "Left zone"
        TrainingPriority.RIGHT_ZONE -> "Right zone"
        TrainingPriority.RHYTHM -> "Rhythm"
        TrainingPriority.ENDURANCE -> "Endurance"
    }

    private fun allocateMinutes(goal: Int): IntArray {
        val allocation = intArrayOf(
            maxOf(1, goal * 20 / 100),
            maxOf(1, goal * 30 / 100),
            maxOf(1, goal * 30 / 100),
            maxOf(1, goal * 20 / 100)
        )
        while (allocation.sum() < goal) allocation[2] += 1
        while (allocation.sum() > goal) {
            val index = allocation.indices.filter { allocation[it] > 1 }.maxByOrNull { allocation[it] } ?: break
            allocation[index] -= 1
        }
        return allocation
    }

    private fun latencyScore(valueMs: Int, fastMs: Int, slowMs: Int): Int {
        if (valueMs <= 0) return 50
        if (valueMs <= fastMs) return 100
        if (valueMs >= slowMs) return 20
        val range = (slowMs - fastMs).coerceAtLeast(1)
        val fraction = (valueMs - fastMs).toDouble() / range
        return (100 - fraction * 80).roundToInt().coerceIn(20, 100)
    }

    private fun confidence(attempts: Int): Int =
        (attempts.coerceAtLeast(0) * 100.0 / (attempts.coerceAtLeast(0) + 20.0)).roundToInt().coerceIn(0, 100)

    private fun confidenceAdjusted(measured: Int, confidence: Int): Int =
        (50 + (measured - 50) * (confidence / 100.0)).roundToInt().coerceIn(0, 100)

    private fun averageInt(values: List<Int>): Int = if (values.isEmpty()) 0 else values.average().roundToInt()
}

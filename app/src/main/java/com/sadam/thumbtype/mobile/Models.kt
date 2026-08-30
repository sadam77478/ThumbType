package com.sadam.thumbtype.mobile

import kotlin.math.roundToInt

enum class AppScreen { Onboarding, Home, Learn, Practice, Progress, Profile, Privacy, Trainer, Results }
enum class ThumbSide { LEFT, RIGHT, FLEX }
enum class CoachLevel { FULL, MINIMAL, OFF }
enum class TrainingFocus { BALANCED, SPEED, ACCURACY, RHYTHM }
enum class KeyboardLayer { LETTERS, NUMBERS }

data class Lesson(
    val id: Int,
    val stage: Int,
    val title: String,
    val subtitle: String,
    val text: String,
    val skill: String,
    val xp: Int = 40,
    val timeLimitSeconds: Int? = null,
    val isPractice: Boolean = false
)

data class AppSettings(
    val darkMode: Boolean = false,
    val haptics: Boolean = true,
    val sounds: Boolean = false,
    val reducedMotion: Boolean = false,
    val largeText: Boolean = false,
    val privacyScreenProtection: Boolean = false,
    val coachLevel: CoachLevel = CoachLevel.FULL
)

data class UserProfile(
    val targetWpm: Int = 50,
    val targetAccuracy: Int = 97,
    val dailyGoalMinutes: Int = 10,
    val focus: TrainingFocus = TrainingFocus.BALANCED,
    val baselineWpm: Int = 0,
    val baselineAccuracy: Int = 0
)

/**
 * Per-key analytics.
 *
 * attempts = every key press made while this key was expected.
 * correct = attempts that entered the expected key.
 * errors = attempts that entered a different key.
 * totalCorrectReactionMs = completion latency accumulated only for successful presses.
 *
 * Keeping attempts separate from correct fixes the old accuracy bug where errors were
 * subtracted from a value that already represented successful presses.
 */
data class KeyAggregate(
    val attempts: Int = 0,
    val correct: Int = 0,
    val errors: Int = 0,
    val totalCorrectReactionMs: Long = 0L
) {
    val accuracy: Int
        get() = if (attempts <= 0) 100 else (correct * 100.0 / attempts).roundToInt().coerceIn(0, 100)

    val errorRate: Int
        get() = if (attempts <= 0) 0 else (errors * 100.0 / attempts).roundToInt().coerceIn(0, 100)

    val averageReactionMs: Int
        get() = if (correct <= 0) 0 else (totalCorrectReactionMs / correct).toInt()
}

/**
 * Per-transition analytics for an intended adjacent key pair.
 *
 * attempts includes both successful and failed attempts at the second key.
 * Successful transition latency is measured from the preceding correctly completed key
 * through the final correct completion, so correction time is included when mistakes occur.
 */
data class TransitionAggregate(
    val attempts: Int = 0,
    val correct: Int = 0,
    val errors: Int = 0,
    val totalSuccessfulMs: Long = 0L
) {
    val averageMs: Int
        get() = if (correct <= 0) 0 else (totalSuccessfulMs / correct).toInt()

    val accuracy: Int
        get() = if (attempts <= 0) 100 else (correct * 100.0 / attempts).roundToInt().coerceIn(0, 100)

    val errorRate: Int
        get() = if (attempts <= 0) 0 else (errors * 100.0 / attempts).roundToInt().coerceIn(0, 100)
}

/** One physical character-key attempt in the trainer. */
data class PressEvent(
    val expected: Char,
    val entered: Char,
    val correct: Boolean,
    val elapsedFromPreviousMs: Long,
    val recommended: ThumbSide,
    val touchSide: ThumbSide,
    val targetIndex: Int = 0
)

data class SessionResult(
    val title: String,
    val rawWpm: Int,
    val netWpm: Int,
    val accuracy: Int,
    val rhythm: Int,
    val consistency: Int,
    val thumbTechnique: Int,
    val thumbBalance: Int,
    val thumbScore: Int,
    val mistakes: Int,
    val correctedErrors: Int,
    val uncorrectedErrors: Int,
    val attempts: Int,
    val chars: Int,
    val durationMs: Long,
    val leftTouches: Int,
    val rightTouches: Int,
    val weakKey: Char?,
    val weakTransition: String?,
    val keyUpdates: Map<Char, KeyAggregate>,
    val transitionUpdates: Map<String, TransitionAggregate>
)

data class HistoryEntry(
    val epochMs: Long,
    val wpm: Int,
    val accuracy: Int,
    val thumbScore: Int,
    val minutes: Int,
    val title: String
)

data class PracticeMode(
    val name: String,
    val subtitle: String,
    val text: String,
    val timeLimitSeconds: Int? = null,
    val skill: String = name
)

data class DailyWorkoutItem(
    val title: String,
    val subtitle: String,
    val minutes: Int,
    val lesson: Lesson
)

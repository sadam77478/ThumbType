package com.sadam.thumbtype.mobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingIntelligenceTest {

    private fun historyEntry(
        index: Int,
        wpm: Int,
        accuracy: Int,
        score: Int
    ) = HistoryEntry(
        epochMs = index.toLong(),
        wpm = wpm,
        accuracy = accuracy,
        thumbScore = score,
        minutes = 1,
        title = "Session $index"
    )

    @Test
    fun noHistoryStartsWithFoundationPriority() {
        val result = TrainingIntelligenceEngine.analyze(
            profile = UserProfile(),
            history = emptyList(),
            keyStats = emptyMap(),
            transitionStats = emptyMap()
        )

        assertEquals(TrainingPriority.FOUNDATION, result.priority)
        assertEquals(1, result.difficultyLevel)
        assertFalse(result.trend.plateauDetected)
    }

    @Test
    fun lowRecentAccuracyBecomesFirstPriority() {
        val history = (1..5).map { historyEntry(it, 42, 89, 520) }
        val result = TrainingIntelligenceEngine.analyze(
            profile = UserProfile(targetWpm = 50, targetAccuracy = 97),
            history = history,
            keyStats = emptyMap(),
            transitionStats = emptyMap()
        )

        assertEquals(TrainingPriority.ACCURACY, result.priority)
        assertEquals(89, result.recentAccuracy)
    }

    @Test
    fun confidentWeakTransitionBecomesTarget() {
        val history = (1..5).map { historyEntry(it, 52, 98, 760) }
        val transitions = mapOf(
            "gh" to TransitionAggregate(
                attempts = 60,
                correct = 42,
                errors = 18,
                totalSuccessfulMs = 35_700L
            )
        )

        val result = TrainingIntelligenceEngine.analyze(
            profile = UserProfile(targetWpm = 50, targetAccuracy = 97),
            history = history,
            keyStats = emptyMap(),
            transitionStats = transitions
        )

        assertEquals(TrainingPriority.TRANSITIONS, result.priority)
        assertTrue(result.weakestTransitions.first().confidence >= 20)
        assertTrue(result.targetedDrill.contains("gh"))
    }

    @Test
    fun plateauIsDetectedAcrossTwoStableWindows() {
        val history = listOf(
            historyEntry(1, 48, 97, 720),
            historyEntry(2, 49, 97, 728),
            historyEntry(3, 48, 98, 724),
            historyEntry(4, 49, 97, 730),
            historyEntry(5, 48, 97, 726),
            historyEntry(6, 49, 97, 731),
            historyEntry(7, 49, 98, 733),
            historyEntry(8, 49, 97, 729),
            historyEntry(9, 50, 97, 735),
            historyEntry(10, 49, 98, 734)
        )

        val result = TrainingIntelligenceEngine.analyze(
            profile = UserProfile(targetWpm = 50, targetAccuracy = 97, focus = TrainingFocus.SPEED),
            history = history,
            keyStats = emptyMap(),
            transitionStats = emptyMap()
        )

        assertTrue(result.trend.hasComparison)
        assertTrue(result.trend.plateauDetected)
        assertEquals(TrainingPriority.SPEED, result.priority)
    }

    @Test
    fun keyMasteryUsesConfidenceSoTinySamplesDoNotLookElite() {
        val tiny = TrainingIntelligenceEngine.keyMastery(
            'a',
            KeyAggregate(attempts = 2, correct = 2, errors = 0, totalCorrectReactionMs = 300L)
        )
        val established = TrainingIntelligenceEngine.keyMastery(
            'a',
            KeyAggregate(attempts = 100, correct = 100, errors = 0, totalCorrectReactionMs = 15_000L)
        )

        assertTrue(established.confidence > tiny.confidence)
        assertTrue(established.score > tiny.score)
    }

    @Test
    fun adaptiveWorkoutMatchesDailyGoalExactly() {
        val intelligence = TrainingIntelligenceProfile(
            priority = TrainingPriority.CENTER_REACH,
            headline = "Center reach",
            difficultyLevel = 6,
            targetedDrill = "thing bring tonight center reach"
        )
        val profile = UserProfile(dailyGoalMinutes = 15)

        val workout = TrainingIntelligenceEngine.buildAdaptiveWorkout(profile, intelligence)

        assertEquals(4, workout.size)
        assertEquals(15, workout.sumOf { it.minutes })
        assertTrue(workout.any { it.lesson.skill == "Center reach" })
    }
}

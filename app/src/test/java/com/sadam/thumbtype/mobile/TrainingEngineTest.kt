package com.sadam.thumbtype.mobile

import org.junit.Assert.assertEquals
import org.junit.Test

class TrainingEngineTest {

    private fun event(
        index: Int,
        expected: Char,
        entered: Char = expected,
        gapMs: Long = 200L,
        recommended: ThumbSide = ThumbSide.LEFT,
        touchSide: ThumbSide = ThumbSide.LEFT
    ) = PressEvent(
        expected = expected,
        entered = entered,
        correct = entered == expected,
        elapsedFromPreviousMs = gapMs,
        recommended = recommended,
        touchSide = touchSide,
        targetIndex = index
    )

    @Test
    fun keyAggregateAccuracyUsesAttemptsAsDenominator() {
        val stat = KeyAggregate(
            attempts = 10,
            correct = 8,
            errors = 2,
            totalCorrectReactionMs = 1_600L
        )

        assertEquals(80, stat.accuracy)
        assertEquals(20, stat.errorRate)
        assertEquals(200, stat.averageReactionMs)
    }

    @Test
    fun transitionAggregateAccuracyUsesAttemptsAsDenominator() {
        val stat = TransitionAggregate(
            attempts = 4,
            correct = 3,
            errors = 1,
            totalSuccessfulMs = 900L
        )

        assertEquals(75, stat.accuracy)
        assertEquals(25, stat.errorRate)
        assertEquals(300, stat.averageMs)
    }

    @Test
    fun perfectSessionProducesExpectedGrossAndNetWpm() {
        val events = List(25) { index ->
            event(index = index, expected = ('a'.code + index % 20).toChar(), gapMs = 2_400L)
        }

        val result = TrainingEngine.calculateResult(
            title = "Perfect",
            events = events,
            durationMs = 60_000L,
            targetWpm = 50
        )

        assertEquals(5, result.rawWpm)
        assertEquals(5, result.netWpm)
        assertEquals(100, result.accuracy)
        assertEquals(0, result.mistakes)
        assertEquals(0, result.correctedErrors)
        assertEquals(0, result.uncorrectedErrors)
        assertEquals(25, result.attempts)
        assertEquals(25, result.chars)
    }

    @Test
    fun grossWpmCountsAttemptsAndNetWpmPenalizesMistakes() {
        val events = List(50) { index ->
            val expected = if (index % 2 == 0) 'a' else 'l'
            val entered = if (index < 5) 'x' else expected
            event(index = index, expected = expected, entered = entered, gapMs = 1_200L)
        }

        val result = TrainingEngine.calculateResult(
            title = "Mixed",
            events = events,
            durationMs = 60_000L,
            targetWpm = 50
        )

        assertEquals(10, result.rawWpm)
        assertEquals(5, result.netWpm)
        assertEquals(90, result.accuracy)
        assertEquals(5, result.mistakes)
        assertEquals(0, result.correctedErrors)
        assertEquals(5, result.uncorrectedErrors)
        assertEquals(50, result.attempts)
        assertEquals(45, result.chars)
    }

    @Test
    fun recoveredAndUnresolvedMistakesAreSeparatedByTargetPosition() {
        val events = listOf(
            event(index = 0, expected = 'a', gapMs = 100L),
            event(index = 1, expected = 'b', entered = 'x', gapMs = 200L),
            event(index = 1, expected = 'b', gapMs = 300L),
            event(index = 2, expected = 'c', entered = 'v', gapMs = 250L)
        )

        val result = TrainingEngine.calculateResult(
            title = "Recovery",
            events = events,
            durationMs = 60_000L,
            targetWpm = 50
        )

        assertEquals(2, result.mistakes)
        assertEquals(1, result.correctedErrors)
        assertEquals(1, result.uncorrectedErrors)
        assertEquals(50, result.accuracy)
    }

    @Test
    fun transitionErrorsAreActuallyRecorded() {
        val events = listOf(
            event(index = 0, expected = 'a', gapMs = 100L),
            event(index = 1, expected = 'b', entered = 'x', gapMs = 200L),
            event(index = 1, expected = 'b', gapMs = 300L)
        )

        val result = TrainingEngine.calculateResult(
            title = "Transition",
            events = events,
            durationMs = 1_000L,
            targetWpm = 50
        )

        val transition = result.transitionUpdates.getValue("ab")
        assertEquals(2, transition.attempts)
        assertEquals(1, transition.correct)
        assertEquals(1, transition.errors)
        assertEquals(50, transition.accuracy)
        assertEquals(500, transition.averageMs)

        val b = result.keyUpdates.getValue('b')
        assertEquals(2, b.attempts)
        assertEquals(1, b.correct)
        assertEquals(1, b.errors)
        assertEquals(50, b.accuracy)
        assertEquals(500, b.averageReactionMs)
    }

    @Test
    fun emptySessionCannotProduceAHighThumbScore() {
        val result = TrainingEngine.calculateResult(
            title = "Empty",
            events = emptyList(),
            durationMs = 60_000L,
            targetWpm = 50
        )

        assertEquals(0, result.rawWpm)
        assertEquals(0, result.netWpm)
        assertEquals(100, result.accuracy)
        assertEquals(0, result.thumbScore)
        assertEquals(0, result.attempts)
    }

    @Test
    fun rhythmAndConsistencyRewardEvenTiming() {
        val even = listOf(200L, 200L, 200L, 200L, 200L)
        val uneven = listOf(80L, 600L, 120L, 900L, 100L)

        assertEquals(100, TrainingEngine.rhythmScore(even))
        assertEquals(100, TrainingEngine.consistencyScore(even))
        check(TrainingEngine.rhythmScore(uneven) < 100)
        check(TrainingEngine.consistencyScore(uneven) < 100)
    }
}

package com.sadam.thumbtype.mobile.app

import com.sadam.thumbtype.mobile.AppSettings
import com.sadam.thumbtype.mobile.DailyWorkoutItem
import com.sadam.thumbtype.mobile.HistoryEntry
import com.sadam.thumbtype.mobile.KeyAggregate
import com.sadam.thumbtype.mobile.Lesson
import com.sadam.thumbtype.mobile.SessionResult
import com.sadam.thumbtype.mobile.TransitionAggregate
import com.sadam.thumbtype.mobile.UserProfile
import com.sadam.thumbtype.mobile.data.repository.ThumbTypeRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThumbTypeReadModelsPerformanceTest {
    @Test
    fun readModels_readsEachBackingDatasetOnce_andDerivesSecondaryDataLocally() = runBlocking {
        val repository = CountingRepository()

        val models = repository.readModels()

        assertEquals(1, repository.profileReads)
        assertEquals(1, repository.completedReads)
        assertEquals(1, repository.keyReads)
        assertEquals(1, repository.transitionReads)
        assertEquals(1, repository.historyReads)
        assertEquals(1, repository.bestWpmReads)
        assertEquals(1, repository.xpReads)
        assertEquals(1, repository.todayReads)
        assertEquals(1, repository.streakReads)
        assertEquals(1, repository.totalCharacterReads)

        assertEquals("th", models.progress.weakTransitions.first().first)
        assertTrue(models.progress.achievements.first { it.first == "ThumbScore 800" }.second)
        assertEquals(2, models.progress.history.size)
        assertEquals(98, models.home.lastAccuracy)
    }

    private class CountingRepository : ThumbTypeRepository {
        var profileReads = 0
        var completedReads = 0
        var keyReads = 0
        var transitionReads = 0
        var historyReads = 0
        var bestWpmReads = 0
        var xpReads = 0
        var todayReads = 0
        var streakReads = 0
        var totalCharacterReads = 0

        override suspend fun isOnboarded() = true
        override suspend fun setOnboarded(value: Boolean) = Unit
        override suspend fun settings() = AppSettings()
        override suspend fun saveSettings(value: AppSettings) = Unit

        override suspend fun profile(): UserProfile {
            profileReads++
            return UserProfile(targetWpm = 50, targetAccuracy = 97, dailyGoalMinutes = 10)
        }

        override suspend fun saveProfile(value: UserProfile) = Unit

        override suspend fun xp(): Int {
            xpReads++
            return 1200
        }

        override suspend fun totalCharacters(): Int {
            totalCharacterReads++
            return 12_000
        }

        override suspend fun totalSeconds() = 1200L

        override suspend fun bestWpm(): Int {
            bestWpmReads++
            return 55
        }

        override suspend fun streak(): Int {
            streakReads++
            return 8
        }

        override suspend fun completedLessonIds(): Set<Int> {
            completedReads++
            return (1..20).toSet()
        }

        override suspend fun completionCount(): Int = error("readModels must derive completion count from completedLessonIds")

        override suspend fun history(): List<HistoryEntry> {
            historyReads++
            return listOf(
                HistoryEntry(1L, 45, 96, 700, 1, "Session 1"),
                HistoryEntry(2L, 50, 98, 820, 1, "Session 2")
            )
        }

        override suspend fun keyStats(): Map<Char, KeyAggregate> {
            keyReads++
            return mapOf('a' to KeyAggregate(attempts = 10, correct = 9, errors = 1, totalCorrectReactionMs = 1800L))
        }

        override suspend fun transitionStats(): Map<String, TransitionAggregate> {
            transitionReads++
            return mapOf(
                "th" to TransitionAggregate(attempts = 10, correct = 7, errors = 3, totalSuccessfulMs = 3500L),
                "he" to TransitionAggregate(attempts = 10, correct = 10, errors = 0, totalSuccessfulMs = 1000L)
            )
        }

        override suspend fun todaySeconds(): Long {
            todayReads++
            return 300L
        }

        override suspend fun currentThumbScore() = 820
        override suspend fun lastWpm() = 50
        override suspend fun lastAccuracy() = 98
        override suspend fun saveSession(result: SessionResult, lesson: Lesson) = Unit
        override suspend fun topWeakKeys(limit: Int) = error("readModels must not issue a second key-stat aggregate read")
        override suspend fun topWeakTransitions(limit: Int) = error("readModels must derive weak transitions from its existing transition snapshot")
        override suspend fun dailyWorkout(): List<DailyWorkoutItem> = error("readModels uses the deterministic intelligence workout builder")
        override suspend fun achievements(): List<Pair<String, Boolean>> = error("readModels must derive achievements from its existing snapshot")
        override suspend fun exportJson() = "{}"
        override suspend fun importJson(raw: String) = Result.success(Unit)
        override suspend fun clearAll() = Unit
    }
}

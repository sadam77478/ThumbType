package com.sadam.thumbtype.mobile.data.repository

import android.content.Context
import com.sadam.thumbtype.mobile.AppSettings
import com.sadam.thumbtype.mobile.DailyWorkoutItem
import com.sadam.thumbtype.mobile.HistoryEntry
import com.sadam.thumbtype.mobile.KeyAggregate
import com.sadam.thumbtype.mobile.Lesson
import com.sadam.thumbtype.mobile.SessionResult
import com.sadam.thumbtype.mobile.TransitionAggregate
import com.sadam.thumbtype.mobile.UserProfile

/**
 * Application-facing asynchronous data contract.
 *
 * Persistence is now backed by Room and DataStore, so repository operations are suspend
 * functions and must never require database or disk work on the Compose UI path.
 */
interface ThumbTypeRepository {
    suspend fun isOnboarded(): Boolean
    suspend fun setOnboarded(value: Boolean)

    suspend fun settings(): AppSettings
    suspend fun saveSettings(value: AppSettings)

    suspend fun profile(): UserProfile
    suspend fun saveProfile(value: UserProfile)

    suspend fun xp(): Int
    suspend fun totalCharacters(): Int
    suspend fun totalSeconds(): Long
    suspend fun bestWpm(): Int
    suspend fun streak(): Int
    suspend fun completedLessonIds(): Set<Int>
    suspend fun completionCount(): Int
    suspend fun history(): List<HistoryEntry>
    suspend fun keyStats(): Map<Char, KeyAggregate>
    suspend fun transitionStats(): Map<String, TransitionAggregate>
    suspend fun todaySeconds(): Long
    suspend fun currentThumbScore(): Int
    suspend fun lastWpm(): Int
    suspend fun lastAccuracy(): Int

    suspend fun saveSession(result: SessionResult, lesson: Lesson)
    suspend fun topWeakKeys(limit: Int = 6): List<Pair<Char, KeyAggregate>>
    suspend fun topWeakTransitions(limit: Int = 6): List<Pair<String, TransitionAggregate>>
    suspend fun dailyWorkout(): List<DailyWorkoutItem>
    suspend fun achievements(): List<Pair<String, Boolean>>

    suspend fun exportJson(): String
    suspend fun importJson(raw: String): Result<Unit>
    suspend fun clearAll()
}

/** Stable construction name retained so application wiring does not leak storage details. */
class DefaultThumbTypeRepository(context: Context) : ThumbTypeRepository by StructuredThumbTypeRepository(context)

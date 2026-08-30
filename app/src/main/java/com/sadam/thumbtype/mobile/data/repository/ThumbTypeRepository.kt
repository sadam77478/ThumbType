package com.sadam.thumbtype.mobile.data.repository

import android.content.Context
import com.sadam.thumbtype.mobile.AppRepository
import com.sadam.thumbtype.mobile.AppSettings
import com.sadam.thumbtype.mobile.DailyWorkoutItem
import com.sadam.thumbtype.mobile.HistoryEntry
import com.sadam.thumbtype.mobile.KeyAggregate
import com.sadam.thumbtype.mobile.Lesson
import com.sadam.thumbtype.mobile.SessionResult
import com.sadam.thumbtype.mobile.TransitionAggregate
import com.sadam.thumbtype.mobile.UserProfile

/**
 * Application-facing data contract.
 *
 * UI and ViewModels depend on this contract instead of the current SharedPreferences
 * implementation. This keeps the existing V3 storage working while allowing a future
 * Room/DataStore-backed repository to replace it without rewriting feature screens.
 */
interface ThumbTypeRepository {
    fun isOnboarded(): Boolean
    fun setOnboarded(value: Boolean)

    fun settings(): AppSettings
    fun saveSettings(value: AppSettings)

    fun profile(): UserProfile
    fun saveProfile(value: UserProfile)

    fun xp(): Int
    fun totalCharacters(): Int
    fun totalSeconds(): Long
    fun bestWpm(): Int
    fun streak(): Int
    fun completedLessonIds(): Set<Int>
    fun completionCount(): Int
    fun history(): List<HistoryEntry>
    fun keyStats(): Map<Char, KeyAggregate>
    fun transitionStats(): Map<String, TransitionAggregate>
    fun todaySeconds(): Long
    fun currentThumbScore(): Int
    fun lastWpm(): Int
    fun lastAccuracy(): Int

    fun saveSession(result: SessionResult, lesson: Lesson)
    fun topWeakKeys(limit: Int = 6): List<Pair<Char, KeyAggregate>>
    fun topWeakTransitions(limit: Int = 6): List<Pair<String, TransitionAggregate>>
    fun dailyWorkout(): List<DailyWorkoutItem>
    fun achievements(): List<Pair<String, Boolean>>

    fun exportJson(): String
    fun importJson(raw: String): Result<Unit>
    fun clearAll()
}

/**
 * Adapter around the verified V3 local repository.
 *
 * Keeping this adapter separate lets us introduce Room/DataStore later behind the same
 * application contract instead of coupling feature code to SharedPreferences.
 */
class DefaultThumbTypeRepository(context: Context) : ThumbTypeRepository {
    private val local = AppRepository(context.applicationContext)

    override fun isOnboarded() = local.isOnboarded()
    override fun setOnboarded(value: Boolean) = local.setOnboarded(value)
    override fun settings() = local.settings()
    override fun saveSettings(value: AppSettings) = local.saveSettings(value)
    override fun profile() = local.profile()
    override fun saveProfile(value: UserProfile) = local.saveProfile(value)
    override fun xp() = local.xp()
    override fun totalCharacters() = local.totalCharacters()
    override fun totalSeconds() = local.totalSeconds()
    override fun bestWpm() = local.bestWpm()
    override fun streak() = local.streak()
    override fun completedLessonIds() = local.completedLessonIds()
    override fun completionCount() = local.completionCount()
    override fun history() = local.history()
    override fun keyStats() = local.keyStats()
    override fun transitionStats() = local.transitionStats()
    override fun todaySeconds() = local.todaySeconds()
    override fun currentThumbScore() = local.currentThumbScore()
    override fun lastWpm() = local.lastWpm()
    override fun lastAccuracy() = local.lastAccuracy()
    override fun saveSession(result: SessionResult, lesson: Lesson) = local.saveSession(result, lesson)
    override fun topWeakKeys(limit: Int) = local.topWeakKeys(limit)
    override fun topWeakTransitions(limit: Int) = local.topWeakTransitions(limit)
    override fun dailyWorkout() = local.dailyWorkout()
    override fun achievements() = local.achievements()
    override fun exportJson() = local.exportJson()
    override fun importJson(raw: String) = local.importJson(raw)
    override fun clearAll() = local.clearAll()
}

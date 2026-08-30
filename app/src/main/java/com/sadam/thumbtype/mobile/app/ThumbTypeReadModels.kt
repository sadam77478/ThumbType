package com.sadam.thumbtype.mobile.app

import com.sadam.thumbtype.mobile.DailyWorkoutItem
import com.sadam.thumbtype.mobile.HistoryEntry
import com.sadam.thumbtype.mobile.KeyAggregate
import com.sadam.thumbtype.mobile.TrainingEngine
import com.sadam.thumbtype.mobile.TransitionAggregate
import com.sadam.thumbtype.mobile.UserProfile
import com.sadam.thumbtype.mobile.data.repository.ThumbTypeRepository

data class HomeUiData(
    val profile: UserProfile = UserProfile(),
    val completedLessonIds: Set<Int> = emptySet(),
    val workout: List<DailyWorkoutItem> = emptyList(),
    val todaySeconds: Long = 0L,
    val thumbScore: Int = 0,
    val streak: Int = 0,
    val lastWpm: Int = 0,
    val lastAccuracy: Int = 0,
    val bestWpm: Int = 0,
    val xp: Int = 0,
    val totalCharacters: Int = 0
)

data class LearnUiData(
    val completedLessonIds: Set<Int> = emptySet()
)

data class PracticeUiData(
    val weakDrill: String = ""
)

data class ProgressUiData(
    val history: List<HistoryEntry> = emptyList(),
    val keyStats: Map<Char, KeyAggregate> = emptyMap(),
    val weakTransitions: List<Pair<String, TransitionAggregate>> = emptyList(),
    val profile: UserProfile = UserProfile(),
    val lastWpm: Int = 0,
    val bestWpm: Int = 0,
    val thumbScore: Int = 0,
    val achievements: List<Pair<String, Boolean>> = emptyList()
)

data class ProfileUiData(
    val xp: Int = 0
)

data class ThumbTypeReadModels(
    val home: HomeUiData = HomeUiData(),
    val learn: LearnUiData = LearnUiData(),
    val practice: PracticeUiData = PracticeUiData(),
    val progress: ProgressUiData = ProgressUiData(),
    val profile: ProfileUiData = ProfileUiData()
)

fun ThumbTypeRepository.readModels(): ThumbTypeReadModels {
    val profile = profile()
    val completed = completedLessonIds()
    val keys = keyStats()
    val transitions = transitionStats()
    val history = history()

    return ThumbTypeReadModels(
        home = HomeUiData(
            profile = profile,
            completedLessonIds = completed,
            workout = dailyWorkout(),
            todaySeconds = todaySeconds(),
            thumbScore = history.lastOrNull()?.thumbScore ?: 0,
            streak = streak(),
            lastWpm = history.lastOrNull()?.wpm ?: profile.baselineWpm,
            lastAccuracy = history.lastOrNull()?.accuracy ?: profile.baselineAccuracy,
            bestWpm = bestWpm(),
            xp = xp(),
            totalCharacters = totalCharacters()
        ),
        learn = LearnUiData(completedLessonIds = completed),
        practice = PracticeUiData(
            weakDrill = TrainingEngine.generateWeakDrill(keys, transitions)
        ),
        progress = ProgressUiData(
            history = history,
            keyStats = keys,
            weakTransitions = topWeakTransitions(),
            profile = profile,
            lastWpm = history.lastOrNull()?.wpm ?: profile.baselineWpm,
            bestWpm = bestWpm(),
            thumbScore = history.lastOrNull()?.thumbScore ?: 0,
            achievements = achievements()
        ),
        profile = ProfileUiData(xp = xp())
    )
}

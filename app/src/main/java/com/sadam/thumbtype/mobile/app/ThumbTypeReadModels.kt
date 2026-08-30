package com.sadam.thumbtype.mobile.app

import androidx.compose.runtime.Immutable
import com.sadam.thumbtype.mobile.DailyWorkoutItem
import com.sadam.thumbtype.mobile.HistoryEntry
import com.sadam.thumbtype.mobile.KeyAggregate
import com.sadam.thumbtype.mobile.LessonRepository
import com.sadam.thumbtype.mobile.TrainingIntelligenceEngine
import com.sadam.thumbtype.mobile.TrainingIntelligenceProfile
import com.sadam.thumbtype.mobile.TransitionAggregate
import com.sadam.thumbtype.mobile.UserProfile
import com.sadam.thumbtype.mobile.data.repository.ThumbTypeRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

@Immutable
data class HomeUiData(
    val profile: UserProfile = UserProfile(),
    val completedLessonIds: Set<Int> = emptySet(),
    val workout: List<DailyWorkoutItem> = emptyList(),
    val intelligence: TrainingIntelligenceProfile = TrainingIntelligenceProfile(),
    val todaySeconds: Long = 0L,
    val thumbScore: Int = 0,
    val streak: Int = 0,
    val lastWpm: Int = 0,
    val lastAccuracy: Int = 0,
    val bestWpm: Int = 0,
    val xp: Int = 0,
    val totalCharacters: Int = 0
)

@Immutable
data class LearnUiData(
    val completedLessonIds: Set<Int> = emptySet()
)

@Immutable
data class PracticeUiData(
    val weakDrill: String = "",
    val intelligence: TrainingIntelligenceProfile = TrainingIntelligenceProfile()
)

@Immutable
data class ProgressUiData(
    val history: List<HistoryEntry> = emptyList(),
    val keyStats: Map<Char, KeyAggregate> = emptyMap(),
    val weakTransitions: List<Pair<String, TransitionAggregate>> = emptyList(),
    val intelligence: TrainingIntelligenceProfile = TrainingIntelligenceProfile(),
    val profile: UserProfile = UserProfile(),
    val lastWpm: Int = 0,
    val bestWpm: Int = 0,
    val thumbScore: Int = 0,
    val achievements: List<Pair<String, Boolean>> = emptyList()
)

@Immutable
data class ProfileUiData(
    val xp: Int = 0
)

@Immutable
data class ThumbTypeReadModels(
    val home: HomeUiData = HomeUiData(),
    val learn: LearnUiData = LearnUiData(),
    val practice: PracticeUiData = PracticeUiData(),
    val progress: ProgressUiData = ProgressUiData(),
    val profile: ProfileUiData = ProfileUiData()
)

/**
 * Builds one consistent UI snapshot with one read per backing dataset.
 *
 * V4.4 deliberately derives weak transitions and achievements from the already-loaded
 * snapshot instead of calling repository helpers that would read Room/DataStore again.
 * Independent reads are started together; callers still keep this work off the main thread.
 */
suspend fun ThumbTypeRepository.readModels(): ThumbTypeReadModels = coroutineScope {
    val profileDeferred = async { profile() }
    val completedDeferred = async { completedLessonIds() }
    val keysDeferred = async { keyStats() }
    val transitionsDeferred = async { transitionStats() }
    val historyDeferred = async { history() }
    val bestWpmDeferred = async { bestWpm() }
    val xpDeferred = async { xp() }
    val todayDeferred = async { todaySeconds() }
    val streakDeferred = async { streak() }
    val totalCharactersDeferred = async { totalCharacters() }

    val profile = profileDeferred.await()
    val completed = completedDeferred.await()
    val keys = keysDeferred.await()
    val transitions = transitionsDeferred.await()
    val history = historyDeferred.await()
    val bestWpm = bestWpmDeferred.await()
    val xp = xpDeferred.await()
    val todaySeconds = todayDeferred.await()
    val streak = streakDeferred.await()
    val totalCharacters = totalCharactersDeferred.await()

    val intelligence = TrainingIntelligenceEngine.analyze(
        profile = profile,
        history = history,
        keyStats = keys,
        transitionStats = transitions
    )
    val adaptiveWorkout = TrainingIntelligenceEngine.buildAdaptiveWorkout(profile, intelligence)
    val last = history.lastOrNull()

    val weakTransitions = transitions.entries
        .asSequence()
        .filter { it.value.attempts >= 2 }
        .sortedByDescending { it.value.errorRate * 14 + it.value.averageMs }
        .take(6)
        .map { it.key to it.value }
        .toList()

    val bestAccuracy = history.maxOfOrNull { it.accuracy } ?: 0
    val bestScore = history.maxOfOrNull { it.thumbScore } ?: 0
    val achievements = listOf(
        "First session" to history.isNotEmpty(),
        "30 WPM" to (bestWpm >= 30),
        "40 WPM" to (bestWpm >= 40),
        "50 WPM Club" to (bestWpm >= 50),
        "98% Accuracy" to (bestAccuracy >= 98),
        "Perfect Accuracy" to (bestAccuracy == 100),
        "7 Day Streak" to (streak >= 7),
        "10K Characters" to (totalCharacters >= 10_000),
        "ThumbScore 800" to (bestScore >= 800),
        "Curriculum 50%" to (completed.size >= LessonRepository.lessons.size / 2)
    )

    ThumbTypeReadModels(
        home = HomeUiData(
            profile = profile,
            completedLessonIds = completed,
            workout = adaptiveWorkout,
            intelligence = intelligence,
            todaySeconds = todaySeconds,
            thumbScore = last?.thumbScore ?: 0,
            streak = streak,
            lastWpm = last?.wpm ?: profile.baselineWpm,
            lastAccuracy = last?.accuracy ?: profile.baselineAccuracy,
            bestWpm = bestWpm,
            xp = xp,
            totalCharacters = totalCharacters
        ),
        learn = LearnUiData(completedLessonIds = completed),
        practice = PracticeUiData(
            weakDrill = intelligence.targetedDrill,
            intelligence = intelligence
        ),
        progress = ProgressUiData(
            history = history,
            keyStats = keys,
            weakTransitions = weakTransitions,
            intelligence = intelligence,
            profile = profile,
            lastWpm = last?.wpm ?: profile.baselineWpm,
            bestWpm = bestWpm,
            thumbScore = last?.thumbScore ?: 0,
            achievements = achievements
        ),
        profile = ProfileUiData(xp = xp)
    )
}

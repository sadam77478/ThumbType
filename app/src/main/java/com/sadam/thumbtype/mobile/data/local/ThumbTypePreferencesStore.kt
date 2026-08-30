package com.sadam.thumbtype.mobile.data.local

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sadam.thumbtype.mobile.AppSettings
import com.sadam.thumbtype.mobile.CoachLevel
import com.sadam.thumbtype.mobile.TrainingFocus
import com.sadam.thumbtype.mobile.UserProfile
import kotlinx.coroutines.flow.first

private val Context.thumbTypeDataStore by preferencesDataStore(name = "thumbtype_preferences_v1")

data class StoredPreferences(
    val migrationVersion: Int = 0,
    val onboarded: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val profile: UserProfile = UserProfile(),
    val xp: Int = 0,
    val totalCharacters: Int = 0,
    val totalSeconds: Long = 0L,
    val bestWpm: Int = 0,
    val streak: Int = 0,
    val lastPracticeDate: String = ""
)

class ThumbTypePreferencesStore(private val context: Context) {
    @Volatile
    private var cachedSnapshot: StoredPreferences? = null

    private object Keys {
        val migrationVersion = intPreferencesKey("migration_version")
        val onboarded = booleanPreferencesKey("onboarded")
        val darkMode = booleanPreferencesKey("dark_mode")
        val haptics = booleanPreferencesKey("haptics")
        val sounds = booleanPreferencesKey("sounds")
        val reducedMotion = booleanPreferencesKey("reduced_motion")
        val largeText = booleanPreferencesKey("large_text")
        val privacyScreenProtection = booleanPreferencesKey("privacy_screen_protection")
        val coachLevel = stringPreferencesKey("coach_level")
        val targetWpm = intPreferencesKey("target_wpm")
        val targetAccuracy = intPreferencesKey("target_accuracy")
        val dailyGoalMinutes = intPreferencesKey("daily_goal_minutes")
        val trainingFocus = stringPreferencesKey("training_focus")
        val baselineWpm = intPreferencesKey("baseline_wpm")
        val baselineAccuracy = intPreferencesKey("baseline_accuracy")
        val xp = intPreferencesKey("xp")
        val totalCharacters = intPreferencesKey("total_characters")
        val totalSeconds = longPreferencesKey("total_seconds")
        val bestWpm = intPreferencesKey("best_wpm")
        val streak = intPreferencesKey("streak")
        val lastPracticeDate = stringPreferencesKey("last_practice_date")
    }

    suspend fun snapshot(): StoredPreferences {
        cachedSnapshot?.let { return it }
        return context.thumbTypeDataStore.data.first().toStoredPreferences().also {
            cachedSnapshot = it
        }
    }

    suspend fun replace(value: StoredPreferences) {
        editAndCache { p -> writeAll(p, value) }
    }

    suspend fun setOnboarded(value: Boolean) {
        editAndCache { it[Keys.onboarded] = value }
    }

    suspend fun saveSettings(value: AppSettings) {
        editAndCache { p ->
            p[Keys.darkMode] = value.darkMode
            p[Keys.haptics] = value.haptics
            p[Keys.sounds] = value.sounds
            p[Keys.reducedMotion] = value.reducedMotion
            p[Keys.largeText] = value.largeText
            p[Keys.privacyScreenProtection] = value.privacyScreenProtection
            p[Keys.coachLevel] = value.coachLevel.name
        }
    }

    suspend fun saveProfile(value: UserProfile) {
        editAndCache { p ->
            p[Keys.targetWpm] = value.targetWpm.coerceIn(20, 120)
            p[Keys.targetAccuracy] = value.targetAccuracy.coerceIn(80, 100)
            p[Keys.dailyGoalMinutes] = value.dailyGoalMinutes.coerceIn(5, 60)
            p[Keys.trainingFocus] = value.focus.name
            p[Keys.baselineWpm] = value.baselineWpm.coerceAtLeast(0)
            p[Keys.baselineAccuracy] = value.baselineAccuracy.coerceIn(0, 100)
        }
    }

    suspend fun updateCounters(transform: (StoredPreferences) -> StoredPreferences) {
        editAndCache { p ->
            val updated = transform(p.toStoredPreferences())
            writeAll(p, updated)
        }
    }

    suspend fun clearKeepingMigrationMarker() {
        editAndCache { p ->
            p.clear()
            p[Keys.migrationVersion] = CURRENT_MIGRATION_VERSION
        }
    }

    private suspend fun editAndCache(transform: suspend (MutablePreferences) -> Unit) {
        val updated = context.thumbTypeDataStore.edit { preferences -> transform(preferences) }
        cachedSnapshot = updated.toStoredPreferences()
    }

    private fun Preferences.toStoredPreferences(): StoredPreferences {
        val settings = AppSettings(
            darkMode = this[Keys.darkMode] ?: false,
            haptics = this[Keys.haptics] ?: true,
            sounds = this[Keys.sounds] ?: false,
            reducedMotion = this[Keys.reducedMotion] ?: false,
            largeText = this[Keys.largeText] ?: false,
            privacyScreenProtection = this[Keys.privacyScreenProtection] ?: false,
            coachLevel = runCatching { CoachLevel.valueOf(this[Keys.coachLevel] ?: CoachLevel.FULL.name) }
                .getOrDefault(CoachLevel.FULL)
        )
        val profile = UserProfile(
            targetWpm = (this[Keys.targetWpm] ?: 50).coerceIn(20, 120),
            targetAccuracy = (this[Keys.targetAccuracy] ?: 97).coerceIn(80, 100),
            dailyGoalMinutes = (this[Keys.dailyGoalMinutes] ?: 10).coerceIn(5, 60),
            focus = runCatching { TrainingFocus.valueOf(this[Keys.trainingFocus] ?: TrainingFocus.BALANCED.name) }
                .getOrDefault(TrainingFocus.BALANCED),
            baselineWpm = (this[Keys.baselineWpm] ?: 0).coerceAtLeast(0),
            baselineAccuracy = (this[Keys.baselineAccuracy] ?: 0).coerceIn(0, 100)
        )
        return StoredPreferences(
            migrationVersion = this[Keys.migrationVersion] ?: 0,
            onboarded = this[Keys.onboarded] ?: false,
            settings = settings,
            profile = profile,
            xp = (this[Keys.xp] ?: 0).coerceAtLeast(0),
            totalCharacters = (this[Keys.totalCharacters] ?: 0).coerceAtLeast(0),
            totalSeconds = (this[Keys.totalSeconds] ?: 0L).coerceAtLeast(0L),
            bestWpm = (this[Keys.bestWpm] ?: 0).coerceAtLeast(0),
            streak = (this[Keys.streak] ?: 0).coerceAtLeast(0),
            lastPracticeDate = this[Keys.lastPracticeDate].orEmpty()
        )
    }

    private fun writeAll(p: MutablePreferences, value: StoredPreferences) {
        p[Keys.migrationVersion] = value.migrationVersion
        p[Keys.onboarded] = value.onboarded
        p[Keys.darkMode] = value.settings.darkMode
        p[Keys.haptics] = value.settings.haptics
        p[Keys.sounds] = value.settings.sounds
        p[Keys.reducedMotion] = value.settings.reducedMotion
        p[Keys.largeText] = value.settings.largeText
        p[Keys.privacyScreenProtection] = value.settings.privacyScreenProtection
        p[Keys.coachLevel] = value.settings.coachLevel.name
        p[Keys.targetWpm] = value.profile.targetWpm.coerceIn(20, 120)
        p[Keys.targetAccuracy] = value.profile.targetAccuracy.coerceIn(80, 100)
        p[Keys.dailyGoalMinutes] = value.profile.dailyGoalMinutes.coerceIn(5, 60)
        p[Keys.trainingFocus] = value.profile.focus.name
        p[Keys.baselineWpm] = value.profile.baselineWpm.coerceAtLeast(0)
        p[Keys.baselineAccuracy] = value.profile.baselineAccuracy.coerceIn(0, 100)
        p[Keys.xp] = value.xp.coerceIn(0, 10_000_000)
        p[Keys.totalCharacters] = value.totalCharacters.coerceAtLeast(0)
        p[Keys.totalSeconds] = value.totalSeconds.coerceIn(0L, 100_000_000L)
        p[Keys.bestWpm] = value.bestWpm.coerceIn(0, 300)
        p[Keys.streak] = value.streak.coerceIn(0, 10_000)
        p[Keys.lastPracticeDate] = value.lastPracticeDate
    }

    companion object {
        const val CURRENT_MIGRATION_VERSION = 1
    }
}

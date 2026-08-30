package com.sadam.thumbtype.mobile

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class AppRepository(context: Context) {
    private val prefs = context.getSharedPreferences("thumbtype_elite_v3", Context.MODE_PRIVATE)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun isOnboarded(): Boolean = prefs.getBoolean("onboarded", false)
    fun setOnboarded(value: Boolean) = prefs.edit().putBoolean("onboarded", value).apply()

    fun settings(): AppSettings {
        val raw = prefs.getString("settings", null) ?: return AppSettings()
        return runCatching {
            val o = JSONObject(raw)
            AppSettings(
                darkMode = o.optBoolean("darkMode", false),
                haptics = o.optBoolean("haptics", true),
                sounds = o.optBoolean("sounds", false),
                reducedMotion = o.optBoolean("reducedMotion", false),
                largeText = o.optBoolean("largeText", false),
                privacyScreenProtection = o.optBoolean("privacyScreenProtection", false),
                coachLevel = runCatching { CoachLevel.valueOf(o.optString("coachLevel", CoachLevel.FULL.name)) }.getOrDefault(CoachLevel.FULL)
            )
        }.getOrDefault(AppSettings())
    }

    fun saveSettings(value: AppSettings) {
        val o = JSONObject()
            .put("darkMode", value.darkMode)
            .put("haptics", value.haptics)
            .put("sounds", value.sounds)
            .put("reducedMotion", value.reducedMotion)
            .put("largeText", value.largeText)
            .put("privacyScreenProtection", value.privacyScreenProtection)
            .put("coachLevel", value.coachLevel.name)
        prefs.edit().putString("settings", o.toString()).apply()
    }

    fun profile(): UserProfile {
        val raw = prefs.getString("profile", null) ?: return UserProfile()
        return runCatching {
            val o = JSONObject(raw)
            UserProfile(
                targetWpm = o.optInt("targetWpm", 50).coerceIn(20, 120),
                targetAccuracy = o.optInt("targetAccuracy", 97).coerceIn(80, 100),
                dailyGoalMinutes = o.optInt("dailyGoalMinutes", 10).coerceIn(5, 60),
                focus = runCatching { TrainingFocus.valueOf(o.optString("focus", TrainingFocus.BALANCED.name)) }.getOrDefault(TrainingFocus.BALANCED),
                baselineWpm = o.optInt("baselineWpm", 0).coerceAtLeast(0),
                baselineAccuracy = o.optInt("baselineAccuracy", 0).coerceIn(0, 100)
            )
        }.getOrDefault(UserProfile())
    }

    fun saveProfile(value: UserProfile) {
        val o = JSONObject()
            .put("targetWpm", value.targetWpm)
            .put("targetAccuracy", value.targetAccuracy)
            .put("dailyGoalMinutes", value.dailyGoalMinutes)
            .put("focus", value.focus.name)
            .put("baselineWpm", value.baselineWpm)
            .put("baselineAccuracy", value.baselineAccuracy)
        prefs.edit().putString("profile", o.toString()).apply()
    }

    fun xp(): Int = prefs.getInt("xp", 0)
    fun totalCharacters(): Int = prefs.getInt("total_characters", 0)
    fun totalSeconds(): Long = prefs.getLong("total_seconds", 0L)
    fun bestWpm(): Int = prefs.getInt("best_wpm", 0)
    fun streak(): Int = prefs.getInt("streak", 0)

    fun completedLessonIds(): Set<Int> = prefs.getStringSet("completed_lessons", emptySet())
        ?.mapNotNull { it.toIntOrNull() }
        ?.toSet()
        ?: emptySet()

    fun completionCount(): Int = completedLessonIds().size

    fun history(): List<HistoryEntry> {
        val raw = prefs.getString("history", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        HistoryEntry(
                            epochMs = o.optLong("epochMs", 0L),
                            wpm = o.optInt("wpm", 0),
                            accuracy = o.optInt("accuracy", 0),
                            thumbScore = o.optInt("thumbScore", 0),
                            minutes = o.optInt("minutes", 0),
                            title = o.optString("title", "Session").take(80)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun keyStats(): Map<Char, KeyAggregate> {
        val raw = prefs.getString("key_stats", null) ?: return emptyMap()
        return runCatching {
            val o = JSONObject(raw)
            buildMap {
                o.keys().forEach { key ->
                    if (key.isNotEmpty()) {
                        val s = o.getJSONObject(key)
                        put(key[0], decodeKeyAggregate(s))
                    }
                }
            }
        }.getOrDefault(emptyMap())
    }

    fun transitionStats(): Map<String, TransitionAggregate> {
        val raw = prefs.getString("transition_stats", null) ?: return emptyMap()
        return runCatching {
            val o = JSONObject(raw)
            buildMap {
                o.keys().forEach { key ->
                    val s = o.getJSONObject(key)
                    put(key.take(4), decodeTransitionAggregate(s))
                }
            }
        }.getOrDefault(emptyMap())
    }

    fun todaySeconds(): Long = prefs.getLong("practice_${dateFormat.format(Date())}", 0L)

    fun currentThumbScore(): Int = history().lastOrNull()?.thumbScore ?: 0
    fun lastWpm(): Int = history().lastOrNull()?.wpm ?: profile().baselineWpm
    fun lastAccuracy(): Int = history().lastOrNull()?.accuracy ?: profile().baselineAccuracy

    fun saveSession(result: SessionResult, lesson: Lesson) {
        val editor = prefs.edit()
        editor.putInt("xp", xp() + lesson.xp)
        editor.putInt("total_characters", totalCharacters() + result.chars)
        editor.putLong("total_seconds", totalSeconds() + result.durationMs / 1000L)
        editor.putInt("best_wpm", maxOf(bestWpm(), result.netWpm))

        if (lesson.id > 0 && !lesson.isPractice) {
            val updated = completedLessonIds().toMutableSet().apply { add(lesson.id) }
            editor.putStringSet("completed_lessons", updated.map(Int::toString).toSet())
        }

        val today = dateFormat.format(Date())
        editor.putLong("practice_$today", todaySeconds() + result.durationMs / 1000L)
        updateStreak(editor, today)

        val hist = (history() + HistoryEntry(
            epochMs = System.currentTimeMillis(),
            wpm = result.netWpm,
            accuracy = result.accuracy,
            thumbScore = result.thumbScore,
            minutes = ((result.durationMs / 60_000.0).coerceAtLeast(0.1)).roundToInt(),
            title = result.title
        )).takeLast(60)
        editor.putString("history", historyToJson(hist).toString())

        val keys = keyStats().toMutableMap()
        result.keyUpdates.forEach { (key, update) ->
            val old = keys[key] ?: KeyAggregate()
            keys[key] = KeyAggregate(
                attempts = old.attempts + update.attempts,
                correct = old.correct + update.correct,
                errors = old.errors + update.errors,
                totalCorrectReactionMs = old.totalCorrectReactionMs + update.totalCorrectReactionMs
            )
        }
        editor.putString("key_stats", keyStatsToJson(keys).toString())

        val transitions = transitionStats().toMutableMap()
        result.transitionUpdates.forEach { (pair, update) ->
            val old = transitions[pair] ?: TransitionAggregate()
            transitions[pair] = TransitionAggregate(
                attempts = old.attempts + update.attempts,
                correct = old.correct + update.correct,
                errors = old.errors + update.errors,
                totalSuccessfulMs = old.totalSuccessfulMs + update.totalSuccessfulMs
            )
        }
        editor.putString("transition_stats", transitionStatsToJson(transitions).toString())

        editor.apply()

        if (lesson.id == 0) {
            val old = profile()
            saveProfile(old.copy(baselineWpm = result.netWpm, baselineAccuracy = result.accuracy))
            setOnboarded(true)
        }
    }

    private fun updateStreak(editor: android.content.SharedPreferences.Editor, today: String) {
        val last = prefs.getString("last_practice_date", null)
        if (last == today) return
        val yesterdayCalendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterday = dateFormat.format(yesterdayCalendar.time)
        val next = if (last == yesterday) streak() + 1 else 1
        editor.putInt("streak", next).putString("last_practice_date", today)
    }

    fun topWeakKeys(limit: Int = 6): List<Pair<Char, KeyAggregate>> = keyStats().entries
        .filter { it.key.isLetterOrDigit() && it.value.attempts >= 2 }
        .sortedByDescending { it.value.errorRate * 12 + it.value.averageReactionMs }
        .take(limit)
        .map { it.key to it.value }

    fun topWeakTransitions(limit: Int = 6): List<Pair<String, TransitionAggregate>> = transitionStats().entries
        .filter { it.value.attempts >= 2 }
        .sortedByDescending { it.value.errorRate * 14 + it.value.averageMs }
        .take(limit)
        .map { it.key to it.value }

    fun dailyWorkout(): List<DailyWorkoutItem> {
        val weakText = TrainingEngine.generateWeakDrill(keyStats(), transitionStats())
        return listOf(
            DailyWorkoutItem("Warm-up", "Easy alternating words", 2, Lesson(-20, 0, "Warm-up", "Find a smooth pace", "we type better with calm steady rhythm today", "Warm-up", 20, isPractice = true)),
            DailyWorkoutItem("Weakness trainer", "Built from your recorded weak keys", 3, Lesson(-21, 0, "Weakness Trainer", "Personalized from your data", weakText, "Weakness", 30, isPractice = true)),
            DailyWorkoutItem("Rhythm", "Make key timing more even", 2, Lesson(-22, 0, "Rhythm Trainer", "Smooth left-right transitions", "read your great new idea then bring the bright thing home", "Rhythm", 25, isPractice = true)),
            DailyWorkoutItem("Speed finish", "Controlled pace, not reckless speed", 3, Lesson(-23, 0, "Speed Finish", "Finish your daily workout", "mobile typing becomes fast when accuracy rhythm and reach work together", "Speed", 35, timeLimitSeconds = 60, isPractice = true))
        )
    }

    fun achievements(): List<Pair<String, Boolean>> {
        val h = history()
        val bestAccuracy = h.maxOfOrNull { it.accuracy } ?: 0
        val score = h.maxOfOrNull { it.thumbScore } ?: 0
        return listOf(
            "First session" to h.isNotEmpty(),
            "30 WPM" to (bestWpm() >= 30),
            "40 WPM" to (bestWpm() >= 40),
            "50 WPM Club" to (bestWpm() >= 50),
            "98% Accuracy" to (bestAccuracy >= 98),
            "Perfect Accuracy" to (bestAccuracy == 100),
            "7 Day Streak" to (streak() >= 7),
            "10K Characters" to (totalCharacters() >= 10_000),
            "ThumbScore 800" to (score >= 800),
            "Curriculum 50%" to (completionCount() >= LessonRepository.lessons.size / 2)
        )
    }

    fun exportJson(): String {
        return JSONObject()
            .put("format", "ThumbTypeBackup")
            .put("version", 4)
            .put("createdAt", System.currentTimeMillis())
            .put("profile", JSONObject(prefs.getString("profile", "{}") ?: "{}"))
            .put("settings", JSONObject(prefs.getString("settings", "{}") ?: "{}"))
            .put("xp", xp())
            .put("totalCharacters", totalCharacters())
            .put("totalSeconds", totalSeconds())
            .put("bestWpm", bestWpm())
            .put("streak", streak())
            .put("completedLessons", JSONArray(completedLessonIds().sorted()))
            .put("history", historyToJson(history()))
            .put("keyStats", keyStatsToJson(keyStats()))
            .put("transitionStats", transitionStatsToJson(transitionStats()))
            .toString(2)
    }

    fun importJson(raw: String): Result<Unit> = runCatching {
        require(raw.length <= 2_000_000) { "Backup is too large." }
        val root = JSONObject(raw)
        require(root.optString("format") == "ThumbTypeBackup") { "Not a ThumbType backup." }
        require(root.optInt("version", 0) in 1..4) { "Unsupported backup version." }

        val profileObj = root.optJSONObject("profile") ?: JSONObject()
        val settingsObj = root.optJSONObject("settings") ?: JSONObject()
        val historyArr = root.optJSONArray("history") ?: JSONArray()
        require(historyArr.length() <= 1000) { "Backup history is invalid." }

        val completed = root.optJSONArray("completedLessons") ?: JSONArray()
        val completedSet = buildSet {
            for (i in 0 until minOf(completed.length(), 200)) add(completed.optInt(i, -1).toString())
        }.filter { it != "-1" }.toSet()

        prefs.edit()
            .putBoolean("onboarded", true)
            .putString("profile", profileObj.toString())
            .putString("settings", settingsObj.toString())
            .putInt("xp", root.optInt("xp", 0).coerceIn(0, 10_000_000))
            .putInt("total_characters", root.optInt("totalCharacters", 0).coerceIn(0, Int.MAX_VALUE))
            .putLong("total_seconds", root.optLong("totalSeconds", 0L).coerceIn(0L, 100_000_000L))
            .putInt("best_wpm", root.optInt("bestWpm", 0).coerceIn(0, 300))
            .putInt("streak", root.optInt("streak", 0).coerceIn(0, 10_000))
            .putStringSet("completed_lessons", completedSet)
            .putString("history", historyArr.toString())
            .putString("key_stats", (root.optJSONObject("keyStats") ?: JSONObject()).toString())
            .putString("transition_stats", (root.optJSONObject("transitionStats") ?: JSONObject()).toString())
            .apply()
    }

    fun clearAll() = prefs.edit().clear().apply()

    private fun decodeKeyAggregate(source: JSONObject): KeyAggregate {
        if (source.has("attempts")) {
            val attempts = source.optInt("attempts", 0).coerceAtLeast(0)
            val correct = source.optInt("correct", 0).coerceIn(0, attempts)
            val errors = source.optInt("errors", attempts - correct).coerceIn(0, attempts)
            return KeyAggregate(
                attempts = attempts,
                correct = correct,
                errors = errors,
                totalCorrectReactionMs = source.optLong("totalCorrectReactionMs", 0L).coerceAtLeast(0L)
            )
        }

        // V1-V3 migration: legacy `presses` counted successful presses, not attempts.
        val legacyCorrect = source.optInt("presses", 0).coerceAtLeast(0)
        val legacyErrors = source.optInt("errors", 0).coerceAtLeast(0)
        return KeyAggregate(
            attempts = legacyCorrect + legacyErrors,
            correct = legacyCorrect,
            errors = legacyErrors,
            totalCorrectReactionMs = source.optLong("totalReactionMs", 0L).coerceAtLeast(0L)
        )
    }

    private fun decodeTransitionAggregate(source: JSONObject): TransitionAggregate {
        if (source.has("attempts")) {
            val attempts = source.optInt("attempts", 0).coerceAtLeast(0)
            val correct = source.optInt("correct", 0).coerceIn(0, attempts)
            val errors = source.optInt("errors", attempts - correct).coerceIn(0, attempts)
            return TransitionAggregate(
                attempts = attempts,
                correct = correct,
                errors = errors,
                totalSuccessfulMs = source.optLong("totalSuccessfulMs", 0L).coerceAtLeast(0L)
            )
        }

        // V1-V3 migration: legacy `count` represented successful transitions.
        val legacyCorrect = source.optInt("count", 0).coerceAtLeast(0)
        val legacyErrors = source.optInt("errors", 0).coerceAtLeast(0)
        return TransitionAggregate(
            attempts = legacyCorrect + legacyErrors,
            correct = legacyCorrect,
            errors = legacyErrors,
            totalSuccessfulMs = source.optLong("totalMs", 0L).coerceAtLeast(0L)
        )
    }

    private fun historyToJson(items: List<HistoryEntry>): JSONArray = JSONArray().apply {
        items.forEach { h ->
            put(JSONObject()
                .put("epochMs", h.epochMs)
                .put("wpm", h.wpm)
                .put("accuracy", h.accuracy)
                .put("thumbScore", h.thumbScore)
                .put("minutes", h.minutes)
                .put("title", h.title))
        }
    }

    private fun keyStatsToJson(items: Map<Char, KeyAggregate>): JSONObject = JSONObject().apply {
        items.forEach { (key, s) ->
            put(key.toString(), JSONObject()
                .put("attempts", s.attempts)
                .put("correct", s.correct)
                .put("errors", s.errors)
                .put("totalCorrectReactionMs", s.totalCorrectReactionMs))
        }
    }

    private fun transitionStatsToJson(items: Map<String, TransitionAggregate>): JSONObject = JSONObject().apply {
        items.forEach { (pair, s) ->
            put(pair, JSONObject()
                .put("attempts", s.attempts)
                .put("correct", s.correct)
                .put("errors", s.errors)
                .put("totalSuccessfulMs", s.totalSuccessfulMs))
        }
    }
}

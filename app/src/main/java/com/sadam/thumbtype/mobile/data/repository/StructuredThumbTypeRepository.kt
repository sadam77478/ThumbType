package com.sadam.thumbtype.mobile.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.sadam.thumbtype.mobile.AppRepository
import com.sadam.thumbtype.mobile.AppSettings
import com.sadam.thumbtype.mobile.CoachLevel
import com.sadam.thumbtype.mobile.DailyWorkoutItem
import com.sadam.thumbtype.mobile.HistoryEntry
import com.sadam.thumbtype.mobile.KeyAggregate
import com.sadam.thumbtype.mobile.Lesson
import com.sadam.thumbtype.mobile.LessonRepository
import com.sadam.thumbtype.mobile.SessionResult
import com.sadam.thumbtype.mobile.TrainingEngine
import com.sadam.thumbtype.mobile.TrainingFocus
import com.sadam.thumbtype.mobile.TransitionAggregate
import com.sadam.thumbtype.mobile.UserProfile
import com.sadam.thumbtype.mobile.data.local.DailyPracticeEntity
import com.sadam.thumbtype.mobile.data.local.KeyStatEntity
import com.sadam.thumbtype.mobile.data.local.LessonProgressEntity
import com.sadam.thumbtype.mobile.data.local.SessionEntity
import com.sadam.thumbtype.mobile.data.local.StoredPreferences
import com.sadam.thumbtype.mobile.data.local.ThumbTypeDatabase
import com.sadam.thumbtype.mobile.data.local.ThumbTypePreferencesStore
import com.sadam.thumbtype.mobile.data.local.TransitionStatEntity
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Production-oriented local repository.
 *
 * Structured/history data lives in Room while small preferences and counters live in
 * DataStore. The original SharedPreferences repository is read once for migration only and
 * retained untouched so an older installed build still has a rollback path.
 */
class StructuredThumbTypeRepository(context: Context) : ThumbTypeRepository {
    private val appContext = context.applicationContext
    private val database = ThumbTypeDatabase.get(appContext)
    private val dao = database.dao()
    private val preferences = ThumbTypePreferencesStore(appContext)
    private val migrationMutex = Mutex()
    @Volatile private var migrationReady = false

    override suspend fun isOnboarded(): Boolean {
        ensureMigrated()
        return preferences.snapshot().onboarded
    }

    override suspend fun setOnboarded(value: Boolean) {
        ensureMigrated()
        preferences.setOnboarded(value)
    }

    override suspend fun settings(): AppSettings {
        ensureMigrated()
        return preferences.snapshot().settings
    }

    override suspend fun saveSettings(value: AppSettings) {
        ensureMigrated()
        preferences.saveSettings(value)
    }

    override suspend fun profile(): UserProfile {
        ensureMigrated()
        return preferences.snapshot().profile
    }

    override suspend fun saveProfile(value: UserProfile) {
        ensureMigrated()
        preferences.saveProfile(value)
    }

    override suspend fun xp(): Int {
        ensureMigrated()
        return preferences.snapshot().xp
    }

    override suspend fun totalCharacters(): Int {
        ensureMigrated()
        return preferences.snapshot().totalCharacters
    }

    override suspend fun totalSeconds(): Long {
        ensureMigrated()
        return preferences.snapshot().totalSeconds
    }

    override suspend fun bestWpm(): Int {
        ensureMigrated()
        return preferences.snapshot().bestWpm
    }

    override suspend fun streak(): Int {
        ensureMigrated()
        return preferences.snapshot().streak
    }

    override suspend fun completedLessonIds(): Set<Int> {
        ensureMigrated()
        return dao.completedLessonIds().toSet()
    }

    override suspend fun completionCount(): Int = completedLessonIds().size

    override suspend fun history(): List<HistoryEntry> {
        ensureMigrated()
        return dao.recentSessions(MAX_HISTORY).asReversed().map { it.toHistoryEntry() }
    }

    override suspend fun keyStats(): Map<Char, KeyAggregate> {
        ensureMigrated()
        return dao.keyStats().mapNotNull { entity ->
            entity.key.firstOrNull()?.let { key -> key to entity.toAggregate() }
        }.toMap()
    }

    override suspend fun transitionStats(): Map<String, TransitionAggregate> {
        ensureMigrated()
        return dao.transitionStats().associate { it.pair to it.toAggregate() }
    }

    override suspend fun todaySeconds(): Long {
        ensureMigrated()
        return dao.practiceSeconds(dateString()) ?: 0L
    }

    override suspend fun currentThumbScore(): Int {
        ensureMigrated()
        return dao.latestSession()?.thumbScore ?: 0
    }

    override suspend fun lastWpm(): Int {
        ensureMigrated()
        return dao.latestSession()?.netWpm ?: preferences.snapshot().profile.baselineWpm
    }

    override suspend fun lastAccuracy(): Int {
        ensureMigrated()
        return dao.latestSession()?.accuracy ?: preferences.snapshot().profile.baselineAccuracy
    }

    override suspend fun saveSession(result: SessionResult, lesson: Lesson) {
        ensureMigrated()
        val now = uniqueSessionTimestamp()
        val today = dateString()

        database.withTransaction {
            dao.upsertSession(result.toEntity(now, lesson))

            result.keyUpdates.forEach { (key, update) ->
                val old = dao.keyStat(key.toString())?.toAggregate() ?: KeyAggregate()
                dao.upsertKeyStat(
                    KeyStatEntity(
                        key = key.toString(),
                        attempts = old.attempts + update.attempts,
                        correct = old.correct + update.correct,
                        errors = old.errors + update.errors,
                        totalCorrectReactionMs = old.totalCorrectReactionMs + update.totalCorrectReactionMs
                    )
                )
            }

            result.transitionUpdates.forEach { (pair, update) ->
                val old = dao.transitionStat(pair)?.toAggregate() ?: TransitionAggregate()
                dao.upsertTransitionStat(
                    TransitionStatEntity(
                        pair = pair.take(4),
                        attempts = old.attempts + update.attempts,
                        correct = old.correct + update.correct,
                        errors = old.errors + update.errors,
                        totalSuccessfulMs = old.totalSuccessfulMs + update.totalSuccessfulMs
                    )
                )
            }

            if (lesson.id > 0 && !lesson.isPractice) {
                dao.upsertLessonProgress(LessonProgressEntity(lesson.id, now))
            }

            val existingToday = dao.practiceSeconds(today) ?: 0L
            dao.upsertDailyPractice(
                DailyPracticeEntity(today, existingToday + result.durationMs.coerceAtLeast(0L) / 1000L)
            )
        }

        preferences.updateCounters { old ->
            val nextStreak = when (old.lastPracticeDate) {
                today -> old.streak
                dateString(-1) -> old.streak + 1
                else -> 1
            }
            val baselineProfile = if (lesson.id == 0) {
                old.profile.copy(
                    baselineWpm = result.netWpm,
                    baselineAccuracy = result.accuracy
                )
            } else old.profile

            old.copy(
                migrationVersion = ThumbTypePreferencesStore.CURRENT_MIGRATION_VERSION,
                onboarded = old.onboarded || lesson.id == 0,
                profile = baselineProfile,
                xp = (old.xp + lesson.xp).coerceIn(0, 10_000_000),
                totalCharacters = (old.totalCharacters.toLong() + result.chars)
                    .coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                totalSeconds = (old.totalSeconds + result.durationMs.coerceAtLeast(0L) / 1000L)
                    .coerceAtMost(100_000_000L),
                bestWpm = maxOf(old.bestWpm, result.netWpm).coerceIn(0, 300),
                streak = nextStreak.coerceIn(0, 10_000),
                lastPracticeDate = today
            )
        }
    }

    override suspend fun topWeakKeys(limit: Int): List<Pair<Char, KeyAggregate>> = keyStats().entries
        .filter { it.key.isLetterOrDigit() && it.value.attempts >= 2 }
        .sortedByDescending { it.value.errorRate * 12 + it.value.averageReactionMs }
        .take(limit.coerceIn(1, 50))
        .map { it.key to it.value }

    override suspend fun topWeakTransitions(limit: Int): List<Pair<String, TransitionAggregate>> = transitionStats().entries
        .filter { it.value.attempts >= 2 }
        .sortedByDescending { it.value.errorRate * 14 + it.value.averageMs }
        .take(limit.coerceIn(1, 50))
        .map { it.key to it.value }

    override suspend fun dailyWorkout(): List<DailyWorkoutItem> {
        val weakText = TrainingEngine.generateWeakDrill(keyStats(), transitionStats())
        return listOf(
            DailyWorkoutItem("Warm-up", "Easy alternating words", 2, Lesson(-20, 0, "Warm-up", "Find a smooth pace", "we type better with calm steady rhythm today", "Warm-up", 20, isPractice = true)),
            DailyWorkoutItem("Weakness trainer", "Built from your recorded weak keys", 3, Lesson(-21, 0, "Weakness Trainer", "Personalized from your data", weakText, "Weakness", 30, isPractice = true)),
            DailyWorkoutItem("Rhythm", "Make key timing more even", 2, Lesson(-22, 0, "Rhythm Trainer", "Smooth left-right transitions", "read your great new idea then bring the bright thing home", "Rhythm", 25, isPractice = true)),
            DailyWorkoutItem("Speed finish", "Controlled pace, not reckless speed", 3, Lesson(-23, 0, "Speed Finish", "Finish your daily workout", "mobile typing becomes fast when accuracy rhythm and reach work together", "Speed", 35, timeLimitSeconds = 60, isPractice = true))
        )
    }

    override suspend fun achievements(): List<Pair<String, Boolean>> {
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

    override suspend fun exportJson(): String {
        ensureMigrated()
        val stored = preferences.snapshot()
        val sessions = dao.recentSessions(MAX_BACKUP_SESSIONS).asReversed()
        val keys = keyStats()
        val transitions = transitionStats()

        return JSONObject()
            .put("format", "ThumbTypeBackup")
            .put("version", BACKUP_VERSION)
            .put("createdAt", System.currentTimeMillis())
            .put("onboarded", stored.onboarded)
            .put("profile", profileToJson(stored.profile))
            .put("settings", settingsToJson(stored.settings))
            .put("xp", stored.xp)
            .put("totalCharacters", stored.totalCharacters)
            .put("totalSeconds", stored.totalSeconds)
            .put("bestWpm", stored.bestWpm)
            .put("streak", stored.streak)
            .put("lastPracticeDate", stored.lastPracticeDate)
            .put("completedLessons", JSONArray(completedLessonIds().sorted()))
            .put("history", historyToJson(sessions.map { it.toHistoryEntry() }))
            .put("sessions", sessionsToJson(sessions))
            .put("keyStats", keyStatsToJson(keys))
            .put("transitionStats", transitionStatsToJson(transitions))
            .put("dailyPractice", dailyPracticeToJson(dao.allDailyPractice()))
            .toString(2)
    }

    override suspend fun importJson(raw: String): Result<Unit> {
        return try {
            require(raw.length <= MAX_BACKUP_BYTES) { "Backup is too large." }
            val root = JSONObject(raw)
            require(root.optString("format") == "ThumbTypeBackup") { "Not a ThumbType backup." }
            val version = root.optInt("version", 0)
            require(version in 1..BACKUP_VERSION) { "Unsupported backup version." }

            val profile = parseProfile(root.optJSONObject("profile") ?: JSONObject())
            val settings = parseSettings(root.optJSONObject("settings") ?: JSONObject())
            val completed = parseCompletedLessons(root.optJSONArray("completedLessons") ?: JSONArray())
            val keyStats = parseKeyStats(root.optJSONObject("keyStats") ?: JSONObject())
            val transitions = parseTransitionStats(root.optJSONObject("transitionStats") ?: JSONObject())
            val sessions = parseSessions(root)
            val daily = parseDailyPractice(root.optJSONArray("dailyPractice") ?: JSONArray())

            database.withTransaction {
                clearDatabaseTables()
                sessions.forEach { dao.upsertSession(it) }
                keyStats.forEach { (key, value) -> dao.upsertKeyStat(value.toEntity(key)) }
                transitions.forEach { (pair, value) -> dao.upsertTransitionStat(value.toEntity(pair)) }
                completed.forEach { dao.upsertLessonProgress(LessonProgressEntity(it, 0L)) }
                daily.forEach { dao.upsertDailyPractice(it) }
            }

            preferences.replace(
                StoredPreferences(
                    migrationVersion = ThumbTypePreferencesStore.CURRENT_MIGRATION_VERSION,
                    onboarded = root.optBoolean("onboarded", true),
                    settings = settings,
                    profile = profile,
                    xp = root.optInt("xp", 0).coerceIn(0, 10_000_000),
                    totalCharacters = root.optInt("totalCharacters", 0).coerceAtLeast(0),
                    totalSeconds = root.optLong("totalSeconds", 0L).coerceIn(0L, 100_000_000L),
                    bestWpm = root.optInt("bestWpm", 0).coerceIn(0, 300),
                    streak = root.optInt("streak", 0).coerceIn(0, 10_000),
                    lastPracticeDate = root.optString("lastPracticeDate", "").take(20)
                )
            )
            migrationReady = true
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun clearAll() {
        ensureMigrated()
        database.withTransaction { clearDatabaseTables() }
        preferences.clearKeepingMigrationMarker()
        // Prevent intentionally deleted data from being re-imported from the rollback store.
        appContext.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private suspend fun ensureMigrated() {
        if (migrationReady) return
        migrationMutex.withLock {
            if (migrationReady) return@withLock
            val existing = preferences.snapshot()
            if (existing.migrationVersion < ThumbTypePreferencesStore.CURRENT_MIGRATION_VERSION) {
                migrateLegacyV3()
            }
            migrationReady = true
        }
    }

    private suspend fun migrateLegacyV3() {
        val legacyPrefs = appContext.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
        if (legacyPrefs.all.isEmpty()) {
            preferences.replace(
                StoredPreferences(migrationVersion = ThumbTypePreferencesStore.CURRENT_MIGRATION_VERSION)
            )
            return
        }

        val legacy = AppRepository(appContext)
        val legacyHistory = legacy.history()
        val legacyKeys = legacy.keyStats()
        val legacyTransitions = legacy.transitionStats()
        val legacyCompleted = legacy.completedLessonIds()
        val today = dateString()
        val legacyTodaySeconds = legacy.todaySeconds()

        database.withTransaction {
            clearDatabaseTables()
            legacyHistory.forEachIndexed { index, item ->
                val epoch = if (item.epochMs > 0) item.epochMs else System.currentTimeMillis() + index
                dao.upsertSession(item.toMigratedEntity(epoch))
            }
            legacyKeys.forEach { (key, value) -> dao.upsertKeyStat(value.toEntity(key)) }
            legacyTransitions.forEach { (pair, value) -> dao.upsertTransitionStat(value.toEntity(pair)) }
            legacyCompleted.forEach { dao.upsertLessonProgress(LessonProgressEntity(it, 0L)) }
            if (legacyTodaySeconds > 0L) {
                dao.upsertDailyPractice(DailyPracticeEntity(today, legacyTodaySeconds))
            }
        }

        preferences.replace(
            StoredPreferences(
                migrationVersion = ThumbTypePreferencesStore.CURRENT_MIGRATION_VERSION,
                onboarded = legacy.isOnboarded(),
                settings = legacy.settings(),
                profile = legacy.profile(),
                xp = legacy.xp(),
                totalCharacters = legacy.totalCharacters(),
                totalSeconds = legacy.totalSeconds(),
                bestWpm = legacy.bestWpm(),
                streak = legacy.streak(),
                lastPracticeDate = legacyPrefs.getString("last_practice_date", "").orEmpty()
            )
        )
    }

    private suspend fun uniqueSessionTimestamp(): Long {
        val now = System.currentTimeMillis()
        val latest = dao.latestSession()?.epochMs ?: Long.MIN_VALUE
        return if (now <= latest) latest + 1L else now
    }

    private suspend fun clearDatabaseTables() {
        dao.clearSessions()
        dao.clearKeyStats()
        dao.clearTransitionStats()
        dao.clearLessonProgress()
        dao.clearDailyPractice()
    }

    private fun dateString(offsetDays: Int = 0): String {
        val calendar = Calendar.getInstance().apply {
            if (offsetDays != 0) add(Calendar.DAY_OF_YEAR, offsetDays)
        }
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
    }

    private fun SessionResult.toEntity(epochMs: Long, lesson: Lesson) = SessionEntity(
        epochMs = epochMs,
        lessonId = lesson.id,
        title = title.take(80),
        rawWpm = rawWpm.coerceIn(0, 300),
        netWpm = netWpm.coerceIn(0, 300),
        accuracy = accuracy.coerceIn(0, 100),
        rhythm = rhythm.coerceIn(0, 100),
        consistency = consistency.coerceIn(0, 100),
        thumbTechnique = thumbTechnique.coerceIn(0, 100),
        thumbBalance = thumbBalance.coerceIn(0, 100),
        thumbScore = thumbScore.coerceIn(0, 1000),
        mistakes = mistakes.coerceAtLeast(0),
        correctedErrors = correctedErrors.coerceAtLeast(0),
        uncorrectedErrors = uncorrectedErrors.coerceAtLeast(0),
        attempts = attempts.coerceAtLeast(0),
        correctChars = chars.coerceAtLeast(0),
        durationMs = durationMs.coerceIn(0L, 86_400_000L),
        leftTouches = leftTouches.coerceAtLeast(0),
        rightTouches = rightTouches.coerceAtLeast(0),
        isPractice = lesson.isPractice
    )

    private fun SessionEntity.toHistoryEntry() = HistoryEntry(
        epochMs = epochMs,
        wpm = netWpm,
        accuracy = accuracy,
        thumbScore = thumbScore,
        minutes = ((durationMs / 60_000.0).coerceAtLeast(0.1)).roundToInt(),
        title = title
    )

    private fun HistoryEntry.toMigratedEntity(epoch: Long) = SessionEntity(
        epochMs = epoch,
        lessonId = 0,
        title = title.take(80),
        rawWpm = wpm.coerceIn(0, 300),
        netWpm = wpm.coerceIn(0, 300),
        accuracy = accuracy.coerceIn(0, 100),
        rhythm = 0,
        consistency = 0,
        thumbTechnique = 0,
        thumbBalance = 0,
        thumbScore = thumbScore.coerceIn(0, 1000),
        mistakes = 0,
        correctedErrors = 0,
        uncorrectedErrors = 0,
        attempts = 0,
        correctChars = 0,
        durationMs = minutes.coerceAtLeast(0) * 60_000L,
        leftTouches = 0,
        rightTouches = 0,
        isPractice = true
    )

    private fun KeyStatEntity.toAggregate() = KeyAggregate(attempts, correct, errors, totalCorrectReactionMs)
    private fun TransitionStatEntity.toAggregate() = TransitionAggregate(attempts, correct, errors, totalSuccessfulMs)

    private fun KeyAggregate.toEntity(key: Char) = KeyStatEntity(
        key.toString(), attempts.coerceAtLeast(0), correct.coerceAtLeast(0), errors.coerceAtLeast(0), totalCorrectReactionMs.coerceAtLeast(0L)
    )

    private fun TransitionAggregate.toEntity(pair: String) = TransitionStatEntity(
        pair.take(4), attempts.coerceAtLeast(0), correct.coerceAtLeast(0), errors.coerceAtLeast(0), totalSuccessfulMs.coerceAtLeast(0L)
    )

    private fun profileToJson(value: UserProfile) = JSONObject()
        .put("targetWpm", value.targetWpm)
        .put("targetAccuracy", value.targetAccuracy)
        .put("dailyGoalMinutes", value.dailyGoalMinutes)
        .put("focus", value.focus.name)
        .put("baselineWpm", value.baselineWpm)
        .put("baselineAccuracy", value.baselineAccuracy)

    private fun settingsToJson(value: AppSettings) = JSONObject()
        .put("darkMode", value.darkMode)
        .put("haptics", value.haptics)
        .put("sounds", value.sounds)
        .put("reducedMotion", value.reducedMotion)
        .put("largeText", value.largeText)
        .put("privacyScreenProtection", value.privacyScreenProtection)
        .put("coachLevel", value.coachLevel.name)

    private fun historyToJson(items: List<HistoryEntry>) = JSONArray().apply {
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

    private fun sessionsToJson(items: List<SessionEntity>) = JSONArray().apply {
        items.forEach { s ->
            put(JSONObject()
                .put("epochMs", s.epochMs)
                .put("lessonId", s.lessonId)
                .put("title", s.title)
                .put("rawWpm", s.rawWpm)
                .put("netWpm", s.netWpm)
                .put("accuracy", s.accuracy)
                .put("rhythm", s.rhythm)
                .put("consistency", s.consistency)
                .put("thumbTechnique", s.thumbTechnique)
                .put("thumbBalance", s.thumbBalance)
                .put("thumbScore", s.thumbScore)
                .put("mistakes", s.mistakes)
                .put("correctedErrors", s.correctedErrors)
                .put("uncorrectedErrors", s.uncorrectedErrors)
                .put("attempts", s.attempts)
                .put("correctChars", s.correctChars)
                .put("durationMs", s.durationMs)
                .put("leftTouches", s.leftTouches)
                .put("rightTouches", s.rightTouches)
                .put("isPractice", s.isPractice))
        }
    }

    private fun keyStatsToJson(items: Map<Char, KeyAggregate>) = JSONObject().apply {
        items.forEach { (key, s) ->
            put(key.toString(), JSONObject()
                .put("attempts", s.attempts)
                .put("correct", s.correct)
                .put("errors", s.errors)
                .put("totalCorrectReactionMs", s.totalCorrectReactionMs))
        }
    }

    private fun transitionStatsToJson(items: Map<String, TransitionAggregate>) = JSONObject().apply {
        items.forEach { (pair, s) ->
            put(pair, JSONObject()
                .put("attempts", s.attempts)
                .put("correct", s.correct)
                .put("errors", s.errors)
                .put("totalSuccessfulMs", s.totalSuccessfulMs))
        }
    }

    private fun dailyPracticeToJson(items: List<DailyPracticeEntity>) = JSONArray().apply {
        items.takeLast(400).forEach { value ->
            put(JSONObject().put("date", value.date).put("seconds", value.seconds))
        }
    }

    private fun parseProfile(o: JSONObject) = UserProfile(
        targetWpm = o.optInt("targetWpm", 50).coerceIn(20, 120),
        targetAccuracy = o.optInt("targetAccuracy", 97).coerceIn(80, 100),
        dailyGoalMinutes = o.optInt("dailyGoalMinutes", 10).coerceIn(5, 60),
        focus = runCatching { TrainingFocus.valueOf(o.optString("focus", TrainingFocus.BALANCED.name)) }
            .getOrDefault(TrainingFocus.BALANCED),
        baselineWpm = o.optInt("baselineWpm", 0).coerceAtLeast(0),
        baselineAccuracy = o.optInt("baselineAccuracy", 0).coerceIn(0, 100)
    )

    private fun parseSettings(o: JSONObject) = AppSettings(
        darkMode = o.optBoolean("darkMode", false),
        haptics = o.optBoolean("haptics", true),
        sounds = o.optBoolean("sounds", false),
        reducedMotion = o.optBoolean("reducedMotion", false),
        largeText = o.optBoolean("largeText", false),
        privacyScreenProtection = o.optBoolean("privacyScreenProtection", false),
        coachLevel = runCatching { CoachLevel.valueOf(o.optString("coachLevel", CoachLevel.FULL.name)) }
            .getOrDefault(CoachLevel.FULL)
    )

    private fun parseCompletedLessons(array: JSONArray): Set<Int> = buildSet {
        for (i in 0 until minOf(array.length(), 500)) {
            array.optInt(i, -1).takeIf { it > 0 }?.let(::add)
        }
    }

    private fun parseKeyStats(o: JSONObject): Map<Char, KeyAggregate> = buildMap {
        o.keys().forEach { rawKey ->
            val key = rawKey.firstOrNull() ?: return@forEach
            val s = o.optJSONObject(rawKey) ?: return@forEach
            val aggregate = if (s.has("attempts")) {
                val attempts = s.optInt("attempts", 0).coerceAtLeast(0)
                val correct = s.optInt("correct", 0).coerceIn(0, attempts)
                KeyAggregate(
                    attempts = attempts,
                    correct = correct,
                    errors = s.optInt("errors", attempts - correct).coerceIn(0, attempts),
                    totalCorrectReactionMs = s.optLong("totalCorrectReactionMs", 0L).coerceAtLeast(0L)
                )
            } else {
                val correct = s.optInt("presses", 0).coerceAtLeast(0)
                val errors = s.optInt("errors", 0).coerceAtLeast(0)
                KeyAggregate(correct + errors, correct, errors, s.optLong("totalReactionMs", 0L).coerceAtLeast(0L))
            }
            put(key, aggregate)
        }
    }

    private fun parseTransitionStats(o: JSONObject): Map<String, TransitionAggregate> = buildMap {
        o.keys().forEach { rawPair ->
            val pair = rawPair.take(4)
            if (pair.isBlank()) return@forEach
            val s = o.optJSONObject(rawPair) ?: return@forEach
            val aggregate = if (s.has("attempts")) {
                val attempts = s.optInt("attempts", 0).coerceAtLeast(0)
                val correct = s.optInt("correct", 0).coerceIn(0, attempts)
                TransitionAggregate(
                    attempts = attempts,
                    correct = correct,
                    errors = s.optInt("errors", attempts - correct).coerceIn(0, attempts),
                    totalSuccessfulMs = s.optLong("totalSuccessfulMs", 0L).coerceAtLeast(0L)
                )
            } else {
                val correct = s.optInt("count", 0).coerceAtLeast(0)
                val errors = s.optInt("errors", 0).coerceAtLeast(0)
                TransitionAggregate(correct + errors, correct, errors, s.optLong("totalMs", 0L).coerceAtLeast(0L))
            }
            put(pair, aggregate)
        }
    }

    private fun parseSessions(root: JSONObject): List<SessionEntity> {
        val sessions = root.optJSONArray("sessions")
        if (sessions != null && sessions.length() > 0) {
            require(sessions.length() <= MAX_BACKUP_SESSIONS) { "Backup session history is invalid." }
            return buildList {
                for (i in 0 until sessions.length()) {
                    val s = sessions.optJSONObject(i) ?: continue
                    val epoch = s.optLong("epochMs", 0L).takeIf { it > 0L } ?: System.currentTimeMillis() + i
                    add(SessionEntity(
                        epochMs = epoch,
                        lessonId = s.optInt("lessonId", 0),
                        title = s.optString("title", "Session").take(80),
                        rawWpm = s.optInt("rawWpm", 0).coerceIn(0, 300),
                        netWpm = s.optInt("netWpm", 0).coerceIn(0, 300),
                        accuracy = s.optInt("accuracy", 0).coerceIn(0, 100),
                        rhythm = s.optInt("rhythm", 0).coerceIn(0, 100),
                        consistency = s.optInt("consistency", 0).coerceIn(0, 100),
                        thumbTechnique = s.optInt("thumbTechnique", 0).coerceIn(0, 100),
                        thumbBalance = s.optInt("thumbBalance", 0).coerceIn(0, 100),
                        thumbScore = s.optInt("thumbScore", 0).coerceIn(0, 1000),
                        mistakes = s.optInt("mistakes", 0).coerceAtLeast(0),
                        correctedErrors = s.optInt("correctedErrors", 0).coerceAtLeast(0),
                        uncorrectedErrors = s.optInt("uncorrectedErrors", 0).coerceAtLeast(0),
                        attempts = s.optInt("attempts", 0).coerceAtLeast(0),
                        correctChars = s.optInt("correctChars", 0).coerceAtLeast(0),
                        durationMs = s.optLong("durationMs", 0L).coerceIn(0L, 86_400_000L),
                        leftTouches = s.optInt("leftTouches", 0).coerceAtLeast(0),
                        rightTouches = s.optInt("rightTouches", 0).coerceAtLeast(0),
                        isPractice = s.optBoolean("isPractice", true)
                    ))
                }
            }.distinctBy { it.epochMs }
        }

        val history = root.optJSONArray("history") ?: JSONArray()
        require(history.length() <= MAX_HISTORY_IMPORT) { "Backup history is invalid." }
        return buildList {
            for (i in 0 until history.length()) {
                val h = history.optJSONObject(i) ?: continue
                val entry = HistoryEntry(
                    epochMs = h.optLong("epochMs", 0L),
                    wpm = h.optInt("wpm", 0).coerceIn(0, 300),
                    accuracy = h.optInt("accuracy", 0).coerceIn(0, 100),
                    thumbScore = h.optInt("thumbScore", 0).coerceIn(0, 1000),
                    minutes = h.optInt("minutes", 0).coerceIn(0, 1440),
                    title = h.optString("title", "Session").take(80)
                )
                val epoch = entry.epochMs.takeIf { it > 0L } ?: System.currentTimeMillis() + i
                add(entry.toMigratedEntity(epoch))
            }
        }.distinctBy { it.epochMs }
    }

    private fun parseDailyPractice(array: JSONArray): List<DailyPracticeEntity> = buildList {
        for (i in 0 until minOf(array.length(), 400)) {
            val o = array.optJSONObject(i) ?: continue
            val date = o.optString("date", "").take(20)
            if (date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                add(DailyPracticeEntity(date, o.optLong("seconds", 0L).coerceIn(0L, 86_400L)))
            }
        }
    }.distinctBy { it.date }

    companion object {
        private const val LEGACY_PREFS = "thumbtype_elite_v3"
        private const val BACKUP_VERSION = 5
        private const val MAX_HISTORY = 1000
        private const val MAX_HISTORY_IMPORT = 1000
        private const val MAX_BACKUP_SESSIONS = 5000
        private const val MAX_BACKUP_BYTES = 4_000_000
    }
}

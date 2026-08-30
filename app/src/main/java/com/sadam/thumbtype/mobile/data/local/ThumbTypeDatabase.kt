package com.sadam.thumbtype.mobile.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val epochMs: Long,
    val lessonId: Int,
    val title: String,
    val rawWpm: Int,
    val netWpm: Int,
    val accuracy: Int,
    val rhythm: Int,
    val consistency: Int,
    val thumbTechnique: Int,
    val thumbBalance: Int,
    val thumbScore: Int,
    val mistakes: Int,
    val correctedErrors: Int,
    val uncorrectedErrors: Int,
    val attempts: Int,
    val correctChars: Int,
    val durationMs: Long,
    val leftTouches: Int,
    val rightTouches: Int,
    val isPractice: Boolean
)

@Entity(tableName = "key_stats")
data class KeyStatEntity(
    @PrimaryKey val key: String,
    val attempts: Int,
    val correct: Int,
    val errors: Int,
    val totalCorrectReactionMs: Long
)

@Entity(tableName = "transition_stats")
data class TransitionStatEntity(
    @PrimaryKey val pair: String,
    val attempts: Int,
    val correct: Int,
    val errors: Int,
    val totalSuccessfulMs: Long
)

@Entity(tableName = "lesson_progress")
data class LessonProgressEntity(
    @PrimaryKey val lessonId: Int,
    val completedAtMs: Long
)

@Entity(tableName = "daily_practice")
data class DailyPracticeEntity(
    @PrimaryKey val date: String,
    val seconds: Long
)

@Dao
interface ThumbTypeDao {
    @Query("SELECT * FROM sessions ORDER BY epochMs DESC LIMIT :limit")
    suspend fun recentSessions(limit: Int): List<SessionEntity>

    @Query("SELECT * FROM sessions ORDER BY epochMs DESC LIMIT 1")
    suspend fun latestSession(): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(value: SessionEntity)

    @Query("SELECT * FROM key_stats")
    suspend fun keyStats(): List<KeyStatEntity>

    @Query("SELECT * FROM key_stats WHERE `key` = :key LIMIT 1")
    suspend fun keyStat(key: String): KeyStatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertKeyStat(value: KeyStatEntity)

    @Query("SELECT * FROM transition_stats")
    suspend fun transitionStats(): List<TransitionStatEntity>

    @Query("SELECT * FROM transition_stats WHERE pair = :pair LIMIT 1")
    suspend fun transitionStat(pair: String): TransitionStatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransitionStat(value: TransitionStatEntity)

    @Query("SELECT lessonId FROM lesson_progress")
    suspend fun completedLessonIds(): List<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLessonProgress(value: LessonProgressEntity)

    @Query("SELECT seconds FROM daily_practice WHERE date = :date LIMIT 1")
    suspend fun practiceSeconds(date: String): Long?

    @Query("SELECT * FROM daily_practice ORDER BY date ASC")
    suspend fun allDailyPractice(): List<DailyPracticeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyPractice(value: DailyPracticeEntity)

    @Query("DELETE FROM sessions")
    suspend fun clearSessions()

    @Query("DELETE FROM key_stats")
    suspend fun clearKeyStats()

    @Query("DELETE FROM transition_stats")
    suspend fun clearTransitionStats()

    @Query("DELETE FROM lesson_progress")
    suspend fun clearLessonProgress()

    @Query("DELETE FROM daily_practice")
    suspend fun clearDailyPractice()
}

@Database(
    entities = [
        SessionEntity::class,
        KeyStatEntity::class,
        TransitionStatEntity::class,
        LessonProgressEntity::class,
        DailyPracticeEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ThumbTypeDatabase : RoomDatabase() {
    abstract fun dao(): ThumbTypeDao

    companion object {
        @Volatile
        private var INSTANCE: ThumbTypeDatabase? = null

        fun get(context: Context): ThumbTypeDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                ThumbTypeDatabase::class.java,
                "thumbtype.db"
            ).build().also { INSTANCE = it }
        }
    }
}

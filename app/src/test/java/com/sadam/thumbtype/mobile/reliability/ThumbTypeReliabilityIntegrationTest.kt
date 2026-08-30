package com.sadam.thumbtype.mobile.reliability

import android.app.Application
import com.sadam.thumbtype.mobile.Lesson
import com.sadam.thumbtype.mobile.PressEvent
import com.sadam.thumbtype.mobile.ThumbSide
import com.sadam.thumbtype.mobile.TrainingEngine
import com.sadam.thumbtype.mobile.UserProfile
import com.sadam.thumbtype.mobile.app.readModels
import com.sadam.thumbtype.mobile.data.repository.StructuredThumbTypeRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ThumbTypeReliabilityIntegrationTest {
    private lateinit var context: Application

    @Before
    fun cleanStorage() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("thumbtype_elite_v3", Application.MODE_PRIVATE).edit().clear().commit()
        runBlocking { StructuredThumbTypeRepository(context).clearAll() }
    }

    @Test
    fun completedSessionsRemainDistinct_andDriveOneConsistentReadModel() = runBlocking {
        val repository = StructuredThumbTypeRepository(context)
        repository.setOnboarded(true)
        repository.saveProfile(UserProfile(targetWpm = 55, targetAccuracy = 98, dailyGoalMinutes = 10))
        val lesson = Lesson(1, 1, "Reliability", "Two saves", "abc", "Foundation", 40, isPractice = false)
        val result = TrainingEngine.calculateResult(
            lesson.title,
            listOf(
                PressEvent('a', 'a', true, 120L, ThumbSide.LEFT, ThumbSide.LEFT, 0),
                PressEvent('b', 'b', true, 140L, ThumbSide.LEFT, ThumbSide.LEFT, 1),
                PressEvent('c', 'c', true, 130L, ThumbSide.LEFT, ThumbSide.LEFT, 2)
            ),
            durationMs = 5_000L,
            targetWpm = 55
        )

        repository.saveSession(result, lesson)
        repository.saveSession(result, lesson)

        assertEquals(2, repository.history().size)
        val models = repository.readModels()
        assertEquals(2, models.progress.history.size)
        assertEquals(100, models.home.lastAccuracy)
        assertTrue(models.learn.completedLessonIds.contains(1))
        assertTrue(models.home.xp >= 80)
    }

    @Test
    fun rejectedBackupNeverDestroysExistingProgress() = runBlocking {
        val repository = StructuredThumbTypeRepository(context)
        repository.setOnboarded(true)
        repository.saveProfile(UserProfile(targetWpm = 60, targetAccuracy = 99, dailyGoalMinutes = 15))
        val before = repository.profile()

        val malformed = repository.importJson("{ definitely-not-json")
        assertTrue(malformed.isFailure)
        assertEquals(before, repository.profile())
        assertTrue(repository.isOnboarded())

        val unsupported = repository.importJson("""{"format":"ThumbTypeBackup","version":999}""")
        assertTrue(unsupported.isFailure)
        assertEquals(before, repository.profile())
        assertTrue(repository.isOnboarded())
    }

    @Test
    fun deleteAllReturnsRepositoryToFreshLocalState() = runBlocking {
        val repository = StructuredThumbTypeRepository(context)
        repository.setOnboarded(true)
        repository.saveProfile(UserProfile(targetWpm = 70, targetAccuracy = 99, dailyGoalMinutes = 20))

        repository.clearAll()

        assertFalse(repository.isOnboarded())
        assertTrue(repository.history().isEmpty())
        assertTrue(repository.completedLessonIds().isEmpty())
        assertEquals(0, repository.xp())
    }
}

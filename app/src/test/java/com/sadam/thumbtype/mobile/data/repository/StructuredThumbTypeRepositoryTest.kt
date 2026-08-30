package com.sadam.thumbtype.mobile.data.repository

import android.app.Application
import com.sadam.thumbtype.mobile.AppRepository
import com.sadam.thumbtype.mobile.AppSettings
import com.sadam.thumbtype.mobile.Lesson
import com.sadam.thumbtype.mobile.PressEvent
import com.sadam.thumbtype.mobile.ThumbSide
import com.sadam.thumbtype.mobile.TrainingEngine
import com.sadam.thumbtype.mobile.TrainingFocus
import com.sadam.thumbtype.mobile.UserProfile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class StructuredThumbTypeRepositoryTest {

    @Test
    fun legacyV3DataMigratesIntoRoomAndDataStoreWithoutLosingProgress() = runBlocking {
        val context: Application = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("thumbtype_elite_v3", Application.MODE_PRIVATE)
            .edit().clear().commit()
        context.deleteDatabase("thumbtype.db")
        File(context.filesDir, "datastore/thumbtype_preferences_v1.preferences_pb").delete()

        val legacy = AppRepository(context)
        legacy.setOnboarded(true)
        legacy.saveSettings(AppSettings(darkMode = true, haptics = false))
        legacy.saveProfile(
            UserProfile(
                targetWpm = 60,
                targetAccuracy = 98,
                dailyGoalMinutes = 15,
                focus = TrainingFocus.ACCURACY,
                baselineWpm = 31,
                baselineAccuracy = 94
            )
        )

        val lesson = Lesson(
            id = 1,
            stage = 1,
            title = "Migration lesson",
            subtitle = "Persistence test",
            text = "abc",
            skill = "Foundation",
            xp = 40,
            isPractice = false
        )
        val events = listOf(
            PressEvent('a', 'a', true, 200L, ThumbSide.LEFT, ThumbSide.LEFT, 0),
            PressEvent('b', 'x', false, 220L, ThumbSide.LEFT, ThumbSide.RIGHT, 1),
            PressEvent('b', 'b', true, 180L, ThumbSide.LEFT, ThumbSide.LEFT, 1),
            PressEvent('c', 'c', true, 190L, ThumbSide.LEFT, ThumbSide.LEFT, 2)
        )
        val result = TrainingEngine.calculateResult("Migration lesson", events, 10_000L, 60)
        legacy.saveSession(result, lesson)

        val repository = StructuredThumbTypeRepository(context)

        assertTrue(repository.isOnboarded())
        assertEquals(60, repository.profile().targetWpm)
        assertEquals(98, repository.profile().targetAccuracy)
        assertTrue(repository.settings().darkMode)
        assertEquals(false, repository.settings().haptics)
        assertTrue(repository.completedLessonIds().contains(1))
        assertEquals(1, repository.history().size)
        assertTrue(repository.keyStats().isNotEmpty())
        assertTrue(repository.transitionStats().isNotEmpty())
        assertEquals(40, repository.xp())

        val backup = repository.exportJson()
        assertTrue(backup.contains("\"version\": 5"))
        assertTrue(backup.contains("\"sessions\""))
        assertTrue(backup.contains("\"dailyPractice\""))
    }
}

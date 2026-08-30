package com.sadam.thumbtype.mobile.app.state

import androidx.lifecycle.SavedStateHandle
import com.sadam.thumbtype.mobile.AppScreen
import com.sadam.thumbtype.mobile.Lesson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ThumbTypeSavedStateTest {
    @Test
    fun screenRoundTrip_respectsOnboardingAndResultsFallback() {
        val handle = SavedStateHandle()
        ThumbTypeSavedState.saveScreen(handle, AppScreen.Progress)
        assertEquals(AppScreen.Onboarding, ThumbTypeSavedState.restoreScreen(handle, onboarded = false))
        assertEquals(AppScreen.Progress, ThumbTypeSavedState.restoreScreen(handle, onboarded = true))

        ThumbTypeSavedState.saveScreen(handle, AppScreen.Results)
        assertEquals(AppScreen.Home, ThumbTypeSavedState.restoreScreen(handle, onboarded = true))
    }

    @Test
    fun malformedScreenFallsBackHome() {
        val handle = SavedStateHandle(mapOf("thumbtype.screen" to "NOT_A_SCREEN"))
        assertEquals(AppScreen.Home, ThumbTypeSavedState.restoreScreen(handle, onboarded = true))
    }

    @Test
    fun lessonRoundTrip_preservesAllBundleSafeFields() {
        val handle = SavedStateHandle()
        val lesson = Lesson(
            id = -44,
            stage = 3,
            title = "Reliability drill",
            subtitle = "Process recreation",
            text = "two thumb typing survives recreation",
            skill = "Reliability",
            xp = 77,
            timeLimitSeconds = 45,
            isPractice = true
        )

        ThumbTypeSavedState.saveLesson(handle, lesson)
        assertEquals(lesson, ThumbTypeSavedState.restoreLesson(handle))

        ThumbTypeSavedState.clear(handle)
        val restoredAfterClear = ThumbTypeSavedState.restoreLesson(handle)
        assertFalse(restoredAfterClear.title == lesson.title)
    }
}

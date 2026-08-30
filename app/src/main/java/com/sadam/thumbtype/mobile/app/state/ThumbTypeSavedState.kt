package com.sadam.thumbtype.mobile.app.state

import androidx.lifecycle.SavedStateHandle
import com.sadam.thumbtype.mobile.AppScreen
import com.sadam.thumbtype.mobile.Lesson
import com.sadam.thumbtype.mobile.LessonRepository

/** Small, Bundle-safe saved-state codec for app navigation and the active lesson. */
object ThumbTypeSavedState {
    private const val KEY_SCREEN = "thumbtype.screen"
    private const val KEY_LESSON_ID = "thumbtype.lesson.id"
    private const val KEY_LESSON_STAGE = "thumbtype.lesson.stage"
    private const val KEY_LESSON_TITLE = "thumbtype.lesson.title"
    private const val KEY_LESSON_SUBTITLE = "thumbtype.lesson.subtitle"
    private const val KEY_LESSON_TEXT = "thumbtype.lesson.text"
    private const val KEY_LESSON_SKILL = "thumbtype.lesson.skill"
    private const val KEY_LESSON_XP = "thumbtype.lesson.xp"
    private const val KEY_LESSON_LIMIT = "thumbtype.lesson.limit"
    private const val KEY_LESSON_PRACTICE = "thumbtype.lesson.practice"

    fun restoreScreen(handle: SavedStateHandle, onboarded: Boolean): AppScreen {
        if (!onboarded) return AppScreen.Onboarding
        val restored = handle.get<String>(KEY_SCREEN)
            ?.let { name -> runCatching { AppScreen.valueOf(name) }.getOrNull() }
            ?: AppScreen.Home

        // Result details are intentionally not placed in SavedStateHandle. The completed
        // session has already been persisted, so a recreated Results destination safely
        // returns to Home instead of showing stale or incomplete data.
        return if (restored == AppScreen.Results) AppScreen.Home else restored
    }

    fun saveScreen(handle: SavedStateHandle, screen: AppScreen) {
        handle[KEY_SCREEN] = screen.name
    }

    fun restoreLesson(handle: SavedStateHandle): Lesson {
        val title = handle.get<String>(KEY_LESSON_TITLE) ?: return LessonRepository.lessons.first()
        return Lesson(
            id = handle.get<Int>(KEY_LESSON_ID) ?: LessonRepository.lessons.first().id,
            stage = handle.get<Int>(KEY_LESSON_STAGE) ?: 0,
            title = title,
            subtitle = handle.get<String>(KEY_LESSON_SUBTITLE).orEmpty(),
            text = handle.get<String>(KEY_LESSON_TEXT).orEmpty(),
            skill = handle.get<String>(KEY_LESSON_SKILL).orEmpty(),
            xp = handle.get<Int>(KEY_LESSON_XP) ?: 40,
            timeLimitSeconds = handle.get<Int>(KEY_LESSON_LIMIT)?.takeIf { it >= 0 },
            isPractice = handle.get<Boolean>(KEY_LESSON_PRACTICE) ?: false
        )
    }

    fun saveLesson(handle: SavedStateHandle, lesson: Lesson) {
        handle[KEY_LESSON_ID] = lesson.id
        handle[KEY_LESSON_STAGE] = lesson.stage
        handle[KEY_LESSON_TITLE] = lesson.title
        handle[KEY_LESSON_SUBTITLE] = lesson.subtitle
        handle[KEY_LESSON_TEXT] = lesson.text
        handle[KEY_LESSON_SKILL] = lesson.skill
        handle[KEY_LESSON_XP] = lesson.xp
        handle[KEY_LESSON_LIMIT] = lesson.timeLimitSeconds ?: -1
        handle[KEY_LESSON_PRACTICE] = lesson.isPractice
    }

    fun clear(handle: SavedStateHandle) {
        listOf(
            KEY_SCREEN,
            KEY_LESSON_ID,
            KEY_LESSON_STAGE,
            KEY_LESSON_TITLE,
            KEY_LESSON_SUBTITLE,
            KEY_LESSON_TEXT,
            KEY_LESSON_SKILL,
            KEY_LESSON_XP,
            KEY_LESSON_LIMIT,
            KEY_LESSON_PRACTICE
        ).forEach { key -> handle.remove<Any?>(key) }
    }
}

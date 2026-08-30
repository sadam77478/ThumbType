package com.sadam.thumbtype.mobile.app

import com.sadam.thumbtype.mobile.AppScreen
import com.sadam.thumbtype.mobile.AppSettings
import com.sadam.thumbtype.mobile.Lesson
import com.sadam.thumbtype.mobile.LessonRepository
import com.sadam.thumbtype.mobile.SessionResult
import com.sadam.thumbtype.mobile.UserProfile

data class ThumbTypeUiState(
    val isLoading: Boolean = true,
    val settings: AppSettings = AppSettings(),
    val profile: UserProfile = UserProfile(),
    val screen: AppScreen = AppScreen.Home,
    val selectedLesson: Lesson = LessonRepository.lessons.first(),
    val lastResult: SessionResult? = null,
    val readModels: ThumbTypeReadModels = ThumbTypeReadModels(),
    val refreshToken: Int = 0,
    val sessionNonce: Int = 0
)

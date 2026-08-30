package com.sadam.thumbtype.mobile.app

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import com.sadam.thumbtype.mobile.AppScreen
import com.sadam.thumbtype.mobile.AppSettings
import com.sadam.thumbtype.mobile.Lesson
import com.sadam.thumbtype.mobile.SessionResult
import com.sadam.thumbtype.mobile.ThumbTypeApplication
import com.sadam.thumbtype.mobile.UserProfile
import com.sadam.thumbtype.mobile.app.navigation.ThumbTypeNavigation
import com.sadam.thumbtype.mobile.app.state.ThumbTypeSavedState
import com.sadam.thumbtype.mobile.data.repository.ThumbTypeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThumbTypeViewModel(
    application: ThumbTypeApplication,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    private val repository: ThumbTypeRepository = application.container.repository

    private val restoredLesson = ThumbTypeSavedState.restoreLesson(savedStateHandle)
    private val restoredScreen = ThumbTypeSavedState.restoreScreen(
        handle = savedStateHandle,
        onboarded = repository.isOnboarded()
    )

    private val _uiState = MutableStateFlow(
        ThumbTypeUiState(
            settings = repository.settings(),
            profile = repository.profile(),
            screen = restoredScreen,
            selectedLesson = restoredLesson,
            readModels = repository.readModels()
        )
    )
    val uiState: StateFlow<ThumbTypeUiState> = _uiState.asStateFlow()

    init {
        ThumbTypeSavedState.saveScreen(savedStateHandle, restoredScreen)
        if (restoredScreen == AppScreen.Trainer) {
            ThumbTypeSavedState.saveLesson(savedStateHandle, restoredLesson)
        }
    }

    fun navigate(screen: AppScreen) {
        ThumbTypeSavedState.saveScreen(savedStateHandle, screen)
        _uiState.value = _uiState.value.copy(screen = screen)
    }

    fun goHome() = navigate(AppScreen.Home)

    fun onBack() {
        navigate(ThumbTypeNavigation.backDestination(_uiState.value.screen))
    }

    fun saveProfile(profile: UserProfile) {
        repository.saveProfile(profile)
        val current = _uiState.value
        _uiState.value = current.copy(
            profile = profile,
            readModels = repository.readModels(),
            refreshToken = current.refreshToken + 1
        )
    }

    fun saveSettings(settings: AppSettings) {
        repository.saveSettings(settings)
        _uiState.value = _uiState.value.copy(settings = settings)
    }

    fun skipOnboarding() {
        repository.setOnboarded(true)
        navigate(AppScreen.Home)
    }

    fun startBaseline() {
        startLesson(
            Lesson(
                id = 0,
                stage = 0,
                title = "Baseline Test",
                subtitle = "Measure your starting speed and accuracy",
                text = "the quick brown fox jumps over the lazy dog then sends a short mobile message with calm accurate rhythm",
                skill = "Baseline",
                xp = 50,
                timeLimitSeconds = 60,
                isPractice = true
            )
        )
    }

    fun startLesson(lesson: Lesson) {
        ThumbTypeSavedState.saveLesson(savedStateHandle, lesson)
        ThumbTypeSavedState.saveScreen(savedStateHandle, AppScreen.Trainer)
        val current = _uiState.value
        _uiState.value = current.copy(
            selectedLesson = lesson,
            lastResult = null,
            sessionNonce = current.sessionNonce + 1,
            screen = AppScreen.Trainer
        )
    }

    fun retryCurrentLesson() {
        startLesson(_uiState.value.selectedLesson)
    }

    fun completeSession(result: SessionResult) {
        val current = _uiState.value
        repository.saveSession(result, current.selectedLesson)
        ThumbTypeSavedState.saveScreen(savedStateHandle, AppScreen.Results)
        _uiState.value = current.copy(
            profile = repository.profile(),
            lastResult = result,
            readModels = repository.readModels(),
            refreshToken = current.refreshToken + 1,
            screen = AppScreen.Results
        )
    }

    fun startWeaknessLesson() {
        val weakText = _uiState.value.readModels.practice.weakDrill
        startLesson(
            Lesson(
                id = -777,
                stage = 0,
                title = "Weakness Trainer",
                subtitle = "Built from your latest performance",
                text = weakText,
                skill = "Weakness",
                xp = 35,
                isPractice = true
            )
        )
    }

    fun restoreBackup(raw: String): Result<Unit> {
        val result = repository.importJson(raw)
        if (result.isSuccess) {
            val current = _uiState.value
            _uiState.value = current.copy(
                settings = repository.settings(),
                profile = repository.profile(),
                readModels = repository.readModels(),
                refreshToken = current.refreshToken + 1
            )
        }
        return result
    }

    fun exportBackupJson(): String = repository.exportJson()

    fun deleteAllLocalData() {
        repository.clearAll()
        ThumbTypeSavedState.clear(savedStateHandle)
        ThumbTypeSavedState.saveScreen(savedStateHandle, AppScreen.Onboarding)
        val current = _uiState.value
        _uiState.value = ThumbTypeUiState(
            screen = AppScreen.Onboarding,
            readModels = repository.readModels(),
            refreshToken = current.refreshToken + 1,
            sessionNonce = current.sessionNonce + 1
        )
    }
}

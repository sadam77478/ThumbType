package com.sadam.thumbtype.mobile.app

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.sadam.thumbtype.mobile.AppScreen
import com.sadam.thumbtype.mobile.AppSettings
import com.sadam.thumbtype.mobile.Lesson
import com.sadam.thumbtype.mobile.SessionResult
import com.sadam.thumbtype.mobile.ThumbTypeApplication
import com.sadam.thumbtype.mobile.UserProfile
import com.sadam.thumbtype.mobile.app.navigation.ThumbTypeNavigation
import com.sadam.thumbtype.mobile.app.state.ThumbTypeSavedState
import com.sadam.thumbtype.mobile.data.repository.ThumbTypeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ThumbTypeViewModel(
    application: ThumbTypeApplication,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    private val repository: ThumbTypeRepository = application.container.repository
    private val restoredLesson = ThumbTypeSavedState.restoreLesson(savedStateHandle)

    private val _uiState = MutableStateFlow(
        ThumbTypeUiState(selectedLesson = restoredLesson)
    )
    val uiState: StateFlow<ThumbTypeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val initial = io {
                InitialData(
                    onboarded = repository.isOnboarded(),
                    settings = repository.settings(),
                    profile = repository.profile(),
                    readModels = repository.readModels()
                )
            }
            val restoredScreen = ThumbTypeSavedState.restoreScreen(savedStateHandle, initial.onboarded)
            ThumbTypeSavedState.saveScreen(savedStateHandle, restoredScreen)
            if (restoredScreen == AppScreen.Trainer) {
                ThumbTypeSavedState.saveLesson(savedStateHandle, restoredLesson)
            }
            _uiState.value = ThumbTypeUiState(
                isLoading = false,
                settings = initial.settings,
                profile = initial.profile,
                screen = restoredScreen,
                selectedLesson = restoredLesson,
                readModels = initial.readModels
            )
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
        val current = _uiState.value
        _uiState.value = current.copy(profile = profile)
        viewModelScope.launch {
            val readModels = io {
                repository.saveProfile(profile)
                repository.readModels()
            }
            val latest = _uiState.value
            _uiState.value = latest.copy(
                profile = profile,
                readModels = readModels,
                refreshToken = latest.refreshToken + 1
            )
        }
    }

    fun saveSettings(settings: AppSettings) {
        _uiState.value = _uiState.value.copy(settings = settings)
        viewModelScope.launch { io { repository.saveSettings(settings) } }
    }

    fun skipOnboarding() {
        navigate(AppScreen.Home)
        viewModelScope.launch { io { repository.setOnboarded(true) } }
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
        val lesson = _uiState.value.selectedLesson
        viewModelScope.launch {
            val refreshed = io {
                repository.saveSession(result, lesson)
                RefreshedData(repository.profile(), repository.readModels())
            }
            ThumbTypeSavedState.saveScreen(savedStateHandle, AppScreen.Results)
            val current = _uiState.value
            _uiState.value = current.copy(
                profile = refreshed.profile,
                lastResult = result,
                readModels = refreshed.readModels,
                refreshToken = current.refreshToken + 1,
                screen = AppScreen.Results
            )
        }
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

    fun restoreBackup(raw: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = io { repository.importJson(raw) }
            if (result.isSuccess) {
                val refreshed = io {
                    FullRefresh(
                        settings = repository.settings(),
                        profile = repository.profile(),
                        readModels = repository.readModels()
                    )
                }
                val current = _uiState.value
                _uiState.value = current.copy(
                    settings = refreshed.settings,
                    profile = refreshed.profile,
                    readModels = refreshed.readModels,
                    refreshToken = current.refreshToken + 1
                )
            }
            onResult(result)
        }
    }

    fun exportBackup(onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            val result = runCatching { io { repository.exportJson() } }
            onResult(result)
        }
    }

    fun deleteAllLocalData(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            io { repository.clearAll() }
            ThumbTypeSavedState.clear(savedStateHandle)
            ThumbTypeSavedState.saveScreen(savedStateHandle, AppScreen.Onboarding)
            val current = _uiState.value
            _uiState.value = ThumbTypeUiState(
                isLoading = false,
                screen = AppScreen.Onboarding,
                readModels = ThumbTypeReadModels(),
                refreshToken = current.refreshToken + 1,
                sessionNonce = current.sessionNonce + 1
            )
            onComplete()
        }
    }

    private suspend fun <T> io(block: suspend () -> T): T = withContext(Dispatchers.IO) { block() }

    private data class InitialData(
        val onboarded: Boolean,
        val settings: AppSettings,
        val profile: UserProfile,
        val readModels: ThumbTypeReadModels
    )

    private data class RefreshedData(
        val profile: UserProfile,
        val readModels: ThumbTypeReadModels
    )

    private data class FullRefresh(
        val settings: AppSettings,
        val profile: UserProfile,
        val readModels: ThumbTypeReadModels
    )
}

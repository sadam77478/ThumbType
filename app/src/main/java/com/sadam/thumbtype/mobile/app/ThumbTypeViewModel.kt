package com.sadam.thumbtype.mobile.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.sadam.thumbtype.mobile.AppRepository
import com.sadam.thumbtype.mobile.AppScreen
import com.sadam.thumbtype.mobile.AppSettings
import com.sadam.thumbtype.mobile.Lesson
import com.sadam.thumbtype.mobile.SessionResult
import com.sadam.thumbtype.mobile.TrainingEngine
import com.sadam.thumbtype.mobile.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThumbTypeViewModel(application: Application) : AndroidViewModel(application) {
    val repository = AppRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(
        ThumbTypeUiState(
            settings = repository.settings(),
            profile = repository.profile(),
            screen = if (repository.isOnboarded()) AppScreen.Home else AppScreen.Onboarding
        )
    )
    val uiState: StateFlow<ThumbTypeUiState> = _uiState.asStateFlow()

    fun navigate(screen: AppScreen) {
        _uiState.value = _uiState.value.copy(screen = screen)
    }

    fun goHome() = navigate(AppScreen.Home)

    fun onBack() {
        val current = _uiState.value.screen
        val destination = when (current) {
            AppScreen.Trainer, AppScreen.Results -> AppScreen.Home
            AppScreen.Privacy -> AppScreen.Profile
            AppScreen.Home, AppScreen.Onboarding -> current
            else -> AppScreen.Home
        }
        navigate(destination)
    }

    fun saveProfile(profile: UserProfile) {
        repository.saveProfile(profile)
        _uiState.value = _uiState.value.copy(profile = profile)
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
        val current = _uiState.value
        _uiState.value = current.copy(
            selectedLesson = lesson,
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
        _uiState.value = current.copy(
            profile = repository.profile(),
            lastResult = result,
            refreshToken = current.refreshToken + 1,
            screen = AppScreen.Results
        )
    }

    fun startWeaknessLesson() {
        val weakText = TrainingEngine.generateWeakDrill(
            repository.keyStats(),
            repository.transitionStats()
        )
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
                refreshToken = current.refreshToken + 1
            )
        }
        return result
    }

    fun exportBackupJson(): String = repository.exportJson()

    fun deleteAllLocalData() {
        repository.clearAll()
        val current = _uiState.value
        _uiState.value = ThumbTypeUiState(
            screen = AppScreen.Onboarding,
            refreshToken = current.refreshToken + 1,
            sessionNonce = current.sessionNonce + 1
        )
    }
}

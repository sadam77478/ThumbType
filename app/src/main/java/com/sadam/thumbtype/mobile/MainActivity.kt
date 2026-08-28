package com.sadam.thumbtype.mobile

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ThumbTypeRoot() }
    }
}

@Composable
fun ThumbTypeRoot() {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val repo = remember { AppRepository(context.applicationContext) }
    var settings by remember { mutableStateOf(repo.settings()) }
    var profile by remember { mutableStateOf(repo.profile()) }
    var screen by remember { mutableStateOf(if (repo.isOnboarded()) AppScreen.Home else AppScreen.Onboarding) }
    var selectedLesson by remember { mutableStateOf(LessonRepository.lessons.first()) }
    var lastResult by remember { mutableStateOf<SessionResult?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    var sessionNonce by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun showMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun startLesson(lesson: Lesson) {
        selectedLesson = lesson
        sessionNonce++
        screen = AppScreen.Trainer
    }

    SideEffect {
        activity?.window?.let { window ->
            if (settings.privacyScreenProtection) {
                window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val result = runCatching {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    ?: error("Could not read backup")
                repo.importJson(text).getOrThrow()
            }
            if (result.isSuccess) {
                settings = repo.settings()
                profile = repo.profile()
                refresh++
                showMessage("Backup restored successfully")
            } else {
                showMessage(result.exceptionOrNull()?.message ?: "Could not restore backup")
            }
        }
    }

    BackHandler(enabled = screen != AppScreen.Home && screen != AppScreen.Onboarding) {
        screen = when (screen) {
            AppScreen.Trainer, AppScreen.Results -> AppScreen.Home
            AppScreen.Privacy -> AppScreen.Profile
            else -> AppScreen.Home
        }
    }

    ThumbTypeTheme(settings) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))) {
                when (screen) {
                    AppScreen.Onboarding -> OnboardingScreen(
                        initialProfile = profile,
                        onSaveProfile = {
                            profile = it
                            repo.saveProfile(it)
                        },
                        onBaseline = {
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
                        },
                        onSkip = {
                            repo.setOnboarded(true)
                            screen = AppScreen.Home
                        }
                    )

                    AppScreen.Trainer -> key(sessionNonce) {
                        TrainerScreen(
                            lesson = selectedLesson,
                            settings = settings,
                            profile = profile,
                            onExit = { screen = AppScreen.Home },
                            onComplete = { result ->
                                repo.saveSession(result, selectedLesson)
                                profile = repo.profile()
                                lastResult = result
                                refresh++
                                screen = AppScreen.Results
                            }
                        )
                    }

                    AppScreen.Results -> {
                        val result = lastResult
                        if (result == null) {
                            screen = AppScreen.Home
                        } else {
                            ResultsScreen(
                                result = result,
                                profile = profile,
                                personalBest = repo.bestWpm(),
                                onContinue = { screen = AppScreen.Home },
                                onRetry = { startLesson(selectedLesson) },
                                onWeakness = {
                                    val weakText = TrainingEngine.generateWeakDrill(repo.keyStats(), repo.transitionStats())
                                    startLesson(Lesson(-777, 0, "Weakness Trainer", "Built from your latest performance", weakText, "Weakness", 35, isPractice = true))
                                }
                            )
                        }
                    }

                    AppScreen.Privacy -> PrivacyScreen(
                        onBack = { screen = AppScreen.Profile },
                        onExport = {
                            runCatching { BackupUtils.shareBackup(context, repo.exportJson()) }
                                .onFailure { showMessage(it.message ?: "Could not export backup") }
                        },
                        onImport = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                        onDeleteAll = {
                            repo.clearAll()
                            settings = AppSettings()
                            profile = UserProfile()
                            lastResult = null
                            refresh++
                            screen = AppScreen.Onboarding
                            showMessage("Local ThumbType data deleted")
                        }
                    )

                    else -> Scaffold(
                        containerColor = MaterialTheme.colorScheme.background,
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        bottomBar = {
                            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                                val items = listOf(
                                    Triple(AppScreen.Home, Icons.Default.Home, "Home"),
                                    Triple(AppScreen.Learn, Icons.Default.School, "Learn"),
                                    Triple(AppScreen.Practice, Icons.Default.Bolt, "Practice"),
                                    Triple(AppScreen.Progress, Icons.Default.AutoGraph, "Progress"),
                                    Triple(AppScreen.Profile, Icons.Default.Person, "Profile")
                                )
                                items.forEach { (target, icon, label) ->
                                    NavigationBarItem(
                                        selected = screen == target,
                                        onClick = { screen = target },
                                        icon = { Icon(icon, label) },
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }
                    ) { padding ->
                        Box(Modifier.fillMaxSize().padding(padding)) {
                            when (screen) {
                                AppScreen.Home -> HomeScreen(repo, refresh, ::startLesson) { screen = it }
                                AppScreen.Learn -> LearnScreen(repo, refresh, ::startLesson)
                                AppScreen.Practice -> PracticeScreen(repo, ::startLesson)
                                AppScreen.Progress -> ProgressScreen(repo, refresh)
                                AppScreen.Profile -> ProfileScreen(
                                    repo = repo,
                                    settings = settings,
                                    profile = profile,
                                    onSettings = {
                                        settings = it
                                        repo.saveSettings(it)
                                    },
                                    onProfile = {
                                        profile = it
                                        repo.saveProfile(it)
                                    },
                                    onPrivacy = { screen = AppScreen.Privacy }
                                )
                                else -> Unit
                            }
                        }
                    }
                }

                if (screen in listOf(AppScreen.Onboarding, AppScreen.Trainer, AppScreen.Results, AppScreen.Privacy)) {
                    SnackbarHost(snackbarHostState, Modifier.align(androidx.compose.ui.Alignment.BottomCenter).navigationBarsPadding())
                }
            }
        }
    }
}

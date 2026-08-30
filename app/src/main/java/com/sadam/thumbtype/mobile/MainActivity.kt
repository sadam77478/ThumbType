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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sadam.thumbtype.mobile.app.ThumbTypeViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ThumbTypeRoot() }
    }
}

@Composable
fun ThumbTypeRoot(viewModel: ThumbTypeViewModel = viewModel()) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val repo = viewModel.repository
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun showMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    SideEffect {
        activity?.window?.let { window ->
            if (state.settings.privacyScreenProtection) {
                window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val result = runCatching {
                val text = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() }
                    ?: error("Could not read backup")
                viewModel.restoreBackup(text).getOrThrow()
            }
            if (result.isSuccess) {
                showMessage("Backup restored successfully")
            } else {
                showMessage(result.exceptionOrNull()?.message ?: "Could not restore backup")
            }
        }
    }

    BackHandler(enabled = state.screen != AppScreen.Home && state.screen != AppScreen.Onboarding) {
        viewModel.onBack()
    }

    ThumbTypeTheme(state.settings) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))) {
                when (state.screen) {
                    AppScreen.Onboarding -> OnboardingScreen(
                        initialProfile = state.profile,
                        onSaveProfile = viewModel::saveProfile,
                        onBaseline = viewModel::startBaseline,
                        onSkip = viewModel::skipOnboarding
                    )

                    AppScreen.Trainer -> key(state.sessionNonce) {
                        TrainerScreen(
                            lesson = state.selectedLesson,
                            settings = state.settings,
                            profile = state.profile,
                            onExit = viewModel::goHome,
                            onComplete = viewModel::completeSession
                        )
                    }

                    AppScreen.Results -> {
                        val result = state.lastResult
                        if (result == null) {
                            LaunchedEffect(Unit) { viewModel.goHome() }
                        } else {
                            ResultsScreen(
                                result = result,
                                profile = state.profile,
                                personalBest = repo.bestWpm(),
                                onContinue = viewModel::goHome,
                                onRetry = viewModel::retryCurrentLesson,
                                onWeakness = viewModel::startWeaknessLesson
                            )
                        }
                    }

                    AppScreen.Privacy -> PrivacyScreen(
                        onBack = { viewModel.navigate(AppScreen.Profile) },
                        onExport = {
                            runCatching { BackupUtils.shareBackup(context, viewModel.exportBackupJson()) }
                                .onFailure { showMessage(it.message ?: "Could not export backup") }
                        },
                        onImport = {
                            importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                        },
                        onDeleteAll = {
                            viewModel.deleteAllLocalData()
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
                                        selected = state.screen == target,
                                        onClick = { viewModel.navigate(target) },
                                        icon = { Icon(icon, label) },
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }
                    ) { padding ->
                        Box(Modifier.fillMaxSize().padding(padding)) {
                            when (state.screen) {
                                AppScreen.Home -> HomeScreen(
                                    repo,
                                    state.refreshToken,
                                    viewModel::startLesson,
                                    viewModel::navigate
                                )

                                AppScreen.Learn -> LearnScreen(
                                    repo,
                                    state.refreshToken,
                                    viewModel::startLesson
                                )

                                AppScreen.Practice -> PracticeScreen(repo, viewModel::startLesson)
                                AppScreen.Progress -> ProgressScreen(repo, state.refreshToken)
                                AppScreen.Profile -> ProfileScreen(
                                    repo = repo,
                                    settings = state.settings,
                                    profile = state.profile,
                                    onSettings = viewModel::saveSettings,
                                    onProfile = viewModel::saveProfile,
                                    onPrivacy = { viewModel.navigate(AppScreen.Privacy) }
                                )

                                else -> Unit
                            }
                        }
                    }
                }

                if (state.screen in listOf(AppScreen.Onboarding, AppScreen.Trainer, AppScreen.Results, AppScreen.Privacy)) {
                    SnackbarHost(
                        snackbarHostState,
                        Modifier.align(androidx.compose.ui.Alignment.BottomCenter).navigationBarsPadding()
                    )
                }
            }
        }
    }
}

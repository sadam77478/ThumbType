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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sadam.thumbtype.mobile.app.ThumbTypeViewModel
import com.sadam.thumbtype.mobile.app.ThumbTypeViewModelFactory
import com.sadam.thumbtype.mobile.app.navigation.ThumbTypeNavigation
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
    val application = context.applicationContext as ThumbTypeApplication
    val viewModelFactory = remember(application) { ThumbTypeViewModelFactory(application) }
    val viewModel: ThumbTypeViewModel = viewModel(factory = viewModelFactory)
    val activity = context as? ComponentActivity
    val state by viewModel.uiState.collectAsStateWithLifecycle()
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
            val readResult = runCatching {
                context.contentResolver.openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() }
                    ?: error("Could not read backup")
            }
            readResult.onSuccess { text ->
                viewModel.restoreBackup(text) { result ->
                    if (result.isSuccess) {
                        showMessage("Backup restored successfully")
                    } else {
                        showMessage(result.exceptionOrNull()?.message ?: "Could not restore backup")
                    }
                }
            }.onFailure {
                showMessage(it.message ?: "Could not read backup")
            }
        }
    }

    BackHandler(enabled = !state.isLoading && state.screen != AppScreen.Home && state.screen != AppScreen.Onboarding) {
        viewModel.onBack()
    }

    ThumbTypeTheme(state.settings) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (state.isLoading) {
                Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
                    PremiumCard(modifier = Modifier.widthIn(max = 360.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(strokeWidth = 4.dp)
                            Spacer(Modifier.height(18.dp))
                            Text("Loading ThumbType", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(5.dp))
                            Text(
                                "Preparing your local training data…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))) {
                    when (state.screen) {
                        AppScreen.Onboarding -> ThumbTypeAdaptiveContainer(maxContentWidth = 680.dp) {
                            PremiumOnboardingScreen(
                                initialProfile = state.profile,
                                onSaveProfile = viewModel::saveProfile,
                                onBaseline = viewModel::startBaseline,
                                onSkip = viewModel::skipOnboarding
                            )
                        }

                        AppScreen.Trainer -> key(state.sessionNonce) {
                            val windowInfo = rememberThumbTypeWindowInfo()
                            ThumbTypeAdaptiveContainer(maxContentWidth = windowInfo.trainerMaxWidth) {
                                PremiumTrainerScreen(
                                    lesson = state.selectedLesson,
                                    settings = state.settings,
                                    profile = state.profile,
                                    onExit = viewModel::goHome,
                                    onComplete = viewModel::completeSession
                                )
                            }
                        }

                        AppScreen.Results -> {
                            val result = state.lastResult
                            if (result == null) {
                                LaunchedEffect(Unit) { viewModel.goHome() }
                            } else {
                                ThumbTypeAdaptiveContainer(maxContentWidth = 760.dp) {
                                    PremiumResultsScreen(
                                        result = result,
                                        profile = state.profile,
                                        personalBest = state.readModels.home.bestWpm,
                                        onContinue = viewModel::goHome,
                                        onRetry = viewModel::retryCurrentLesson,
                                        onWeakness = viewModel::startWeaknessLesson
                                    )
                                }
                            }
                        }

                        AppScreen.Privacy -> ThumbTypeAdaptiveContainer(maxContentWidth = 760.dp) {
                            PremiumPrivacyScreen(
                                onBack = { viewModel.navigate(AppScreen.Profile) },
                                onExport = {
                                    viewModel.exportBackup { exportResult ->
                                        exportResult.onSuccess { json ->
                                            runCatching { BackupUtils.shareBackup(context, json) }
                                                .onFailure { showMessage(it.message ?: "Could not export backup") }
                                        }.onFailure {
                                            showMessage(it.message ?: "Could not create backup")
                                        }
                                    }
                                },
                                onImport = {
                                    importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                                },
                                onDeleteAll = {
                                    viewModel.deleteAllLocalData {
                                        showMessage("Local ThumbType data deleted")
                                    }
                                }
                            )
                        }

                        else -> ThumbTypeAppShell(
                            currentScreen = state.screen,
                            snackbarHostState = snackbarHostState,
                            onNavigate = viewModel::navigate
                        ) {
                            when (state.screen) {
                                AppScreen.Home -> PremiumHomeScreen(
                                    data = state.readModels.home,
                                    onStart = viewModel::startLesson,
                                    onNavigate = viewModel::navigate
                                )

                                AppScreen.Learn -> PremiumLearnScreen(
                                    data = state.readModels.learn,
                                    onStart = viewModel::startLesson
                                )

                                AppScreen.Practice -> PremiumPracticeScreen(
                                    data = state.readModels.practice,
                                    onStart = viewModel::startLesson
                                )

                                AppScreen.Progress -> PremiumProgressScreen(state.readModels.progress)

                                AppScreen.Profile -> PremiumProfileScreen(
                                    data = state.readModels.profile,
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

                    if (state.screen in ThumbTypeNavigation.fullScreenDestinations) {
                        SnackbarHost(
                            snackbarHostState,
                            Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                        )
                    }
                }
            }
        }
    }
}

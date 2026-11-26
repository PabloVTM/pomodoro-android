package com.pva.pomodoro.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pva.pomodoro.PomodoroViewModel
import com.pva.pomodoro.timer.PomodoroTimerService
import com.pva.pomodoro.ui.theme.PomodoroTheme

@Composable
fun PomodoroApp(viewModel: PomodoroViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    KeepScreenOn(state.isRunning)
    RequestNotificationPermission()

    LaunchedEffect(state.isRunning, state.settings.notificationsEnabled) {
        PomodoroTimerService.sync(context, state)
    }

    PomodoroTheme {
        PomodoroScreen(
            state = state,
            onToggleTimer = viewModel::toggleTimer,
            onReset = viewModel::reset,
            onSkip = viewModel::skip,
            onOpenSettings = { showSettings = true },
        )

        if (showSettings) {
            SettingsDialog(
                settings = state.settings,
                onSettingsChange = viewModel::applySettings,
                onResetSessionCount = viewModel::resetSessionCount,
                onDismiss = { showSettings = false },
            )
        }
    }
}

/** Garde l'écran allumé pendant une session, pour suivre le compte à rebours. */
@Composable
private fun KeepScreenOn(active: Boolean) {
    val view = LocalView.current
    SideEffect { view.keepScreenOn = active }
}

/** Demande la permission d'afficher des notifications (Android 13+). */
@Composable
private fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Le minuteur fonctionne aussi sans notification. */ }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
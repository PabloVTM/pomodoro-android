package com.pva.pomodoro.wear.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Typography
import com.pva.pomodoro.wear.R
import com.pva.pomodoro.wear.model.Phase
import com.pva.pomodoro.wear.model.WearTimerState
import com.pva.pomodoro.wear.sync.WearProtocol
import com.pva.pomodoro.wear.sync.WearRepository
import com.pva.pomodoro.wear.util.formatClock
import java.util.Locale
import kotlinx.coroutines.delay

// Palette PabloSystems : sauge profonde, ivoire chaud, vermillon vif.
private val DeepSage = Color(0xFF4B5A50)
private val WarmIvory = Color(0xFFF1EEE3)
private val Vermillion = Color(0xFFF20B0B)
private val InkLight = Color(0xFF2B332D)
private val InkDark = Color(0xFF111512)
private val SurfaceDark = Color(0xFF1A201C)
private val SageLight = Color(0xFFA9C0B2)
private val VermillionLight = Color(0xFFFF6B5E)
private val LightSageSurface = Color(0xFFF7F4E9)
private val DarkSageVariant = Color(0xFFB7C2BA)

private val LightColors = Colors(
    primary = DeepSage,
    primaryVariant = Color(0xFF33413A),
    secondary = Vermillion,
    secondaryVariant = Color(0xFFB30000),
    background = WarmIvory,
    surface = LightSageSurface,
    error = Vermillion,
    onPrimary = WarmIvory,
    onSecondary = WarmIvory,
    onBackground = InkLight,
    onSurface = InkLight,
    onSurfaceVariant = Color(0xFF5D675F),
    onError = WarmIvory,
)

private val DarkColors = Colors(
    primary = SageLight,
    primaryVariant = DeepSage,
    secondary = VermillionLight,
    secondaryVariant = Vermillion,
    background = InkDark,
    surface = SurfaceDark,
    error = VermillionLight,
    onPrimary = InkDark,
    onSecondary = InkDark,
    onBackground = WarmIvory,
    onSurface = WarmIvory,
    onSurfaceVariant = DarkSageVariant,
    onError = InkDark,
)

private val WearTypography = Typography(defaultFontFamily = FontFamily.Monospace)

@Composable
fun WearPomodoroTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = WearTypography,
        content = content,
    )
}

@Composable
fun WearApp() {
    val context = LocalContext.current
    val repository = remember { WearRepository(context) }
    val state by repository.state.collectAsStateWithLifecycle()
    val offline by repository.offline.collectAsStateWithLifecycle()

    DisposableEffect(repository) {
        repository.start()
        onDispose { repository.close() }
    }

    WearPomodoroTheme {
        WearTimerScreen(
            state = state,
            offline = offline,
            onToggle = {
                val action = if (state?.isRunning == true) {
                    WearProtocol.ACTION_PAUSE
                } else {
                    WearProtocol.ACTION_START
                }
                repository.sendAction(action)
            },
            onReset = { repository.sendAction(WearProtocol.ACTION_RESET) },
            onSkip = { repository.sendAction(WearProtocol.ACTION_SKIP) },
        )
    }
}

@Composable
private fun WearTimerScreen(
    state: WearTimerState?,
    offline: Boolean,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    onSkip: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
    ) {
        TimeText(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 4.dp),
        )

        if (state == null) {
            WaitingContent()
            return@Box
        }

        TimerView(
            state = state,
            offline = offline,
            onToggle = onToggle,
            onReset = onReset,
            onSkip = onSkip,
        )
    }
}

@Composable
private fun WaitingContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.wear_waiting),
            color = MaterialTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun TimerView(
    state: WearTimerState,
    offline: Boolean,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    onSkip: () -> Unit,
) {
    val dark = isSystemInDarkTheme()
    val accent = phaseAccent(state.phase, dark)
    val onAccent = if (dark) InkDark else WarmIvory
    val trackColor = MaterialTheme.colors.onBackground.copy(alpha = 0.12f)

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.isRunning, state.endAtLocalMillis) {
        if (state.isRunning) {
            while (true) {
                now = System.currentTimeMillis()
                if (state.endAtLocalMillis - now <= 0L) break
                delay(250)
            }
        }
    }

    val remaining = if (state.isRunning) {
        (state.endAtLocalMillis - now).coerceAtLeast(0L)
    } else {
        state.remainingMillis
    }
    val progress = if (state.totalMillis > 0L) {
        (remaining.toFloat() / state.totalMillis).coerceIn(0f, 1f)
    } else {
        0f
    }
    val status = when {
        offline -> R.string.wear_status_offline
        state.isRunning -> R.string.wear_status_running
        remaining == state.totalMillis -> R.string.wear_status_ready
        else -> R.string.wear_status_paused
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 14.dp, end = 14.dp, top = 26.dp, bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "[ ${stringResource(state.phase.labelRes).lowercase(Locale.getDefault())} ]",
            color = accent,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(Modifier.height(3.dp))

        Box(
            modifier = Modifier.size(102.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 6.dp.toPx()
                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                drawArc(
                    color = accent,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatClock(remaining),
                    color = MaterialTheme.colors.onBackground,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 25.sp,
                )
                Text(
                    text = "> ${stringResource(status).lowercase(Locale.getDefault())}",
                    color = if (offline) MaterialTheme.colors.error
                    else MaterialTheme.colors.onSurfaceVariant,
                    fontSize = 8.sp,
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = onToggle,
            modifier = Modifier
                .width(142.dp)
                .height(44.dp),
            colors = ButtonDefaults.primaryButtonColors(
                backgroundColor = accent,
                contentColor = onAccent,
            ),
        ) {
            Text(
                text = stringResource(
                    if (state.isRunning) R.string.wear_action_pause
                    else R.string.wear_action_start,
                ),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(2.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActionLabel(stringResource(R.string.wear_action_reset), onReset)
            ActionLabel(stringResource(R.string.wear_action_skip), onSkip)
        }
    }
}

@Composable
private fun ActionLabel(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        color = MaterialTheme.colors.onSurfaceVariant,
        fontSize = 9.sp,
    )
}

private val Phase.labelRes: Int
    get() = when (this) {
        Phase.WORK -> R.string.wear_phase_work
        Phase.SHORT_BREAK -> R.string.wear_phase_short_break
        Phase.LONG_BREAK -> R.string.wear_phase_long_break
    }

private fun phaseAccent(phase: Phase, dark: Boolean): Color = when (phase) {
    Phase.WORK -> if (dark) VermillionLight else Vermillion
    Phase.SHORT_BREAK -> if (dark) SageLight else DeepSage
    Phase.LONG_BREAK -> if (dark) Color(0xFFC9DCCF) else Color(0xFF33413A)
}
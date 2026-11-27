package com.pva.pomodoro.ui

import android.content.res.Configuration
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pva.pomodoro.R
import com.pva.pomodoro.model.Phase
import com.pva.pomodoro.model.PomodoroSettings
import com.pva.pomodoro.model.TimerState
import com.pva.pomodoro.model.labelRes
import com.pva.pomodoro.ui.theme.PhaseColors
import com.pva.pomodoro.ui.theme.PomodoroTheme
import com.pva.pomodoro.ui.theme.phaseColors
import com.pva.pomodoro.shared.formatClock
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroScreen(
    state: TimerState,
    onToggleTimer: () -> Unit,
    onReset: () -> Unit,
    onSkip: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val colors = phaseColors(state.phase)
    val targetProgress = if (state.totalMillis > 0L) {
        state.remainingMillis.toFloat() / state.totalMillis
    } else {
        0f
    }
    val progress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 300, easing = LinearEasing),
        label = "timerProgress",
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { PromptTitle() },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings_title),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    actionIconContentColor = MaterialTheme.colorScheme.primary,
                ),
            )
        },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
        ) {
            val dialSize = minOf(maxWidth * 0.78f, 320.dp)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PhaseBadge(state.phase, colors)

                Spacer(Modifier.weight(1f))

                TimerDial(state, dialSize, colors, progress)

                Spacer(Modifier.weight(1f))

                SessionProgress(state)

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = onToggleTimer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.onAccent,
                    ),
                ) {
                    Text(
                        text = stringResource(
                            if (state.isRunning) R.string.action_pause else R.string.action_start,
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    TextButton(onClick = onReset) {
                        Text(stringResource(R.string.action_reset))
                    }
                    TextButton(onClick = onSkip) {
                        Text(stringResource(R.string.action_skip))
                    }
                }
            }
        }
    }
}

@Composable
private fun PromptTitle() {
    val cursorAlpha by rememberInfiniteTransition(label = "cursor").animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 530, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cursorAlpha",
    )
    val accent = MaterialTheme.colorScheme.secondary

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground)) {
                    append("pablo@systems")
                }
                withStyle(SpanStyle(color = accent)) {
                    append(":~/pomodoro")
                }
            },
            fontFamily = FontFamily.Monospace,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "▌",
            modifier = Modifier.alpha(cursorAlpha),
            fontFamily = FontFamily.Monospace,
            fontSize = 15.sp,
            color = accent,
        )
    }
}

@Composable
private fun PhaseBadge(phase: Phase, colors: PhaseColors) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = colors.accent)) { append("[ ") }
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                    append(stringResource(phase.labelRes).lowercase(Locale.getDefault()))
                }
                withStyle(SpanStyle(color = colors.accent)) { append(" ]") }
            },
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun TimerDial(
    state: TimerState,
    size: Dp,
    colors: PhaseColors,
    progress: Float,
) {
    val statusRes = when {
        state.isRunning -> R.string.status_running
        state.remainingMillis == state.totalMillis -> R.string.status_ready
        else -> R.string.status_paused
    }

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxSize(),
            color = colors.accent,
            strokeWidth = 10.dp,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formatClock(state.remainingMillis),
                fontFamily = FontFamily.Monospace,
                fontSize = 58.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = colors.accent)) { append("> ") }
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                        append(stringResource(statusRes).lowercase(Locale.getDefault()))
                    }
                },
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun SessionProgress(state: TimerState) {
    val settings = state.settings
    val accent = MaterialTheme.colorScheme.secondary
    val sessionsBeforeLongBreak = settings.sessionsBeforeLongBreak.coerceAtLeast(1)
    val filled = if (state.phase == Phase.LONG_BREAK) {
        sessionsBeforeLongBreak
    } else {
        state.completedWorkSessions % sessionsBeforeLongBreak
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(sessionsBeforeLongBreak) { index ->
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(
                            if (index < filled) accent
                            else MaterialTheme.colorScheme.surfaceVariant,
                        ),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = pluralStringResource(
                R.plurals.pomodoro_count,
                state.completedWorkSessions,
                state.completedWorkSessions,
            ),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true, name = "Session en cours")
@Composable
private fun PomodoroScreenPreview() {
    PomodoroTheme {
        PomodoroScreen(
            state = TimerState(
                remainingMillis = 18 * 60_000L + 42_000L,
                totalMillis = 25 * 60_000L,
                isRunning = true,
                completedWorkSessions = 2,
                settings = PomodoroSettings(),
            ),
            onToggleTimer = {},
            onReset = {},
            onSkip = {},
            onOpenSettings = {},
        )
    }
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "Pause (nuit)",
)
@Composable
private fun PomodoroScreenDarkPreview() {
    PomodoroTheme {
        PomodoroScreen(
            state = TimerState(
                phase = Phase.SHORT_BREAK,
                remainingMillis = 4 * 60_000L + 10_000L,
                totalMillis = 5 * 60_000L,
                completedWorkSessions = 3,
                settings = PomodoroSettings(),
            ),
            onToggleTimer = {},
            onReset = {},
            onSkip = {},
            onOpenSettings = {},
        )
    }
}

package com.pva.pomodoro.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pva.pomodoro.R
import com.pva.pomodoro.model.PomodoroSettings
import com.pva.pomodoro.widget.PomodoroWidgetProvider

/** Réglages des durées et du comportement du minuteur. Les changements sont appliqués immédiatement. */
@Composable
fun SettingsDialog(
    settings: PomodoroSettings,
    onSettingsChange: (PomodoroSettings) -> Unit,
    onResetSessionCount: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val widgetManager = remember(context) { AppWidgetManager.getInstance(context) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                StepperRow(
                    label = stringResource(R.string.settings_work),
                    value = settings.workMinutes,
                    valueText = stringResource(R.string.minutes_value, settings.workMinutes),
                    range = 1..120,
                    onValueChange = { onSettingsChange(settings.copy(workMinutes = it)) },
                )
                StepperRow(
                    label = stringResource(R.string.settings_short_break),
                    value = settings.shortBreakMinutes,
                    valueText = stringResource(R.string.minutes_value, settings.shortBreakMinutes),
                    range = 1..60,
                    onValueChange = { onSettingsChange(settings.copy(shortBreakMinutes = it)) },
                )
                StepperRow(
                    label = stringResource(R.string.settings_long_break),
                    value = settings.longBreakMinutes,
                    valueText = stringResource(R.string.minutes_value, settings.longBreakMinutes),
                    range = 1..60,
                    onValueChange = { onSettingsChange(settings.copy(longBreakMinutes = it)) },
                )
                StepperRow(
                    label = stringResource(R.string.settings_sessions),
                    value = settings.sessionsBeforeLongBreak,
                    valueText = stringResource(R.string.sessions_value, settings.sessionsBeforeLongBreak),
                    range = 2..8,
                    onValueChange = { onSettingsChange(settings.copy(sessionsBeforeLongBreak = it)) },
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                SwitchRow(
                    title = stringResource(R.string.settings_auto_start),
                    subtitle = stringResource(R.string.settings_auto_start_desc),
                    checked = settings.autoStartNext,
                    onCheckedChange = { onSettingsChange(settings.copy(autoStartNext = it)) },
                )
                SwitchRow(
                    title = stringResource(R.string.settings_sound),
                    subtitle = stringResource(R.string.settings_sound_desc),
                    checked = settings.soundEnabled,
                    onCheckedChange = { onSettingsChange(settings.copy(soundEnabled = it)) },
                )
                SwitchRow(
                    title = stringResource(R.string.settings_vibration),
                    subtitle = stringResource(R.string.settings_vibration_desc),
                    checked = settings.vibrationEnabled,
                    onCheckedChange = { onSettingsChange(settings.copy(vibrationEnabled = it)) },
                )
                SwitchRow(
                    title = stringResource(R.string.settings_notifications),
                    subtitle = stringResource(R.string.settings_notifications_desc),
                    checked = settings.notificationsEnabled,
                    onCheckedChange = { onSettingsChange(settings.copy(notificationsEnabled = it)) },
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                TextButton(onClick = onResetSessionCount) {
                    Text(stringResource(R.string.settings_reset_sessions))
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = stringResource(R.string.settings_widgets_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = {
                        widgetManager.requestPinAppWidget(
                            ComponentName(context, PomodoroWidgetProvider::class.java),
                            null,
                            null,
                        )
                    },
                    enabled = widgetManager.isRequestPinAppWidgetSupported,
                ) {
                    Text(stringResource(R.string.settings_add_widget))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        },
    )
}

@Composable
private fun StepperRow(
    label: String,
    value: Int,
    valueText: String,
    range: IntRange,
    onValueChange: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepperButton(
                symbol = "−",
                enabled = value > range.first,
                onClick = { onValueChange((value - 1).coerceIn(range)) },
            )
            Text(
                text = valueText,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
            )
            StepperButton(
                symbol = "+",
                enabled = value < range.last,
                onClick = { onValueChange((value + 1).coerceIn(range)) },
            )
        }
    }
}

@Composable
private fun StepperButton(symbol: String, enabled: Boolean, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.size(36.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary,
        ),
    ) {
        Text(text = symbol, fontSize = 18.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
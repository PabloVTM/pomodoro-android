package com.pva.pomodoro.timer

import android.content.Context
import androidx.core.content.edit
import com.pva.pomodoro.data.SettingsRepository
import com.pva.pomodoro.model.CommandOrigin
import com.pva.pomodoro.model.Phase
import com.pva.pomodoro.model.TimerState

/** Persiste l'état du minuteur pour le restaurer après la mort du processus. */
class TimerStateStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val settingsRepository = SettingsRepository(context)

    fun load(): TimerState {
        val settings = settingsRepository.settings
        val phase = runCatching {
            Phase.valueOf(prefs.getString(KEY_PHASE, Phase.WORK.name) ?: Phase.WORK.name)
        }.getOrDefault(Phase.WORK)
        val total = prefs.getLong(KEY_TOTAL, settings.durationMillis(phase))
        val running = prefs.getBoolean(KEY_RUNNING, false)
        val endAt = prefs.getLong(KEY_END_AT, 0L).takeIf { it > 0L }
        val remaining = if (running && endAt != null) {
            (endAt - System.currentTimeMillis()).coerceAtLeast(0L)
        } else {
            prefs.getLong(KEY_REMAINING, total)
        }
        val commandOrigin = runCatching {
            CommandOrigin.valueOf(
                prefs.getString(KEY_COMMAND_ORIGIN, CommandOrigin.PHONE.name)
                    ?: CommandOrigin.PHONE.name,
            )
        }.getOrDefault(CommandOrigin.PHONE)
        return TimerState(
            phase = phase,
            remainingMillis = remaining,
            totalMillis = total,
            isRunning = running,
            endAtMillis = endAt,
            completedWorkSessions = prefs.getInt(KEY_COMPLETED, 0),
            settings = settings,
            commandOrigin = commandOrigin,
        )
    }

    fun save(state: TimerState) {
        prefs.edit {
            putString(KEY_PHASE, state.phase.name)
            putLong(KEY_TOTAL, state.totalMillis)
            putLong(KEY_REMAINING, state.remainingMillis)
            putBoolean(KEY_RUNNING, state.isRunning)
            putLong(KEY_END_AT, state.endAtMillis ?: 0L)
            putInt(KEY_COMPLETED, state.completedWorkSessions)
            putString(KEY_COMMAND_ORIGIN, state.commandOrigin.name)
        }
    }

    private companion object {
        const val PREFS_NAME = "timer_state"
        const val KEY_PHASE = "phase"
        const val KEY_TOTAL = "total_millis"
        const val KEY_REMAINING = "remaining_millis"
        const val KEY_RUNNING = "is_running"
        const val KEY_END_AT = "end_at_millis"
        const val KEY_COMPLETED = "completed_sessions"
        const val KEY_COMMAND_ORIGIN = "command_origin"
    }
}
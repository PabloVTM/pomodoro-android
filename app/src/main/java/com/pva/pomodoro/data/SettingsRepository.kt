package com.pva.pomodoro.data

import android.content.Context
import androidx.core.content.edit
import com.pva.pomodoro.model.PomodoroSettings

/** Stocke les réglages dans SharedPreferences. */
class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val settings: PomodoroSettings
        get() = PomodoroSettings(
            workMinutes = prefs.getInt(KEY_WORK, 25),
            shortBreakMinutes = prefs.getInt(KEY_SHORT_BREAK, 5),
            longBreakMinutes = prefs.getInt(KEY_LONG_BREAK, 15),
            sessionsBeforeLongBreak = prefs.getInt(KEY_SESSIONS, 4),
            autoStartNext = prefs.getBoolean(KEY_AUTO_START, true),
            soundEnabled = prefs.getBoolean(KEY_SOUND, true),
            notificationsEnabled = prefs.getBoolean(KEY_NOTIFICATIONS, true),
        )

    fun save(settings: PomodoroSettings) {
        prefs.edit {
            putInt(KEY_WORK, settings.workMinutes)
            putInt(KEY_SHORT_BREAK, settings.shortBreakMinutes)
            putInt(KEY_LONG_BREAK, settings.longBreakMinutes)
            putInt(KEY_SESSIONS, settings.sessionsBeforeLongBreak)
            putBoolean(KEY_AUTO_START, settings.autoStartNext)
            putBoolean(KEY_SOUND, settings.soundEnabled)
            putBoolean(KEY_NOTIFICATIONS, settings.notificationsEnabled)
        }
    }

    private companion object {
        const val PREFS_NAME = "pomodoro_settings"
        const val KEY_WORK = "work_minutes"
        const val KEY_SHORT_BREAK = "short_break_minutes"
        const val KEY_LONG_BREAK = "long_break_minutes"
        const val KEY_SESSIONS = "sessions_before_long_break"
        const val KEY_AUTO_START = "auto_start_next"
        const val KEY_SOUND = "sound_enabled"
        const val KEY_NOTIFICATIONS = "notifications_enabled"
    }
}
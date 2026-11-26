package com.pva.pomodoro.model

/** Préférences de l'utilisateur, persistées entre deux lancements. */
data class PomodoroSettings(
    val workMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val sessionsBeforeLongBreak: Int = 4,
    val autoStartNext: Boolean = true,
    val soundEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
) {
    fun durationMillis(phase: Phase): Long = when (phase) {
        Phase.WORK -> workMinutes
        Phase.SHORT_BREAK -> shortBreakMinutes
        Phase.LONG_BREAK -> longBreakMinutes
    }.toLong() * 60_000L
}
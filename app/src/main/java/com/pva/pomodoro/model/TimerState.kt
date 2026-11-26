package com.pva.pomodoro.model

/**
 * État partagé du minuteur, utilisé par l'interface, le service, les widgets
 * et les alarmes.
 */
data class TimerState(
    val phase: Phase = Phase.WORK,
    val remainingMillis: Long = 25 * 60_000L,
    val totalMillis: Long = 25 * 60_000L,
    val isRunning: Boolean = false,
    val endAtMillis: Long? = null,
    val completedWorkSessions: Int = 0,
    val settings: PomodoroSettings = PomodoroSettings(),
)
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
    /**
     * Appareil à l'origine de la dernière commande. Persisté afin qu'une
     * session lancée depuis la montre reste annoncée par la montre, même
     * après une mort du processus ou un redémarrage.
     */
    val commandOrigin: CommandOrigin = CommandOrigin.PHONE,
)

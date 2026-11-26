package com.pva.pomodoro.model

/** Règles d'enchaînement des phases d'un cycle Pomodoro. */
object PomodoroCycle {

    /**
     * Détermine la phase qui suit [current].
     *
     * [completedWorkSessions] est le nombre de sessions de concentration
     * terminées, session courante incluse si elle vient d'être achevée.
     */
    fun nextPhase(
        current: Phase,
        completedWorkSessions: Int,
        sessionsBeforeLongBreak: Int,
    ): Phase = when {
        current != Phase.WORK -> Phase.WORK
        completedWorkSessions > 0 &&
            sessionsBeforeLongBreak > 0 &&
            completedWorkSessions % sessionsBeforeLongBreak == 0 -> Phase.LONG_BREAK
        else -> Phase.SHORT_BREAK
    }
}
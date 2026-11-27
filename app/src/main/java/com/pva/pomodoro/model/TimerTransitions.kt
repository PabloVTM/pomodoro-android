package com.pva.pomodoro.model

/**
 * Calcule l'état suivant après la fin (ou le passage volontaire) d'une phase.
 *
 * Logique pure, extraite du moteur pour être testable sans Android.
 */
object TimerTransitions {

    fun afterPhase(
        current: TimerState,
        counted: Boolean,
        autoStart: Boolean,
        now: Long,
    ): TimerState {
        var completed = current.completedWorkSessions
        if (current.phase == Phase.WORK && counted) completed += 1

        val nextPhase = PomodoroCycle.nextPhase(
            current = current.phase,
            completedWorkSessions = completed,
            sessionsBeforeLongBreak = current.settings.sessionsBeforeLongBreak,
        )
        val nextTotal = current.settings.durationMillis(nextPhase)
        return current.copy(
            phase = nextPhase,
            completedWorkSessions = completed,
            totalMillis = nextTotal,
            remainingMillis = nextTotal,
            isRunning = autoStart,
            endAtMillis = if (autoStart) now + nextTotal else null,
        )
    }

    /**
     * Recalcule proportionnellement le temps restant lorsqu'une durée change en
     * cours de session : la fraction déjà écoulée est conservée, l'anneau de
     * progression reste cohérent.
     */
    fun rescaleRemaining(current: TimerState, newTotal: Long, now: Long): TimerState {
        if (current.totalMillis <= 0L || current.remainingMillis >= current.totalMillis) {
            return current.copy(totalMillis = newTotal, remainingMillis = newTotal)
        }
        val ratio = current.remainingMillis.toDouble() / current.totalMillis
        val newRemaining = (newTotal * ratio).toLong().coerceIn(0L, newTotal)
        return current.copy(
            totalMillis = newTotal,
            remainingMillis = newRemaining,
            endAtMillis = if (current.isRunning) now + newRemaining else null,
        )
    }
}

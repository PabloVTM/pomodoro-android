package com.pva.pomodoro.wear.model

/** Phases partagées avec le téléphone (mêmes noms sérialisés). */
enum class Phase {
    WORK,
    SHORT_BREAK,
    LONG_BREAK,
}

/**
 * État du minuteur reçu du téléphone.
 *
 * `endAtPhoneMillis` est exprimé dans l'horloge du téléphone ;
 * `clockOffsetMillis` vaut « heure téléphone - heure montre », mesuré par un
 * aller-retour de messages. La fin locale est donc `endAt - offset`.
 */
data class WearTimerState(
    val phase: Phase,
    val totalMillis: Long,
    val remainingMillis: Long,
    val isRunning: Boolean,
    val endAtPhoneMillis: Long,
    val clockOffsetMillis: Long,
    val completedWorkSessions: Int,
    val sessionsBeforeLongBreak: Int,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
) {
    val endAtLocalMillis: Long
        get() = if (endAtPhoneMillis > 0L) endAtPhoneMillis - clockOffsetMillis else 0L
}
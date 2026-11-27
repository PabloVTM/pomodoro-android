package com.pva.pomodoro.wear.sync

import com.google.android.gms.wearable.DataMap
import com.pva.pomodoro.wear.model.Phase
import com.pva.pomodoro.wear.model.WearTimerState

/**
 * Décode l'état publié par le téléphone. Logique partagée par le dépôt
 * ([WearRepository]) et le service de fond ([WearStateListenerService]).
 */
object WearStateParser {

    fun parse(bytes: ByteArray, clockOffsetMillis: Long, now: Long): WearTimerState? {
        val map = runCatching { DataMap.fromByteArray(bytes) }.getOrNull() ?: return null
        val phase = runCatching {
            Phase.valueOf(map.getString(WearProtocol.KEY_PHASE) ?: Phase.WORK.name)
        }.getOrDefault(Phase.WORK)
        val running = map.getBoolean(WearProtocol.KEY_RUNNING)
        val endAtPhone = if (running) map.getLong(WearProtocol.KEY_END_AT) else 0L
        val remaining = if (running && endAtPhone > 0L) {
            (endAtPhone - clockOffsetMillis - now).coerceAtLeast(0L)
        } else {
            map.getLong(WearProtocol.KEY_REMAINING_MS)
        }

        return WearTimerState(
            phase = phase,
            totalMillis = map.getLong(WearProtocol.KEY_TOTAL_MS),
            remainingMillis = remaining,
            isRunning = running,
            endAtPhoneMillis = endAtPhone,
            clockOffsetMillis = clockOffsetMillis,
            completedWorkSessions = map.getInt(WearProtocol.KEY_COMPLETED),
            sessionsBeforeLongBreak = map.getInt(WearProtocol.KEY_SESSIONS).coerceAtLeast(1),
            soundEnabled = map.getBoolean(WearProtocol.KEY_SOUND, true),
            vibrationEnabled = map.getBoolean(WearProtocol.KEY_VIBRATION, true),
        )
    }
}

package com.pva.pomodoro.sync

import android.content.Context
import com.pva.pomodoro.model.TimerState
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

/**
 * Publie l'état du minuteur vers la montre. Seuls les changements structurels
 * sont envoyés : la montre calcule elle-même le décompte à partir de `endAt`.
 */
object WearStatePublisher {

    @Volatile
    private var lastSignature: String? = null

    @Synchronized
    fun publish(context: Context, state: TimerState, force: Boolean = false) {
        val signature = signatureOf(state)
        if (!force && signature == lastSignature) return
        lastSignature = signature
        if (!playServicesAvailable(context)) return

        val now = System.currentTimeMillis()
        val request = PutDataMapRequest.create(WearProtocol.STATE_PATH).apply {
            dataMap.putString(WearProtocol.KEY_PHASE, state.phase.name)
            dataMap.putLong(WearProtocol.KEY_TOTAL_MS, state.totalMillis)
            dataMap.putLong(WearProtocol.KEY_REMAINING_MS, state.remainingMillis)
            dataMap.putBoolean(WearProtocol.KEY_RUNNING, state.isRunning)
            dataMap.putLong(WearProtocol.KEY_END_AT, state.endAtMillis ?: 0L)
            dataMap.putInt(WearProtocol.KEY_COMPLETED, state.completedWorkSessions)
            dataMap.putInt(WearProtocol.KEY_WORK_MIN, state.settings.workMinutes)
            dataMap.putInt(WearProtocol.KEY_SHORT_BREAK_MIN, state.settings.shortBreakMinutes)
            dataMap.putInt(WearProtocol.KEY_LONG_BREAK_MIN, state.settings.longBreakMinutes)
            dataMap.putInt(WearProtocol.KEY_SESSIONS, state.settings.sessionsBeforeLongBreak)
            dataMap.putBoolean(WearProtocol.KEY_SOUND, state.settings.soundEnabled)
            dataMap.putBoolean(WearProtocol.KEY_VIBRATION, state.settings.vibrationEnabled)
            dataMap.putLong(WearProtocol.KEY_UPDATED_AT, now)
        }.asPutDataRequest().setUrgent()

        Wearable.getDataClient(context.applicationContext)
            .putDataItem(request)
            .addOnFailureListener { lastSignature = null }
    }

    private fun signatureOf(state: TimerState): String = buildString {
        append(state.phase).append('|')
        append(state.totalMillis).append('|')
        append(state.isRunning).append('|')
        append(state.endAtMillis).append('|')
        append(state.completedWorkSessions).append('|')
        append(state.settings.workMinutes).append('|')
        append(state.settings.shortBreakMinutes).append('|')
        append(state.settings.longBreakMinutes).append('|')
        append(state.settings.sessionsBeforeLongBreak)
        append('|').append(state.settings.soundEnabled)
        append('|').append(state.settings.vibrationEnabled)
        if (!state.isRunning) {
            append('|').append(state.remainingMillis)
        }
    }

    private fun playServicesAvailable(context: Context): Boolean =
        GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
}
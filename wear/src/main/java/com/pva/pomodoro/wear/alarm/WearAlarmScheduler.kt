package com.pva.pomodoro.wear.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.pva.pomodoro.wear.model.WearTimerState

/**
 * Programme l'alerte de fin de phase sur la montre, même quand l'écran est en
 * veille ou l'application en arrière-plan.
 */
class WearAlarmScheduler(private val context: Context) {

    private val alarmManager: AlarmManager? =
        context.getSystemService(AlarmManager::class.java)

    fun sync(state: WearTimerState?) {
        val endAt = state?.takeIf { it.isRunning }?.endAtLocalMillis ?: 0L
        if (endAt > System.currentTimeMillis()) schedule(endAt) else cancel()
    }

    fun cancel() {
        runCatching { alarmManager?.cancel(pendingIntent()) }
    }

    private fun schedule(endAt: Long) {
        val manager = alarmManager ?: return
        val intent = pendingIntent()
        val scheduled = runCatching {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAt, intent)
        }
        if (scheduled.isFailure) {
            runCatching { manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAt, intent) }
        }
    }

    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, WearAlarmReceiver::class.java).setAction(ACTION_PHASE_END),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val ACTION_PHASE_END = "com.pva.pomodoro.wear.action.PHASE_END"

        private const val REQUEST_CODE = 42
    }
}
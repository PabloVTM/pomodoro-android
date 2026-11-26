package com.pva.pomodoro.wear.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.pva.pomodoro.wear.model.WearTimerState

/**
 * Programme l'alerte de fin de phase sur la montre, même quand l'écran est en
 * veille ou l'application en arrière-plan.
 *
 * Les réglages son/vibration du téléphone sont transportés par l'alarme afin de
 * rester appliqués même si l'application n'est plus ouverte.
 */
class WearAlarmScheduler(private val context: Context) {

    private val alarmManager: AlarmManager? =
        context.getSystemService(AlarmManager::class.java)

    fun sync(state: WearTimerState?) {
        val running = state?.takeIf { it.isRunning }
        if (running == null) {
            cancel()
            return
        }
        val endAt = running.endAtLocalMillis
        if (endAt > System.currentTimeMillis()) {
            schedule(endAt, running.vibrationEnabled, running.soundEnabled)
        } else {
            cancel()
        }
    }

    fun cancel() {
        runCatching { alarmManager?.cancel(pendingIntent(vibrate = false, beep = false)) }
    }

    private fun schedule(endAt: Long, vibrate: Boolean, beep: Boolean) {
        val manager = alarmManager ?: return
        val intent = pendingIntent(vibrate, beep)
        val scheduled = runCatching {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAt, intent)
        }
        if (scheduled.isFailure) {
            runCatching { manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAt, intent) }
        }
    }

    private fun pendingIntent(vibrate: Boolean, beep: Boolean): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, WearAlarmReceiver::class.java)
                .setAction(ACTION_PHASE_END)
                .putExtra(EXTRA_VIBRATE, vibrate)
                .putExtra(EXTRA_BEEP, beep),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        const val ACTION_PHASE_END = "com.pva.pomodoro.wear.action.PHASE_END"
        const val EXTRA_VIBRATE = "extra_vibrate"
        const val EXTRA_BEEP = "extra_beep"

        private const val REQUEST_CODE = 42
    }
}

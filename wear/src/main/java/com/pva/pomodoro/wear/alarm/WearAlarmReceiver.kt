package com.pva.pomodoro.wear.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/** Alerte de fin de phase déclenchée par [WearAlarmScheduler]. */
class WearAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != WearAlarmScheduler.ACTION_PHASE_END) return

        val vibrate = intent.getBooleanExtra(WearAlarmScheduler.EXTRA_VIBRATE, true)
        val beep = intent.getBooleanExtra(WearAlarmScheduler.EXTRA_BEEP, true)
        if (!vibrate && !beep) return

        Log.i(TAG, "Alerte de fin de phase (vibration=$vibrate, son=$beep)")
        // Laisse le processus vivre le temps du bip.
        val pending = goAsync()
        Thread {
            try {
                WearAlerter.alert(context, vibrate = vibrate, beep = beep)
            } finally {
                pending.finish()
            }
        }.start()
    }

    private companion object {
        const val TAG = "WearAlarm"
    }
}

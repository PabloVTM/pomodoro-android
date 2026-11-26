package com.pva.pomodoro.wear.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/** Alerte de fin de phase déclenchée par [WearAlarmScheduler]. */
class WearAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != WearAlarmScheduler.ACTION_PHASE_END) return

        Log.i(TAG, "Alerte de fin de phase")
        // Laisse le processus vivre le temps du bip.
        val pending = goAsync()
        Thread {
            try {
                WearAlerter.alert(context)
            } finally {
                pending.finish()
            }
        }.start()
    }

    private companion object {
        const val TAG = "WearAlarm"
    }
}
package com.pva.pomodoro.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Reçoit l'alarme exacte de fin de phase. */
class TimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TimerActions.PHASE_END) return
        val engine = TimerEngine.get(context)
        engine.onAlarmFired()
        PomodoroTimerService.sync(context, engine.state.value)
    }
}
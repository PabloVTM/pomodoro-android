package com.pva.pomodoro.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Exécute les actions des widgets et de la notification. */
class TimerActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val engine = TimerEngine.get(context)
        when (intent.action) {
            TimerActions.TOGGLE -> engine.toggle()
            TimerActions.START -> engine.start()
            TimerActions.PAUSE -> engine.pause()
            TimerActions.RESET -> engine.reset()
            TimerActions.SKIP -> engine.skip()
        }
        PomodoroTimerService.sync(context, engine.state.value)
    }
}
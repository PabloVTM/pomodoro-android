package com.pva.pomodoro.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Au démarrage de l'appareil, restaure l'état du minuteur et reprogramme
 * l'alarme de fin de phase.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val engine = TimerEngine.get(context)
        PomodoroTimerService.sync(context, engine.state.value)
    }
}
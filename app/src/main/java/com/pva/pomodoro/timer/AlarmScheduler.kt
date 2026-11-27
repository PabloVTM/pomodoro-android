package com.pva.pomodoro.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi

/** Programme l'alarme exacte qui clôt la phase en cours, même en veille. */
object AlarmScheduler {

    private const val REQUEST_CODE = 1001

    /**
     * Indique si l'application peut programmer des alarmes exactes. Sur
     * Android 12 (API 31) et 32, la permission `SCHEDULE_EXACT_ALARM` doit être
     * accordée par l'utilisateur ; à partir d'Android 13, `USE_EXACT_ALARM`
     * l'accorde automatiquement.
     */
    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val manager = context.getSystemService(AlarmManager::class.java) ?: return false
        return manager.canScheduleExactAlarms()
    }

    /** Intent de réglage permettant à l'utilisateur d'autoriser les alarmes exactes. */
    @RequiresApi(Build.VERSION_CODES.S)
    fun requestExactAlarmPermissionIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            Uri.fromParts("package", context.packageName, null),
        )

    fun schedule(context: Context, endAtMillis: Long) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = phaseEndIntent(context)
        try {
            val canBeExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                manager.canScheduleExactAlarms()
            if (canBeExact) {
                manager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    endAtMillis,
                    pendingIntent,
                )
            } else {
                manager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    endAtMillis,
                    pendingIntent,
                )
            }
        } catch (_: SecurityException) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAtMillis, pendingIntent)
        }
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java)
            ?.cancel(phaseEndIntent(context))
    }

    private fun phaseEndIntent(context: Context): PendingIntent {
        val intent = Intent(context, TimerAlarmReceiver::class.java)
            .setAction(TimerActions.PHASE_END)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
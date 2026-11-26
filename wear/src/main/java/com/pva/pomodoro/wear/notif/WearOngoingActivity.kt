package com.pva.pomodoro.wear.notif

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import com.pva.pomodoro.wear.MainActivity
import com.pva.pomodoro.wear.R
import com.pva.pomodoro.wear.model.Phase
import com.pva.pomodoro.wear.model.WearTimerState

/**
 * Activité en cours pendant la session : elle empêche Wear OS de revenir au
 * cadran et permet de retrouver le minuteur d'un geste.
 */
class WearOngoingActivity(private val context: Context) {

    fun update(state: WearTimerState?) {
        if (state == null || !state.isRunning || state.endAtLocalMillis <= 0L) {
            cancel()
            return
        }
        if (!canPostNotifications()) {
            cancel()
            return
        }

        ensureChannel()
        val touchIntent = touchIntent()

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_pomodoro)
            .setContentTitle(phaseLabel(state.phase))
            .setContentText(context.getString(R.string.wear_ongoing_text))
            .setContentIntent(touchIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(state.endAtLocalMillis)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        val ongoing = OngoingActivity.Builder(context, NOTIFICATION_ID, builder)
            .setStaticIcon(R.drawable.ic_stat_pomodoro)
            .setTouchIntent(touchIntent)
            .setStatus(
                Status.forPart(
                    Status.TimerPart(state.endAtLocalMillis, PAUSED_NONE, state.totalMillis),
                ),
            )
            .build()
        ongoing.apply(context)

        runCatching { notifyNow(builder) }
    }

    @SuppressLint("MissingPermission")
    private fun notifyNow(builder: NotificationCompat.Builder) {
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun cancel() {
        runCatching { NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID) }
    }

    private fun touchIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun phaseLabel(phase: Phase): String = context.getString(
        when (phase) {
            Phase.WORK -> R.string.wear_phase_work
            Phase.SHORT_BREAK -> R.string.wear_phase_short_break
            Phase.LONG_BREAK -> R.string.wear_phase_long_break
        },
    ).replaceFirstChar { it.uppercase() }

    private fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.wear_ongoing_channel),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            },
        )
    }

    private companion object {
        const val CHANNEL_ID = "wear_timer"
        const val NOTIFICATION_ID = 21
        const val PAUSED_NONE = -1L
    }
}
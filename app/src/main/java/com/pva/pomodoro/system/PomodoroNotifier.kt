package com.pva.pomodoro.system

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.pva.pomodoro.MainActivity
import com.pva.pomodoro.R
import com.pva.pomodoro.model.TimerState
import com.pva.pomodoro.model.labelRes
import com.pva.pomodoro.timer.TimerActionReceiver
import com.pva.pomodoro.timer.TimerActions
import com.pva.pomodoro.shared.formatClock

/** Notifications de fin de phase et de minuteur en cours. */
class PomodoroNotifier(private val context: Context) {

    init {
        // Retire l'ancien canal (vibration gérée par l'application désormais).
        runCatching {
            context.getSystemService(NotificationManager::class.java)
                ?.deleteNotificationChannel(CHANNEL_COMPLETION_LEGACY_ID)
        }
        createChannel(
            id = CHANNEL_COMPLETION_ID,
            name = R.string.notification_channel_name,
            description = R.string.notification_channel_description,
            importance = NotificationManager.IMPORTANCE_HIGH,
        ) {
            setSound(null, null)
            // La vibration est gérée par l'application (VibrationPlayer) afin de
            // fonctionner même sans notifications et de rester pilotable par le
            // réglage « Vibration ».
            enableVibration(false)
        }
        createChannel(
            id = CHANNEL_TIMER_ID,
            name = R.string.timer_channel_name,
            description = R.string.timer_channel_description,
            importance = NotificationManager.IMPORTANCE_LOW,
        ) {
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
    }

    fun notifyPhaseComplete(title: String, message: String) {
        if (!canPostNotifications()) return

        val notification = NotificationCompat.Builder(context, CHANNEL_COMPLETION_ID)
            .setSmallIcon(R.drawable.ic_stat_pomodoro)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent())
            .build()

        post(NOTIFICATION_COMPLETION_ID, notification)
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

    @SuppressLint("MissingPermission")
    private fun post(id: Int, notification: android.app.Notification) {
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }

    fun buildTimerNotification(state: TimerState): android.app.Notification {
        val phaseLabel = context.getString(state.phase.labelRes)
        val remaining = formatClock(state.remainingMillis)
        val builder = NotificationCompat.Builder(context, CHANNEL_TIMER_ID)
            .setSmallIcon(R.drawable.ic_stat_pomodoro)
            .setContentTitle(phaseLabel)
            .setContentText(
                context.getString(
                    if (state.isRunning) R.string.timer_notification_running
                    else R.string.timer_notification_paused,
                    remaining,
                ),
            )
            .setContentIntent(openAppIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(0, context.getString(R.string.action_pause), action(TimerActions.TOGGLE, 11))
            .addAction(0, context.getString(R.string.action_reset), action(TimerActions.RESET, 12))
            .addAction(0, context.getString(R.string.action_skip), action(TimerActions.SKIP, 13))

        val endAt = state.endAtMillis
        if (state.isRunning && endAt != null) {
            builder.setUsesChronometer(true)
                .setWhen(endAt)
                .setChronometerCountDown(true)
                .setShowWhen(true)
        }
        return builder.build()
    }

    fun updateTimerNotification(state: TimerState) {
        post(NOTIFICATION_TIMER_ID, buildTimerNotification(state))
    }

    fun cancelTimerNotification() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_TIMER_ID)
    }

    private fun createChannel(
        id: String,
        name: Int,
        description: Int,
        importance: Int,
        configure: NotificationChannel.() -> Unit,
    ) {
        val channel = NotificationChannel(
            id,
            context.getString(name),
            importance,
        ).apply {
            this.description = context.getString(description)
            configure()
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun action(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, TimerActionReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        const val CHANNEL_COMPLETION_ID = "pomodoro_completion_v2"
        const val CHANNEL_COMPLETION_LEGACY_ID = "pomodoro_completion_v1"
        const val CHANNEL_TIMER_ID = "pomodoro_timer_v1"
        const val NOTIFICATION_COMPLETION_ID = 1
        const val NOTIFICATION_TIMER_ID = 2
    }
}
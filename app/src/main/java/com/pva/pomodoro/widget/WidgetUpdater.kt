package com.pva.pomodoro.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.pva.pomodoro.R
import com.pva.pomodoro.model.Phase
import com.pva.pomodoro.model.labelRes
import com.pva.pomodoro.model.TimerState
import com.pva.pomodoro.timer.TimerActionReceiver
import com.pva.pomodoro.timer.TimerActions
import com.pva.pomodoro.timer.TimerStateStore
import com.pva.pomodoro.shared.formatClock
import java.util.Locale

/** Construit et rafraîchit les RemoteViews des widgets bureau et verrouillage. */
object WidgetUpdater {

    fun updateAll(context: Context, state: TimerState) {
        val manager = AppWidgetManager.getInstance(context)
        update(
            context,
            manager,
            state,
            PomodoroWidgetProvider::class.java,
            R.layout.widget_pomodoro,
            showDots = true,
        )
        update(
            context,
            manager,
            state,
            LockScreenWidgetProvider::class.java,
            R.layout.widget_lock_pomodoro,
            showDots = false,
        )
    }

    fun refresh(context: Context) {
        updateAll(context, TimerStateStore(context).load())
    }

    internal fun buildViews(
        context: Context,
        state: TimerState,
        layout: Int,
        showDots: Boolean,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, layout)
        val phaseColor = ContextCompat.getColor(context, phaseColorRes(state.phase))

        views.setTextViewText(R.id.widget_prompt, context.getString(R.string.widget_prompt))
        views.setTextColor(R.id.widget_prompt, ContextCompat.getColor(context, R.color.widget_sage))
        views.setTextViewText(
            R.id.widget_phase,
            context.getString(state.phase.labelRes).uppercase(Locale.getDefault()),
        )
        views.setTextColor(R.id.widget_phase, phaseColor)

        if (state.isRunning) {
            views.setViewVisibility(R.id.widget_chronometer, View.VISIBLE)
            views.setViewVisibility(R.id.widget_time_static, View.GONE)
            val base = SystemClock.elapsedRealtime() + state.remainingMillis.coerceAtLeast(0L)
            views.setChronometerCountDown(R.id.widget_chronometer, true)
            views.setChronometer(R.id.widget_chronometer, base, null, true)
            views.setTextColor(R.id.widget_chronometer, phaseColor)
        } else {
            views.setViewVisibility(R.id.widget_chronometer, View.GONE)
            views.setViewVisibility(R.id.widget_time_static, View.VISIBLE)
            views.setTextViewText(R.id.widget_time_static, formatClock(state.remainingMillis))
            views.setTextColor(R.id.widget_time_static, phaseColor)
        }

        if (showDots) {
            val total = state.settings.sessionsBeforeLongBreak.coerceAtLeast(1)
            val filled = if (state.phase == Phase.LONG_BREAK) {
                total
            } else {
                state.completedWorkSessions % total
            }
            val dots = buildString {
                repeat(total) { index ->
                    append(if (index < filled) "\u25CF" else "\u25CB")
                    if (index < total - 1) append(' ')
                }
            }
            views.setTextViewText(R.id.widget_dots, dots)
            views.setTextColor(R.id.widget_dots, ContextCompat.getColor(context, R.color.widget_sage))
        }

        val toggleLabel = if (state.isRunning) R.string.action_pause else R.string.action_start
        views.setTextViewText(
            R.id.widget_toggle,
            context.getString(toggleLabel).uppercase(Locale.getDefault()),
        )
        views.setTextColor(
            R.id.widget_toggle,
            ContextCompat.getColor(context, R.color.widget_accent_text),
        )
        views.setContentDescription(R.id.widget_toggle, context.getString(toggleLabel))

        views.setOnClickPendingIntent(R.id.widget_toggle, action(context, TimerActions.TOGGLE, 21))
        views.setOnClickPendingIntent(R.id.widget_reset, action(context, TimerActions.RESET, 22))
        views.setOnClickPendingIntent(R.id.widget_skip, action(context, TimerActions.SKIP, 23))

        return views
    }

    private fun update(
        context: Context,
        manager: AppWidgetManager,
        state: TimerState,
        provider: Class<*>,
        layout: Int,
        showDots: Boolean,
    ) {
        val ids = manager.getAppWidgetIds(ComponentName(context, provider))
        if (ids.isEmpty()) return
        manager.updateAppWidget(ids, buildViews(context, state, layout, showDots))
    }

    private fun action(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, TimerActionReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun phaseColorRes(phase: Phase): Int = when (phase) {
        Phase.WORK -> R.color.widget_phase_work
        Phase.SHORT_BREAK -> R.color.widget_phase_short
        Phase.LONG_BREAK -> R.color.widget_phase_long
    }
}
package com.pva.pomodoro.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import com.pva.pomodoro.timer.TimerStateStore

/** Base commune aux widgets bureau et écran verrouillé. */
abstract class BaseWidgetProvider(
    private val layout: Int,
    private val showDots: Boolean,
) : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val state = TimerStateStore(context).load()
        val views = WidgetUpdater.buildViews(context, state, layout, showDots)
        appWidgetIds.forEach { appWidgetManager.updateAppWidget(it, views) }
    }
}
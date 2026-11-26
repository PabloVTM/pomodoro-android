package com.pva.pomodoro.widget

import com.pva.pomodoro.R

/** Widget de l'écran verrouillé (appareils compatibles). */
class LockScreenWidgetProvider : BaseWidgetProvider(
    layout = R.layout.widget_lock_pomodoro,
    showDots = false,
)
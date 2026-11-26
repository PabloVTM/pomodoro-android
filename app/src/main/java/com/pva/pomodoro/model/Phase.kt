package com.pva.pomodoro.model

import androidx.annotation.StringRes
import com.pva.pomodoro.R

/** Les trois phases d'un cycle Pomodoro. */
enum class Phase(@StringRes val labelRes: Int) {
    WORK(R.string.phase_work),
    SHORT_BREAK(R.string.phase_short_break),
    LONG_BREAK(R.string.phase_long_break),
}
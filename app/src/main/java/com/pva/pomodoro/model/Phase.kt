package com.pva.pomodoro.model

import androidx.annotation.StringRes
import com.pva.pomodoro.R

/**
 * Les trois phases d'un cycle Pomodoro. Le type lui-même est partagé avec la
 * montre (`:shared`) ; seuls les libellés restent locaux au téléphone.
 */
typealias Phase = com.pva.pomodoro.shared.Phase

@get:StringRes
val Phase.labelRes: Int
    get() = when (this) {
        Phase.WORK -> R.string.phase_work
        Phase.SHORT_BREAK -> R.string.phase_short_break
        Phase.LONG_BREAK -> R.string.phase_long_break
    }

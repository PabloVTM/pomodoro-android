package com.pva.pomodoro.wear.sync

import android.content.Context
import androidx.core.content.edit

/**
 * Conserve le dernier décalage d'horloge mesuré, afin de pouvoir réarmer
 * l'alarme de fin de phase depuis le service de fond, quand l'application
 * montre n'est pas ouverte.
 */
class ClockOffsetStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): Long = prefs.getLong(KEY_OFFSET, 0L)

    fun save(offsetMillis: Long) {
        prefs.edit { putLong(KEY_OFFSET, offsetMillis) }
    }

    private companion object {
        const val PREFS_NAME = "wear_clock"
        const val KEY_OFFSET = "clock_offset_millis"
    }
}

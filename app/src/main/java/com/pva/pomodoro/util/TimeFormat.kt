package com.pva.pomodoro.util

import java.util.Locale
import kotlin.math.ceil

/** Formate une durée en millisecondes sous la forme « m:ss ». */
fun formatClock(remainingMillis: Long): String {
    val totalSeconds = ceil(remainingMillis / 1000.0).toLong().coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}
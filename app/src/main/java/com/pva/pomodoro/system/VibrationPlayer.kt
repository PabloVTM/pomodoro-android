package com.pva.pomodoro.system

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Fait vibrer le téléphone à la fin d'une phase.
 *
 * La vibration est émise par l'application (et non par la notification) afin de
 * rester disponible même si l'utilisateur a refusé la permission de notification.
 */
class VibrationPlayer(private val context: Context) {

    fun playCompletion() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        } ?: return

        runCatching {
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0L, 450L, 200L, 450L),
                    -1,
                ),
            )
        }
    }
}

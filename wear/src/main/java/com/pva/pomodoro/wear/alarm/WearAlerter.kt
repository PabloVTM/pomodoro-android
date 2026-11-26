package com.pva.pomodoro.wear.alarm

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Vibration et bip de fin de phase, émis par la montre. */
object WearAlerter {

    fun alert(context: Context) {
        vibrate(context)
        beep()
    }

    private fun vibrate(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            context.getSystemService(Vibrator::class.java)
        } ?: return

        runCatching {
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0L, 600L, 250L, 600L),
                    -1,
                ),
            )
        }
    }

    private fun beep() {
        val generator = runCatching {
            ToneGenerator(AudioManager.STREAM_ALARM, VOLUME_PERCENT)
        }.getOrNull() ?: return

        try {
            generator.startTone(ToneGenerator.TONE_PROP_BEEP2, TONE_DURATION_MS)
            Thread.sleep(TONE_DURATION_MS + PAUSE_MS.toLong())
            generator.startTone(ToneGenerator.TONE_PROP_BEEP2, TONE_DURATION_MS)
            Thread.sleep(TONE_DURATION_MS.toLong())
        } finally {
            generator.release()
        }
    }

    private const val VOLUME_PERCENT = 90
    private const val TONE_DURATION_MS = 450
    private const val PAUSE_MS = 250
}
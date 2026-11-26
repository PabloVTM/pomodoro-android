package com.pva.pomodoro.system

import android.media.AudioManager
import android.media.ToneGenerator

/**
 * Joue un petit bip à la fin d'une phase.
 *
 * Le son est produit par l'application (et non par la notification) afin de
 * fonctionner même si l'utilisateur a refusé la permission de notification.
 */
class TonePlayer {

    private var toneGenerator: ToneGenerator? = null

    fun playCompletion() {
        val generator = toneGenerator ?: createGenerator() ?: return
        toneGenerator = generator
        runCatching { generator.startTone(ToneGenerator.TONE_PROP_BEEP2, TONE_DURATION_MS) }
    }

    fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }

    private fun createGenerator(): ToneGenerator? = runCatching {
        ToneGenerator(AudioManager.STREAM_NOTIFICATION, VOLUME_PERCENT)
    }.getOrNull()

    private companion object {
        const val VOLUME_PERCENT = 90
        const val TONE_DURATION_MS = 400
    }
}
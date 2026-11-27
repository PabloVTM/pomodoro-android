package com.pva.pomodoro.system

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper

/**
 * Joue un petit bip à la fin d'une phase.
 *
 * Le son est produit par l'application (et non par la notification) afin de
 * fonctionner même si l'utilisateur a refusé la permission de notification.
 * Le [ToneGenerator] est recréé à la demande puis libéré une fois le bip
 * terminé, afin de ne pas rester alloué pour la vie du singleton.
 */
class TonePlayer {

    private val handler = Handler(Looper.getMainLooper())
    private var toneGenerator: ToneGenerator? = null
    private val releaseRunnable = Runnable { release() }

    fun playCompletion() {
        release()
        val generator = createGenerator() ?: return
        toneGenerator = generator
        runCatching { generator.startTone(ToneGenerator.TONE_PROP_BEEP2, TONE_DURATION_MS) }
        handler.postDelayed(releaseRunnable, (TONE_DURATION_MS + RELEASE_DELAY_MS).toLong())
    }

    fun release() {
        handler.removeCallbacks(releaseRunnable)
        toneGenerator?.release()
        toneGenerator = null
    }

    private fun createGenerator(): ToneGenerator? = runCatching {
        ToneGenerator(AudioManager.STREAM_NOTIFICATION, VOLUME_PERCENT)
    }.getOrNull()

    private companion object {
        const val VOLUME_PERCENT = 90
        const val TONE_DURATION_MS = 400
        const val RELEASE_DELAY_MS = 100
    }
}

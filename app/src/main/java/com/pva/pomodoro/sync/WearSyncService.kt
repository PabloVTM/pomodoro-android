package com.pva.pomodoro.sync

import android.os.Handler
import android.os.Looper
import com.pva.pomodoro.model.CommandOrigin
import com.pva.pomodoro.timer.PomodoroTimerService
import com.pva.pomodoro.timer.TimerEngine
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import java.nio.ByteBuffer

/** Reçoit les demandes et commandes envoyées par la montre. */
class WearSyncService : WearableListenerService() {

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onMessageReceived(event: MessageEvent) {
        when (event.path) {
            WearProtocol.REQUEST_PATH -> mainHandler.post {
                val engine = TimerEngine.get(this)
                WearStatePublisher.publish(this, engine.state.value, force = true)
                replyWithClock(event)
            }

            WearProtocol.ACTION_PATH -> {
                val action = event.data?.toString(Charsets.UTF_8) ?: return
                mainHandler.post { handleAction(action) }
            }
        }
    }

    /** Renvoie l'heure du téléphone pour mesurer le décalage des horloges. */
    private fun replyWithClock(event: MessageEvent) {
        val watchSentAt = event.data
            ?.takeIf { it.size >= Long.SIZE_BYTES }
            ?.let { ByteBuffer.wrap(it).long }
            ?: return

        val payload = ByteBuffer.allocate(2 * Long.SIZE_BYTES)
            .putLong(watchSentAt)
            .putLong(System.currentTimeMillis())
            .array()

        runCatching {
            Wearable.getMessageClient(this)
                .sendMessage(event.sourceNodeId, WearProtocol.PONG_PATH, payload)
        }
    }

    private fun handleAction(action: String) {
        val engine = TimerEngine.get(this)
        when (action) {
            WearProtocol.ACTION_TOGGLE -> engine.toggle(CommandOrigin.WEAR)
            WearProtocol.ACTION_START -> engine.start(CommandOrigin.WEAR)
            WearProtocol.ACTION_PAUSE -> engine.pause(CommandOrigin.WEAR)
            WearProtocol.ACTION_RESET -> engine.reset(CommandOrigin.WEAR)
            WearProtocol.ACTION_SKIP -> engine.skip(CommandOrigin.WEAR)
            else -> return
        }
        PomodoroTimerService.sync(this, engine.state.value)
        WearStatePublisher.publish(this, engine.state.value, force = true)
    }
}
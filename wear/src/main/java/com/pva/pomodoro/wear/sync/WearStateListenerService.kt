package com.pva.pomodoro.wear.sync

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService
import com.pva.pomodoro.wear.alarm.WearAlarmScheduler

/**
 * Reçoit les changements d'état du téléphone même lorsque l'application montre
 * n'est pas ouverte, et réarme l'alarme de fin de phase. C'est ce qui garantit
 * le bip / la vibration à la fin d'une phase après une mort du processus ou un
 * passage en arrière-plan.
 */
class WearStateListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        try {
            val offset = ClockOffsetStore(this).load()
            val now = System.currentTimeMillis()
            val scheduler = WearAlarmScheduler(this)
            for (event in dataEvents) {
                if (event.type != DataEvent.TYPE_CHANGED) continue
                if (event.dataItem.uri.path != WearProtocol.STATE_PATH) continue
                val bytes = event.dataItem.data ?: continue
                val state = WearStateParser.parse(bytes, offset, now) ?: continue
                scheduler.sync(state)
            }
        } finally {
            dataEvents.release()
        }
    }
}

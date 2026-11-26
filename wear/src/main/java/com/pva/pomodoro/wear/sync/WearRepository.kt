package com.pva.pomodoro.wear.sync

import android.content.Context
import com.pva.pomodoro.wear.alarm.WearAlarmScheduler
import com.pva.pomodoro.wear.model.Phase
import com.pva.pomodoro.wear.model.WearTimerState
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import java.nio.ByteBuffer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Reçoit l'état du téléphone via Wearable Data Layer et lui envoie les
 * commandes de l'utilisateur.
 *
 * Le décalage entre les horloges est mesuré par un aller-retour de messages :
 * la montre envoie son heure, le téléphone répond avec la sienne. On en déduit
 * « heure téléphone - heure montre », ce qui évite d'ajouter la latence de
 * livraison au décompte.
 */
class WearRepository(context: Context) {

    private val appContext = context.applicationContext
    private val dataClient = Wearable.getDataClient(appContext)
    private val messageClient = Wearable.getMessageClient(appContext)
    private val nodeClient = Wearable.getNodeClient(appContext)
    private val alarmScheduler = WearAlarmScheduler(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var clockOffsetMillis = 0L

    private val _state = MutableStateFlow<WearTimerState?>(null)
    val state: StateFlow<WearTimerState?> = _state.asStateFlow()

    private val _offline = MutableStateFlow(false)
    val offline: StateFlow<Boolean> = _offline.asStateFlow()

    private val dataListener = DataClient.OnDataChangedListener { events ->
        try {
            for (event in events) {
                if (event.type == DataEvent.TYPE_CHANGED &&
                    event.dataItem.uri.path == WearProtocol.STATE_PATH
                ) {
                    event.dataItem.data?.let { parse(it) }
                }
            }
        } finally {
            events.release()
        }
    }

    private val messageListener = MessageClient.OnMessageReceivedListener { event ->
        if (event.path == WearProtocol.PONG_PATH) handlePong(event.data)
    }

    fun start() {
        runCatching { dataClient.addListener(dataListener) }
        runCatching { messageClient.addListener(messageListener) }
        scope.launch {
            loadInitialState()
            while (isActive) {
                requestSync()
                delay(SYNC_INTERVAL_MS)
            }
        }
    }

    fun close() {
        runCatching { dataClient.removeListener(dataListener) }
        runCatching { messageClient.removeListener(messageListener) }
        scope.cancel()
    }

    fun sendAction(action: String) {
        scope.launch {
            val sent = send(WearProtocol.ACTION_PATH, action.toByteArray(Charsets.UTF_8))
            _offline.value = !sent
            requestSync()
        }
    }

    private suspend fun requestSync() {
        val payload = ByteBuffer.allocate(Long.SIZE_BYTES)
            .putLong(System.currentTimeMillis())
            .array()
        val sent = send(WearProtocol.REQUEST_PATH, payload)
        if (!sent) _offline.value = true
    }

    private suspend fun loadInitialState() {
        val items = runCatching { dataClient.getDataItems().await() }.getOrNull() ?: return
        items.firstOrNull { it.uri.path == WearProtocol.STATE_PATH }
            ?.let { item -> item.data?.let { parse(it) } }
    }

    private suspend fun send(path: String, payload: ByteArray): Boolean {
        val nodes = runCatching { nodeClient.connectedNodes.await() }.getOrNull().orEmpty()
        var sent = false
        for (node in nodes) {
            val result = runCatching {
                messageClient.sendMessage(node.id, path, payload).await()
            }
            if (result.isSuccess) sent = true
        }
        return sent
    }

    private fun handlePong(bytes: ByteArray) {
        if (bytes.size < 2 * Long.SIZE_BYTES) return
        val buffer = ByteBuffer.wrap(bytes)
        val watchSentAt = buffer.long
        val phoneNow = buffer.long
        val now = System.currentTimeMillis()
        val roundTrip = (now - watchSentAt).coerceAtLeast(0L)
        val offset = phoneNow - (now - roundTrip / 2)

        if (offset != clockOffsetMillis) {
            clockOffsetMillis = offset
            _state.value = _state.value?.copy(clockOffsetMillis = offset)
            alarmScheduler.sync(_state.value)
        }
        _offline.value = false
    }

    private fun parse(bytes: ByteArray) {
        val map = runCatching { DataMap.fromByteArray(bytes) }.getOrNull() ?: return
        val phase = runCatching {
            Phase.valueOf(map.getString(WearProtocol.KEY_PHASE) ?: Phase.WORK.name)
        }.getOrDefault(Phase.WORK)
        val running = map.getBoolean(WearProtocol.KEY_RUNNING)
        val endAtPhone = if (running) map.getLong(WearProtocol.KEY_END_AT) else 0L
        val remaining = if (running && endAtPhone > 0L) {
            (endAtPhone - clockOffsetMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        } else {
            map.getLong(WearProtocol.KEY_REMAINING_MS)
        }

        val state = WearTimerState(
            phase = phase,
            totalMillis = map.getLong(WearProtocol.KEY_TOTAL_MS),
            remainingMillis = remaining,
            isRunning = running,
            endAtPhoneMillis = endAtPhone,
            clockOffsetMillis = clockOffsetMillis,
            completedWorkSessions = map.getInt(WearProtocol.KEY_COMPLETED),
            sessionsBeforeLongBreak = map.getInt(WearProtocol.KEY_SESSIONS).coerceAtLeast(1),
            soundEnabled = map.getBoolean(WearProtocol.KEY_SOUND, true),
            vibrationEnabled = map.getBoolean(WearProtocol.KEY_VIBRATION, true),
        )
        _state.value = state
        alarmScheduler.sync(state)
        _offline.value = false
    }

    private companion object {
        /** Recalage régulier des horloges (et de l'état) pendant l'ouverture. */
        const val SYNC_INTERVAL_MS = 30_000L
    }
}
package com.pva.pomodoro.timer

import android.content.Context
import android.util.Log
import com.pva.pomodoro.R
import com.pva.pomodoro.data.SettingsRepository
import com.pva.pomodoro.model.CommandOrigin
import com.pva.pomodoro.model.Phase
import com.pva.pomodoro.model.PomodoroSettings
import com.pva.pomodoro.model.TimerState
import com.pva.pomodoro.model.TimerTransitions
import com.pva.pomodoro.system.PomodoroNotifier
import com.pva.pomodoro.system.TonePlayer
import com.pva.pomodoro.system.VibrationPlayer
import com.pva.pomodoro.sync.WearStatePublisher
import com.pva.pomodoro.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Source de vérité du minuteur. Toutes les méthodes doivent être appelées sur
 * le thread principal (interface, receivers, service).
 */
class TimerEngine private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val settingsRepository = SettingsRepository(context)
    private val store = TimerStateStore(context)
    private val notifier = PomodoroNotifier(context)
    private val tonePlayer = TonePlayer()
    private val vibrationPlayer = VibrationPlayer(context)

    private val restored = store.load()
    private val overdueAtStart = restored.isRunning &&
        (restored.endAtMillis ?: 0L) <= System.currentTimeMillis()

    private val _state = MutableStateFlow(normalize(restored))
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private var tickerJob: Job? = null

    init {
        if (overdueAtStart) {
            completePhase(counted = true, skipped = false, autoStart = false)
        } else if (_state.value.isRunning) {
            _state.value.endAtMillis?.let { AlarmScheduler.schedule(context, it) }
            launchTicker()
        }
        WidgetUpdater.updateAll(context, _state.value)
        WearStatePublisher.publish(context, _state.value)
    }

    fun toggle(origin: CommandOrigin = CommandOrigin.PHONE) {
        if (_state.value.isRunning) pauseInternal(origin) else startInternal(origin)
    }

    fun start(origin: CommandOrigin = CommandOrigin.PHONE) = startInternal(origin)

    private fun startInternal(origin: CommandOrigin) {
        val current = _state.value
        if (current.isRunning) return
        val remaining = if (current.remainingMillis <= 0L) current.totalMillis else current.remainingMillis
        applyState(
            current.copy(
                isRunning = true,
                remainingMillis = remaining,
                endAtMillis = System.currentTimeMillis() + remaining,
                commandOrigin = origin,
            ),
        )
        launchTicker()
    }

    fun pause(origin: CommandOrigin = CommandOrigin.PHONE) = pauseInternal(origin)

    private fun pauseInternal(origin: CommandOrigin) {
        val current = _state.value
        if (!current.isRunning) return
        val remaining = ((current.endAtMillis ?: System.currentTimeMillis()) - System.currentTimeMillis())
            .coerceAtLeast(0L)
        tickerJob?.cancel()
        tickerJob = null
        if (remaining <= 0L) {
            completePhase(counted = true, skipped = false, origin = origin)
            return
        }
        applyState(
            current.copy(
                isRunning = false,
                remainingMillis = remaining,
                endAtMillis = null,
                commandOrigin = origin,
            ),
        )
    }

    fun reset(origin: CommandOrigin = CommandOrigin.PHONE) {
        tickerJob?.cancel()
        tickerJob = null
        val current = _state.value
        val total = current.settings.durationMillis(current.phase)
        applyState(
            current.copy(
                isRunning = false,
                totalMillis = total,
                remainingMillis = total,
                endAtMillis = null,
                commandOrigin = origin,
            ),
        )
    }

    /** Passe à la phase suivante sans compter ni alerter. */
    fun skip(origin: CommandOrigin = CommandOrigin.PHONE) {
        completePhase(counted = false, skipped = true, origin = origin)
    }

    fun applySettings(settings: PomodoroSettings) {
        settingsRepository.save(settings)
        val current = _state.value
        val total = settings.durationMillis(current.phase)
        val withSettings = current.copy(settings = settings)
        val untouched = !current.isRunning && current.remainingMillis == current.totalMillis
        val updated = when {
            untouched -> withSettings.copy(totalMillis = total, remainingMillis = total)
            total == current.totalMillis -> withSettings
            else -> {
                // Changement en cours de session : le temps restant est recalculé
                // proportionnellement pour garder l'anneau de progression cohérent.
                TimerTransitions.rescaleRemaining(withSettings, total, System.currentTimeMillis())
            }
        }
        applyState(updated)
    }

    fun resetSessionCount() {
        applyState(_state.value.copy(completedWorkSessions = 0))
    }

    /** Appelé par l'alarme de fin de phase. */
    fun onAlarmFired() {
        val current = _state.value
        if (!current.isRunning) return
        val endAt = current.endAtMillis ?: return
        if (endAt > System.currentTimeMillis()) return
        completePhase(counted = true, skipped = false)
    }

    private fun completePhase(
        counted: Boolean,
        skipped: Boolean,
        autoStart: Boolean = _state.value.settings.autoStartNext,
        origin: CommandOrigin = _state.value.commandOrigin,
    ) {
        tickerJob?.cancel()
        tickerJob = null

        val current = _state.value
        val settings = current.settings
        val next = TimerTransitions.afterPhase(
            current = current,
            counted = counted,
            autoStart = autoStart,
            now = System.currentTimeMillis(),
        )
        applyState(next.copy(commandOrigin = origin))

        if (!skipped) {
            // Une session lancée depuis la montre est annoncée par la montre.
            val alertOnPhone = origin == CommandOrigin.PHONE
            Log.i(
                "PomodoroTimer",
                "Fin de phase, alerte sur ${if (alertOnPhone) "le téléphone" else "la montre"}",
            )
            if (settings.soundEnabled && alertOnPhone) tonePlayer.playCompletion()
            if (settings.vibrationEnabled && alertOnPhone) vibrationPlayer.playCompletion()
            if (settings.notificationsEnabled && alertOnPhone) {
                notifyPhaseComplete(current.phase, next.phase, settings)
            }
        }

        if (autoStart) launchTicker()
    }

    private fun launchTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                val current = _state.value
                if (!current.isRunning) break
                val remaining = (current.endAtMillis ?: 0L) - System.currentTimeMillis()
                if (remaining <= 0L) {
                    completePhase(counted = true, skipped = false)
                    break
                }
                _state.value = current.copy(remainingMillis = remaining)
                delay(TICK_INTERVAL_MS)
            }
        }
    }

    private fun applyState(newState: TimerState) {
        _state.value = newState
        store.save(newState)
        WidgetUpdater.updateAll(context, newState)
        WearStatePublisher.publish(context, newState)
        if (newState.isRunning && newState.endAtMillis != null) {
            AlarmScheduler.schedule(context, newState.endAtMillis)
        } else {
            AlarmScheduler.cancel(context)
        }
    }

    private fun normalize(state: TimerState): TimerState {
        if (!state.isRunning) return state
        val remaining = ((state.endAtMillis ?: 0L) - System.currentTimeMillis()).coerceAtLeast(0L)
        return state.copy(remainingMillis = remaining)
    }

    private fun notifyPhaseComplete(finished: Phase, next: Phase, settings: PomodoroSettings) {
        val title: String
        val message: String
        if (finished == Phase.WORK) {
            title = context.getString(R.string.notification_work_done_title)
            message = if (next == Phase.LONG_BREAK) {
                context.getString(R.string.notification_long_break_next, settings.longBreakMinutes)
            } else {
                context.getString(R.string.notification_short_break_next, settings.shortBreakMinutes)
            }
        } else {
            title = context.getString(R.string.notification_break_done_title)
            message = context.getString(R.string.notification_break_done_message)
        }
        notifier.notifyPhaseComplete(title, message)
    }

    companion object {
        private const val TICK_INTERVAL_MS = 200L

        @Volatile
        private var instance: TimerEngine? = null

        fun get(context: Context): TimerEngine =
            instance ?: synchronized(this) {
                instance ?: TimerEngine(context.applicationContext).also { instance = it }
            }
    }
}

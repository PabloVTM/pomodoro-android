package com.pva.pomodoro.timer

import android.content.Context
import android.util.Log
import com.pva.pomodoro.R
import com.pva.pomodoro.data.SettingsRepository
import com.pva.pomodoro.model.Phase
import com.pva.pomodoro.model.PomodoroCycle
import com.pva.pomodoro.model.PomodoroSettings
import com.pva.pomodoro.model.TimerState
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

    /**
     * Dernière commande reçue. Quand une session est pilotée depuis la montre,
     * c'est elle qui alerte et le téléphone reste silencieux.
     */
    private var commandOrigin = CommandOrigin.PHONE

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
        commandOrigin = origin
        if (_state.value.isRunning) pauseInternal() else startInternal()
    }

    fun start(origin: CommandOrigin = CommandOrigin.PHONE) {
        commandOrigin = origin
        startInternal()
    }

    private fun startInternal() {
        val current = _state.value
        if (current.isRunning) return
        val remaining = if (current.remainingMillis <= 0L) current.totalMillis else current.remainingMillis
        applyState(
            current.copy(
                isRunning = true,
                remainingMillis = remaining,
                endAtMillis = System.currentTimeMillis() + remaining,
            ),
        )
        launchTicker()
    }

    fun pause(origin: CommandOrigin = CommandOrigin.PHONE) {
        commandOrigin = origin
        pauseInternal()
    }

    private fun pauseInternal() {
        val current = _state.value
        if (!current.isRunning) return
        val remaining = ((current.endAtMillis ?: System.currentTimeMillis()) - System.currentTimeMillis())
            .coerceAtLeast(0L)
        tickerJob?.cancel()
        tickerJob = null
        if (remaining <= 0L) {
            completePhase(counted = true, skipped = false)
            return
        }
        applyState(current.copy(isRunning = false, remainingMillis = remaining, endAtMillis = null))
    }

    fun reset(origin: CommandOrigin = CommandOrigin.PHONE) {
        commandOrigin = origin
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
            ),
        )
    }

    /** Passe à la phase suivante sans compter ni alerter. */
    fun skip(origin: CommandOrigin = CommandOrigin.PHONE) {
        commandOrigin = origin
        completePhase(counted = false, skipped = true)
    }

    fun applySettings(settings: PomodoroSettings) {
        settingsRepository.save(settings)
        val current = _state.value
        val untouched = !current.isRunning && current.remainingMillis == current.totalMillis
        val total = settings.durationMillis(current.phase)
        val updated = if (untouched) {
            current.copy(settings = settings, totalMillis = total, remainingMillis = total)
        } else {
            current.copy(settings = settings)
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
    ) {
        tickerJob?.cancel()
        tickerJob = null

        val current = _state.value
        val settings = current.settings
        var completed = current.completedWorkSessions
        if (current.phase == Phase.WORK && counted) completed += 1

        val nextPhase = PomodoroCycle.nextPhase(
            current = current.phase,
            completedWorkSessions = completed,
            sessionsBeforeLongBreak = settings.sessionsBeforeLongBreak,
        )
        val nextTotal = settings.durationMillis(nextPhase)
        applyState(
            current.copy(
                phase = nextPhase,
                completedWorkSessions = completed,
                totalMillis = nextTotal,
                remainingMillis = nextTotal,
                isRunning = autoStart,
                endAtMillis = if (autoStart) System.currentTimeMillis() + nextTotal else null,
            ),
        )

        if (!skipped) {
            // Une session lancée depuis la montre est annoncée par la montre.
            val alertOnPhone = commandOrigin == CommandOrigin.PHONE
            Log.i(
                "PomodoroTimer",
                "Fin de phase, alerte sur ${if (alertOnPhone) "le téléphone" else "la montre"}",
            )
            if (settings.soundEnabled && alertOnPhone) tonePlayer.playCompletion()
            if (settings.vibrationEnabled && alertOnPhone) vibrationPlayer.playCompletion()
            if (settings.notificationsEnabled && alertOnPhone) {
                notifyPhaseComplete(current.phase, nextPhase, settings)
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
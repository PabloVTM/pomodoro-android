package com.pva.pomodoro

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.pva.pomodoro.model.PomodoroSettings
import com.pva.pomodoro.model.TimerState
import com.pva.pomodoro.timer.TimerEngine
import kotlinx.coroutines.flow.StateFlow

/** Expose l'état du moteur partagé à l'interface Compose. */
class PomodoroViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = TimerEngine.get(application)

    val uiState: StateFlow<TimerState> = engine.state

    fun toggleTimer() = engine.toggle()

    fun reset() = engine.reset()

    fun skip() = engine.skip()

    fun applySettings(settings: PomodoroSettings) = engine.applySettings(settings)

    fun resetSessionCount() = engine.resetSessionCount()
}
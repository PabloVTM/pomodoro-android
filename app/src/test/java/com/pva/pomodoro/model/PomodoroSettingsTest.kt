package com.pva.pomodoro.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PomodoroSettingsTest {

    @Test
    fun `default settings match the classic pomodoro technique`() {
        val settings = PomodoroSettings()

        assertEquals(25, settings.workMinutes)
        assertEquals(5, settings.shortBreakMinutes)
        assertEquals(15, settings.longBreakMinutes)
        assertEquals(4, settings.sessionsBeforeLongBreak)
    }

    @Test
    fun `durations are converted to milliseconds`() {
        val settings = PomodoroSettings(
            workMinutes = 25,
            shortBreakMinutes = 5,
            longBreakMinutes = 15,
        )

        assertEquals(1_500_000L, settings.durationMillis(Phase.WORK))
        assertEquals(300_000L, settings.durationMillis(Phase.SHORT_BREAK))
        assertEquals(900_000L, settings.durationMillis(Phase.LONG_BREAK))
    }
}
package com.pva.pomodoro.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PomodoroCycleTest {

    @Test
    fun `work is followed by a short break`() {
        assertEquals(
            Phase.SHORT_BREAK,
            PomodoroCycle.nextPhase(Phase.WORK, completedWorkSessions = 1, sessionsBeforeLongBreak = 4),
        )
    }

    @Test
    fun `long break after a full cycle`() {
        assertEquals(
            Phase.LONG_BREAK,
            PomodoroCycle.nextPhase(Phase.WORK, completedWorkSessions = 4, sessionsBeforeLongBreak = 4),
        )
    }

    @Test
    fun `long break repeats at every multiple of the cycle size`() {
        assertEquals(
            Phase.LONG_BREAK,
            PomodoroCycle.nextPhase(Phase.WORK, completedWorkSessions = 8, sessionsBeforeLongBreak = 4),
        )
    }

    @Test
    fun `work without any completed session leads to a short break`() {
        assertEquals(
            Phase.SHORT_BREAK,
            PomodoroCycle.nextPhase(Phase.WORK, completedWorkSessions = 0, sessionsBeforeLongBreak = 4),
        )
    }

    @Test
    fun `short break is followed by work`() {
        assertEquals(
            Phase.WORK,
            PomodoroCycle.nextPhase(Phase.SHORT_BREAK, completedWorkSessions = 2, sessionsBeforeLongBreak = 4),
        )
    }

    @Test
    fun `long break is followed by work`() {
        assertEquals(
            Phase.WORK,
            PomodoroCycle.nextPhase(Phase.LONG_BREAK, completedWorkSessions = 4, sessionsBeforeLongBreak = 4),
        )
    }
}
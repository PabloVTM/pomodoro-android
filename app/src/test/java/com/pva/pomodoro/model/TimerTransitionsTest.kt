package com.pva.pomodoro.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerTransitionsTest {

    private val settings = PomodoroSettings(
        workMinutes = 25,
        shortBreakMinutes = 5,
        longBreakMinutes = 15,
        sessionsBeforeLongBreak = 4,
    )

    @Test
    fun `completed work session counts and leads to a short break`() {
        val current = TimerState(phase = Phase.WORK, completedWorkSessions = 1, settings = settings)

        val next = TimerTransitions.afterPhase(current, counted = true, autoStart = false, now = 1_000L)

        assertEquals(Phase.SHORT_BREAK, next.phase)
        assertEquals(2, next.completedWorkSessions)
        assertEquals(settings.durationMillis(Phase.SHORT_BREAK), next.totalMillis)
        assertEquals(settings.durationMillis(Phase.SHORT_BREAK), next.remainingMillis)
        assertFalse(next.isRunning)
        assertNull(next.endAtMillis)
    }

    @Test
    fun `work session reaching the cycle size leads to a long break`() {
        val current = TimerState(phase = Phase.WORK, completedWorkSessions = 3, settings = settings)

        val next = TimerTransitions.afterPhase(current, counted = true, autoStart = false, now = 0L)

        assertEquals(Phase.LONG_BREAK, next.phase)
        assertEquals(4, next.completedWorkSessions)
    }

    @Test
    fun `a skipped work session is not counted`() {
        val current = TimerState(phase = Phase.WORK, completedWorkSessions = 2, settings = settings)

        val next = TimerTransitions.afterPhase(current, counted = false, autoStart = false, now = 0L)

        assertEquals(Phase.SHORT_BREAK, next.phase)
        assertEquals(2, next.completedWorkSessions)
    }

    @Test
    fun `a break always returns to work without counting`() {
        val current = TimerState(phase = Phase.SHORT_BREAK, completedWorkSessions = 2, settings = settings)

        val next = TimerTransitions.afterPhase(current, counted = true, autoStart = false, now = 0L)

        assertEquals(Phase.WORK, next.phase)
        assertEquals(2, next.completedWorkSessions)
    }

    @Test
    fun `auto start schedules the end of the next phase`() {
        val current = TimerState(phase = Phase.WORK, settings = settings)
        val now = 10_000L

        val next = TimerTransitions.afterPhase(current, counted = true, autoStart = true, now = now)

        assertTrue(next.isRunning)
        assertEquals(now + settings.durationMillis(Phase.SHORT_BREAK), next.endAtMillis)
    }

    @Test
    fun `rescaling keeps the elapsed fraction of a running session`() {
        val current = TimerState(
            phase = Phase.WORK,
            totalMillis = 1_000L,
            remainingMillis = 250L,
            isRunning = true,
            endAtMillis = 5_000L,
            settings = settings,
        )

        val rescaled = TimerTransitions.rescaleRemaining(current, newTotal = 2_000L, now = 1_000L)

        // Un quart restant de 2000 ms = 500 ms.
        assertEquals(2_000L, rescaled.totalMillis)
        assertEquals(500L, rescaled.remainingMillis)
        assertEquals(1_500L, rescaled.endAtMillis)
    }

    @Test
    fun `rescaling an untouched session resets it to the new duration`() {
        val current = TimerState(
            phase = Phase.WORK,
            totalMillis = 1_000L,
            remainingMillis = 1_000L,
            settings = settings,
        )

        val rescaled = TimerTransitions.rescaleRemaining(current, newTotal = 2_000L, now = 0L)

        assertEquals(2_000L, rescaled.totalMillis)
        assertEquals(2_000L, rescaled.remainingMillis)
        assertNull(rescaled.endAtMillis)
    }
}

package com.pva.pomodoro.wear.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class ClockSyncTest {

    @Test
    fun `no offset when both clocks agree and the round trip is instant`() {
        val offset = ClockSync.offset(now = 1_000L, watchSentAt = 1_000L, phoneNow = 1_000L)

        assertEquals(0L, offset)
    }

    @Test
    fun `half of the round trip is removed from the offset`() {
        // La montre a envoyé à 1000, le téléphone répond à 1100, la montre reçoit
        // à 1200 : l'aller-retour vaut 200 ms, soit 100 ms de latence par sens.
        val offset = ClockSync.offset(now = 1_200L, watchSentAt = 1_000L, phoneNow = 1_100L)

        // heure téléphone - heure montre = 1100 - (1200 - 100) = 0.
        assertEquals(0L, offset)
    }

    @Test
    fun `phone clock ahead of the watch yields a positive offset`() {
        val offset = ClockSync.offset(now = 1_000L, watchSentAt = 1_000L, phoneNow = 5_000L)

        assertEquals(4_000L, offset)
    }

    @Test
    fun `a clock skew backwards is measured even with a slow round trip`() {
        val offset = ClockSync.offset(now = 1_500L, watchSentAt = 1_000L, phoneNow = 900L)

        // aller-retour = 500 ms, latence par sens = 250 ms,
        // offset = 900 - (1500 - 250) = -350.
        assertEquals(-350L, offset)
    }
}

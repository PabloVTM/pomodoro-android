package com.pva.pomodoro.wear.sync

/**
 * Recalage des horloges téléphone / montre.
 *
 * La montre envoie son heure (`watchSentAt`), le téléphone répond avec la
 * sienne (`phoneNow`). On estime « heure téléphone - heure montre » en
 * retranchant la moitié de l'aller-retour, ce qui évite d'ajouter la latence de
 * livraison au décompte. Fonction pure, testable sans Android.
 */
object ClockSync {

    fun offset(now: Long, watchSentAt: Long, phoneNow: Long): Long {
        val roundTrip = (now - watchSentAt).coerceAtLeast(0L)
        return phoneNow - (now - roundTrip / 2)
    }
}

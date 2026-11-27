package com.pva.pomodoro.model

/** Appareil à l'origine de la dernière commande. */
enum class CommandOrigin {
    /** Interface, widget ou notification du téléphone. */
    PHONE,

    /** Application de la montre. */
    WEAR,
}

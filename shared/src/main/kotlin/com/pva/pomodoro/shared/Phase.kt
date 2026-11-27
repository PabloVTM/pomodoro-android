package com.pva.pomodoro.shared

/**
 * Les trois phases d'un cycle Pomodoro, partagées entre le téléphone et la
 * montre. Les libellés sont fournis par chaque application (extension
 * `labelRes`), afin de garder ce module indépendant des ressources Android.
 */
enum class Phase {
    WORK,
    SHORT_BREAK,
    LONG_BREAK,
}

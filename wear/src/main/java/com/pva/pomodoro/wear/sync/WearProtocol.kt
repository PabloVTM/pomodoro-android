package com.pva.pomodoro.wear.sync

/** Doit rester identique au protocole du module téléphone. */
object WearProtocol {
    const val STATE_PATH = "/pomodoro/state"
    const val ACTION_PATH = "/pomodoro/action"
    const val REQUEST_PATH = "/pomodoro/request"
    const val PONG_PATH = "/pomodoro/pong"

    const val ACTION_TOGGLE = "toggle"
    const val ACTION_START = "start"
    const val ACTION_PAUSE = "pause"
    const val ACTION_RESET = "reset"
    const val ACTION_SKIP = "skip"

    const val KEY_PHASE = "phase"
    const val KEY_TOTAL_MS = "totalMs"
    const val KEY_REMAINING_MS = "remainingMs"
    const val KEY_RUNNING = "running"
    const val KEY_END_AT = "endAt"
    const val KEY_COMPLETED = "completed"
    const val KEY_WORK_MIN = "workMin"
    const val KEY_SHORT_BREAK_MIN = "shortBreakMin"
    const val KEY_LONG_BREAK_MIN = "longBreakMin"
    const val KEY_SESSIONS = "sessions"
    const val KEY_SOUND = "sound"
    const val KEY_VIBRATION = "vibration"
    const val KEY_UPDATED_AT = "updatedAt"
}
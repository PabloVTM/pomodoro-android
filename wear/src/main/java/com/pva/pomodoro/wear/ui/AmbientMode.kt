package com.pva.pomodoro.wear.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LifecycleOwner
import androidx.wear.ambient.AmbientLifecycleObserver
import kotlinx.coroutines.delay

/**
 * Intervalle de rafraîchissement de l'écran de veille. Le système ne rafraîchit
 * sinon qu'environ une fois par minute via `onUpdateAmbient`.
 */
private const val AMBIENT_REFRESH_INTERVAL_MS = 15_000L

/**
 * État du mode ambiant (écran en veille) et compteur de rafraîchissement.
 */
@Stable
class AmbientModeState internal constructor(
    val isAmbient: State<Boolean>,
    val tick: State<Int>,
)

@Composable
fun rememberAmbientMode(): AmbientModeState {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val isAmbient = remember { mutableStateOf(false) }
    val tick = remember { mutableIntStateOf(0) }

    DisposableEffect(activity) {
        val lifecycleOwner = activity as? LifecycleOwner
        if (lifecycleOwner == null) return@DisposableEffect onDispose { }

        val callback = object : AmbientLifecycleObserver.AmbientLifecycleCallback {
            override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) {
                isAmbient.value = true
            }

            override fun onUpdateAmbient() {
                tick.intValue += 1
            }

            override fun onExitAmbient() {
                isAmbient.value = false
            }
        }

        val observer = AmbientLifecycleObserver(activity, callback)
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val inAmbient = isAmbient.value
    LaunchedEffect(inAmbient) {
        if (!inAmbient) return@LaunchedEffect
        while (true) {
            delay(AMBIENT_REFRESH_INTERVAL_MS)
            tick.intValue += 1
        }
    }

    return remember(isAmbient, tick) { AmbientModeState(isAmbient, tick) }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
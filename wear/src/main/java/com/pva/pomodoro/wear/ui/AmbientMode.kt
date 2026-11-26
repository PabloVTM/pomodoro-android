package com.pva.pomodoro.wear.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LifecycleOwner
import androidx.wear.ambient.AmbientLifecycleObserver

/**
 * État du mode ambiant (écran en veille) et compteur de rafraîchissement :
 * le système n'appelle `onUpdateAmbient` qu'environ une fois par minute.
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

    return remember(isAmbient, tick) { AmbientModeState(isAmbient, tick) }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
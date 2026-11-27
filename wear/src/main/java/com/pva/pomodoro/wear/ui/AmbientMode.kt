package com.pva.pomodoro.wear.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LifecycleOwner
import androidx.wear.ambient.AmbientLifecycleObserver

/**
 * État du mode ambiant (écran en veille). L'affichage ne dépend que de l'état
 * reçu du téléphone : rien n'est recalculé à chaque rafraîchissement système,
 * donc aucun `tick` n'est nécessaire.
 */
@Stable
class AmbientModeState internal constructor(
    val isAmbient: State<Boolean>,
)

@Composable
fun rememberAmbientMode(): AmbientModeState {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val isAmbient = remember { mutableStateOf(false) }

    DisposableEffect(activity) {
        val lifecycleOwner = activity as? LifecycleOwner
        if (lifecycleOwner == null) return@DisposableEffect onDispose { }

        val callback = object : AmbientLifecycleObserver.AmbientLifecycleCallback {
            override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) {
                isAmbient.value = true
            }

            override fun onUpdateAmbient() {
                // Rien de dynamique à rafraîchir : le contenu ambiant ne change
                // qu'aux transitions d'état reçues du téléphone.
            }

            override fun onExitAmbient() {
                isAmbient.value = false
            }
        }

        val observer = AmbientLifecycleObserver(activity, callback)
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return remember(isAmbient) { AmbientModeState(isAmbient) }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

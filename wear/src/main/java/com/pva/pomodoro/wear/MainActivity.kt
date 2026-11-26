package com.pva.pomodoro.wear

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.pva.pomodoro.wear.ui.WearApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        setContent {
            WearApp()
        }
    }

    /**
     * La notification d'activité en cours maintient le minuteur visible en
     * veille ; elle nécessite l'autorisation de notification sur Android 13+.
     * Une seule demande est faite.
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) return

        val prefs = getSharedPreferences("wear_ui", MODE_PRIVATE)
        if (prefs.getBoolean(KEY_NOTIFICATION_PROMPTED, false)) return
        prefs.edit().putBoolean(KEY_NOTIFICATION_PROMPTED, true).apply()

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQUEST_NOTIFICATIONS,
        )
    }

    private companion object {
        const val KEY_NOTIFICATION_PROMPTED = "notification_prompted"
        const val REQUEST_NOTIFICATIONS = 1
    }
}
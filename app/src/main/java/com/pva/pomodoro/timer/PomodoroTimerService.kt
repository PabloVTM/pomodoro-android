package com.pva.pomodoro.timer

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.pva.pomodoro.model.TimerState
import com.pva.pomodoro.system.PomodoroNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Maintient le processus en vie pendant une session et affiche le décompte
 * dans une notification persistante.
 */
class PomodoroTimerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var engine: TimerEngine
    private lateinit var notifier: PomodoroNotifier
    private var observer: Job? = null

    override fun onCreate() {
        super.onCreate()
        engine = TimerEngine.get(this)
        notifier = PomodoroNotifier(this)

        // Service démarré alors que le minuteur est à l'arrêt : inutile de
        // passer en premier plan (coûteux et source d'un flash de notification).
        if (!engine.state.value.isRunning) {
            stopSelf()
            return
        }

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notifier.buildTimerNotification(engine.state.value),
            if (Build.VERSION.SDK_INT >= 34) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            },
        )

        observer = scope.launch {
            engine.state.collect { state ->
                if (state.isRunning) {
                    notifier.updateTimerNotification(state)
                } else {
                    stopSelf()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        observer?.cancel()
        scope.cancel()
        notifier.cancelTimerNotification()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 2

        fun startSafely(context: Context) {
            runCatching {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, PomodoroTimerService::class.java),
                )
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, PomodoroTimerService::class.java))
        }

        /** Démarre ou arrête le service selon l'état et le réglage des notifications. */
        fun sync(context: Context, state: TimerState) {
            if (state.isRunning && state.settings.notificationsEnabled) {
                startSafely(context)
            } else {
                stop(context)
            }
        }
    }
}
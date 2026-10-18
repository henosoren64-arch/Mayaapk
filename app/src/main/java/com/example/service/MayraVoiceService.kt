package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.MayraApplication
import com.example.R
import com.example.model.AssistantStatus
import com.example.session.MayraSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MayraVoiceService : Service() {

    companion object {
        const val NOTIFICATION_ID = 4099
        const val ACTION_TOGGLE_MUTE = "com.example.mayra.ACTION_TOGGLE_MUTE"
        const val ACTION_INTERRUPT = "com.example.mayra.ACTION_INTERRUPT"
        const val ACTION_STOP = "com.example.mayra.ACTION_STOP"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var stateCollectJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        MayraSessionManager.init(applicationContext)
        startAsForegroundService()
        observeSessionState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE_MUTE -> {
                MayraSessionManager.toggleMicMute()
            }
            ACTION_INTERRUPT -> {
                MayraSessionManager.handleInterruption("Notification action")
            }
            ACTION_STOP -> {
                MayraSessionManager.stopSession(this)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startAsForegroundService() {
        val notification = buildNotification(
            title = getString(R.string.service_running_title),
            content = getString(R.string.service_running_text),
            isMuted = MayraSessionManager.uiState.value.isMicMuted
        )

        val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        } else {
            0
        }

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            foregroundServiceType
        )
    }

    private fun observeSessionState() {
        stateCollectJob?.cancel()
        stateCollectJob = serviceScope.launch {
            MayraSessionManager.uiState.collect { state ->
                val statusText = when (state.assistantStatus) {
                    AssistantStatus.Listening -> "Listening to you, darling..."
                    AssistantStatus.Thinking -> "Thinking up a witty response..."
                    AssistantStatus.Speaking -> "Mayra is speaking..."
                    AssistantStatus.Interrupted -> "Interrupted! Ready for you."
                    AssistantStatus.Idle -> "Standing by. Tap to speak."
                }

                val notification = buildNotification(
                    title = if (state.isMicMuted) "Mayra (Muted)" else "Mayra is Active",
                    content = statusText,
                    isMuted = state.isMicMuted
                )

                val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun buildNotification(title: String, content: String, isMuted: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val muteIntent = Intent(this, MayraVoiceService::class.java).apply {
            action = ACTION_TOGGLE_MUTE
        }
        val mutePendingIntent = PendingIntent.getService(
            this, 1, muteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val interruptIntent = Intent(this, MayraVoiceService::class.java).apply {
            action = ACTION_INTERRUPT
        }
        val interruptPendingIntent = PendingIntent.getService(
            this, 2, interruptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, MayraVoiceService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 3, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val muteActionTitle = if (isMuted) "Unmute" else "Mute"

        return NotificationCompat.Builder(this, MayraApplication.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.mayra_launcher_icon)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_media_pause, muteActionTitle, mutePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Interrupt", interruptPendingIntent)
            .addAction(android.R.drawable.ic_delete, "Stop", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stateCollectJob?.cancel()
        serviceScope.cancel()
    }
}

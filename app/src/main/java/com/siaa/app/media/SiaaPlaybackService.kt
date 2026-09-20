package com.siaa.app.media

import android.app.Notification
import android.app.PendingIntent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import com.siaa.app.MainActivity
import android.os.Build
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import android.net.Uri
import com.siaa.app.R
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.siaa.app.SiaaApplication
import com.siaa.core.audio.AudioOutputGuard
import com.siaa.core.audio.AudioFocusController
import com.siaa.core.model.SessionMode
import com.siaa.core.runtime.RuntimeCommand
import com.siaa.core.runtime.SessionConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class SiaaPlaybackService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private var session: MediaSession? = null
    private lateinit var audioOutputGuard: AudioOutputGuard
    private lateinit var audioFocus: AudioFocusController
    private val router = EarbudCommandRouter()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val appGraph get() = (application as SiaaApplication).graph
    private val runtime get() = appGraph.runtime

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, notification("SIAA listo"))

        player = ExoPlayer.Builder(this).build()
        session = MediaSession.Builder(this, player)
            .setCallback(object : MediaSession.Callback {
                override fun onMediaButtonEvent(
                    session: MediaSession,
                    controllerInfo: MediaSession.ControllerInfo,
                    intent: Intent
                ): Boolean {
                    val mapped = router.fromMediaButtonIntent(intent) ?: return false
                    appGraph.mediaDiagnostics.record("keyCode=${mapped.second} -> ${mapped.first}", mapped.first.name)
                    return runtime.onCommand(mapped.first)
                }
            })
            .build()

        audioOutputGuard = AudioOutputGuard(this) { runtime.pauseForRouteChange() }
        audioOutputGuard.register()
        audioFocus = AudioFocusController(this, onTransientLoss = { runtime.pauseForRouteChange() })

        serviceScope.launch {
            runtime.snapshot.collectLatest { snap ->
                val manager = getSystemService(NotificationManager::class.java)
                manager.notify(NOTIFICATION_ID, notification("${snap.mode}: ${snap.message} · ${snap.completedItems}"))
                if (snap.state == com.siaa.core.runtime.LessonState.SESSION_END || snap.state == com.siaa.core.runtime.LessonState.ERROR) {
                    // Dar tiempo al mensaje final de TTS y luego liberar el ancla/sesión foreground.
                    delay(2_500)
                    if (runtime.snapshot.value.state == snap.state) {
                        deactivateMediaAnchor()
                        audioFocus.abandon()
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SESSION -> {
                val mode = intent.getStringExtra(EXTRA_MODE)?.let { runCatching { SessionMode.valueOf(it) }.getOrNull() }
                    ?: SessionMode.ADAPTIVE
                val maxItems = intent.getIntExtra(EXTRA_MAX_ITEMS, 60).coerceIn(1, 500)
                val announceControls = intent.getBooleanExtra(EXTRA_ANNOUNCE_CONTROLS, true)
                val feedbackExplanations = intent.getBooleanExtra(EXTRA_FEEDBACK_EXPLANATIONS, true)
                if (audioFocus.request()) {
                    activateMediaAnchor()
                    serviceScope.launch {
                        appGraph.contentReady.first { it }
                        runtime.start(SessionConfig(
                            mode = mode,
                            maxItems = maxItems,
                            announceControls = announceControls,
                            feedbackExplanations = feedbackExplanations
                        ))
                    }
                } else appGraph.mediaDiagnostics.record("AudioFocus no concedido")
            }
            ACTION_STOP_SESSION -> {
                runtime.onCommand(RuntimeCommand.STOP)
                deactivateMediaAnchor()
                audioFocus.abandon()
            }
            ACTION_START_CALIBRATION -> {
                activateMediaAnchor()
                appGraph.mediaDiagnostics.record("Calibración activa: esperando comandos multimedia")
                getSystemService(NotificationManager::class.java).notify(
                    NOTIFICATION_ID, notification("Calibración de audífonos activa")
                )
            }
            ACTION_STOP_CALIBRATION -> {
                deactivateMediaAnchor()
                appGraph.mediaDiagnostics.record("Calibración detenida")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Mantener la sesión: el caso de uso exige pantalla apagada / UI cerrada.
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        if (runtime.snapshot.value.state !in setOf(
                com.siaa.core.runtime.LessonState.IDLE,
                com.siaa.core.runtime.LessonState.SESSION_END,
                com.siaa.core.runtime.LessonState.ERROR
            )) {
            runtime.stop()
        }
        audioOutputGuard.unregister()
        audioFocus.abandon()
        session?.release()
        session = null
        player.release()
        serviceScope.cancel()
        super.onDestroy()
    }


    /**
     * Media3 enruta mejor controles externos cuando la sesión tiene un Player en estado activo.
     * El audio pedagógico real sigue siendo TTS; este WAV local, en loop y volumen cero, actúa
     * únicamente como ancla de playback para la MediaSession.
     */
    private fun activateMediaAnchor() {
        if (player.isPlaying) return
        val uri = Uri.parse("android.resource://$packageName/${R.raw.siaa_silence}")
        player.setMediaItem(MediaItem.fromUri(uri))
        player.repeatMode = Player.REPEAT_MODE_ONE
        player.volume = 0f
        player.prepare()
        player.play()
    }

    private fun deactivateMediaAnchor() {
        player.pause()
        player.clearMediaItems()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Sesiones SIAA", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun notification(text: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 10, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 11, Intent(this, SiaaPlaybackService::class.java).apply { action = ACTION_STOP_SESSION },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("SIAA · entrenamiento auditivo")
            .setContentText(text)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_media_pause, "Detener", stopIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val ACTION_START_SESSION = "com.siaa.app.action.START_SESSION"
        const val ACTION_STOP_SESSION = "com.siaa.app.action.STOP_SESSION"
        const val ACTION_START_CALIBRATION = "com.siaa.app.action.START_CALIBRATION"
        const val ACTION_STOP_CALIBRATION = "com.siaa.app.action.STOP_CALIBRATION"
        const val EXTRA_MODE = "mode"
        const val EXTRA_MAX_ITEMS = "max_items"
        const val EXTRA_ANNOUNCE_CONTROLS = "announce_controls"
        const val EXTRA_FEEDBACK_EXPLANATIONS = "feedback_explanations"
        private const val CHANNEL_ID = "siaa_session"
        private const val NOTIFICATION_ID = 1001
    }
}

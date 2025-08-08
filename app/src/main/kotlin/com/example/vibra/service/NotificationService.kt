package com.example.vibra.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import android.support.v4.media.session.MediaSessionCompat
import com.example.vibra.R
import com.example.vibra.model.Music
import com.example.vibra.model.MusicPlayerManager

class NotificationService : Service() {

    companion object {
        private const val CHANNEL_ID = "vibra_channel"
        private const val NOTIFICATION_ID = 42

        fun start(context: Context, music: Music) {
            val intent = Intent(context, NotificationService::class.java).apply {
                putExtra("music_name", music.name)
            }
            context.startForegroundService(intent)
        }

        fun update(context: Context, music: Music) {
            val intent = Intent(context, NotificationService::class.java).apply {
                putExtra("music_name", music.name)
                putExtra("update_only", true)
            }
            context.startService(intent)
        }
    }

    private lateinit var mediaSession: MediaSessionCompat

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        mediaSession = MediaSessionCompat(this, "VibraSession").apply {
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val musicName = intent?.getStringExtra("music_name") ?: "Lecture"
        val updateOnly = intent?.getBooleanExtra("update_only", false) ?: false

        val notification = buildMusicNotification(musicName)

        if (!updateOnly) {
            startForeground(NOTIFICATION_ID, notification)
        } else {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, notification)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        stopForeground(true)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildMusicNotification(musicName: String): Notification {
        val isPlaying = MusicPlayerManager.isPlaying()

        // Intents pour les boutons
        val previousPending = PendingIntent.getBroadcast(
            this, 0,
            Intent(this, NotificationReceiver::class.java).apply { action = "ACTION_PREVIOUS" },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val playPausePending = PendingIntent.getBroadcast(
            this, 1,
            Intent(this, NotificationReceiver::class.java).apply { action = "ACTION_PLAY_PAUSE" },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val nextPending = PendingIntent.getBroadcast(
            this, 2,
            Intent(this, NotificationReceiver::class.java).apply { action = "ACTION_NEXT" },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.music_note)
            .setContentTitle(musicName)
            .setContentText(if (isPlaying) "Lecture en cours" else "En pause")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // ✅ visible sur écran verrouillé
            .addAction(R.drawable.ic_previous, "Précédent", previousPending)
            .addAction(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                if (isPlaying) "Pause" else "Lecture",
                playPausePending
            )
            .addAction(R.drawable.ic_next, "Suivant", nextPending)
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2) // 3 boutons visibles même en compact
            )
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Vibra Music Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "Notifications pour la lecture musicale"
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}

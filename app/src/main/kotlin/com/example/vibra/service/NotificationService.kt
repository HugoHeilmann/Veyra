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
import android.support.v4.media.session.PlaybackStateCompat
import com.example.vibra.R
import com.example.vibra.model.Music
import com.example.vibra.model.MusicHolder
import com.example.vibra.model.MusicPlayerManager

class NotificationService : Service() {

    companion object {
        private const val CHANNEL_ID = "vibra_channel"
        private const val NOTIFICATION_ID = 42

        fun start(context: Context, music: Music) {
            val intent = Intent(context, NotificationService::class.java).apply {
                putExtra("music_name", music.name)
                putExtra("artist_name", music.artist)
            }
            context.startForegroundService(intent)
        }

        fun update(context: Context) {
            val intent = Intent(context, NotificationService::class.java).apply {
                action = "UPDATE_NOTIFICATION"
            }
            context.startService(intent)
        }
    }

    private lateinit var mediaSession: MediaSessionCompat

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        mediaSession = MediaSessionCompat(this, "VibraSession").apply {
            // permettre la gestion des boutons média
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
            isActive = true

            // Callback : quand le système envoie un événement média (ex: via UI système),
            // on exécute directement la logique du player ici et on met à jour la notification.
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    val current = MusicHolder.getCurrentMusic()
                    current?.let { MusicPlayerManager.playMusic(this@NotificationService, it) }
                    // demander la mise à jour de la notif après changement d'état
                    update(this@NotificationService)
                }

                override fun onPause() {
                    MusicPlayerManager.pauseMusic(this@NotificationService)
                    update(this@NotificationService)
                }

                override fun onSkipToNext() {
                    MusicHolder.getNext()?.let { MusicHolder.setPlayedMusic(this@NotificationService, it) }
                    update(this@NotificationService)
                }

                override fun onSkipToPrevious() {
                    MusicHolder.getPrevious()?.let { MusicHolder.setPlayedMusic(this@NotificationService, it) }
                    update(this@NotificationService)
                }
            })
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val musicName = intent?.getStringExtra("music_name")
            ?: MusicPlayerManager.getCurrentMusic()?.name
            ?: "No Name Music"

        val artistName = intent?.getStringExtra("artist_name")
            ?: MusicPlayerManager.getCurrentMusic()?.artist
            ?: "Unknown Artist"

        // Build playback state : on expose les actions (pour avoir le style des nouveaux boutons)
        // mais on met PLAYBACK_POSITION_UNKNOWN pour éviter l'apparition du slider.
        val isPlaying = MusicPlayerManager.isPlaying()
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )
            .setState(
                if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                1.0f
            )
            .build()
        mediaSession.setPlaybackState(playbackState)

        if (intent?.action == "UPDATE_NOTIFICATION") {
            val notification = buildMusicNotification(musicName, artistName)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, notification)
        } else {
            startForeground(NOTIFICATION_ID, buildMusicNotification(musicName, artistName))
        }

        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)

        // stop music
        MusicPlayerManager.stopMusic()

        // stop service and notification
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        mediaSession.release()

        // stop music
        MusicPlayerManager.stopMusic()

        // stop service and notification
        stopForeground(true)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildMusicNotification(musicName: String, artistName: String): Notification {
        val isPlaying = MusicPlayerManager.isPlaying()

        // Intents pour les boutons — on garde les PendingIntent (ils pointent vers le BroadcastReceiver)
        // mais les events média (qui viennent du system) sont aussi pris en charge par MediaSession.Callback
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
            .setContentText(artistName)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // visible écran verrouillé
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
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Vibra Music Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications pour la lecture musicale"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}

package com.example.vibra.service

import android.Manifest
import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.example.vibra.MainActivity
import com.example.vibra.R
import com.example.vibra.model.MediaSessionManager

class NotificationService : Service() {

    private var mediaSession: MediaSessionCompat? = null

    companion object {
        private const val CHANNEL_ID = "custom_channel"
        private const val NOTIF_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Init media session
        mediaSession = MediaSessionManager.getSession()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Ensure MediaSession active
        if (mediaSession == null) {
            MediaSessionManager.init(this)
            mediaSession = MediaSessionManager.getSession()
        }

        val title = intent?.getStringExtra("NOTIF_TITLE") ?: "Vibra"
        val text = intent?.getStringExtra("NOTIF_TEXT") ?: "Unknown artist - Unknown album"

        startForeground(NOTIF_ID, buildNotification(title, text))

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Lecteur musique",
                NotificationManager.IMPORTANCE_HIGH
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, text: String): Notification {
        // Vérifier permission Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                // Permission non accordée → renvoyer notif vide
                return NotificationCompat.Builder(this, CHANNEL_ID).build()
            }
        }

        // Créer PendingIntent pour chaque action
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpenApp = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        )
        val pendingRewind = PendingIntent.getBroadcast(
            this, 0,
            Intent(this, NotificationActionReceiver::class.java).apply { action = "ACTION_REWIND_10" },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val pendingPrev = PendingIntent.getBroadcast(
            this, 1,
            Intent(this, NotificationActionReceiver::class.java).apply { action = "ACTION_SKIP_PREV" },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val pendingPlay = PendingIntent.getBroadcast(
            this, 2,
            Intent(this, NotificationActionReceiver::class.java).apply { action = "ACTION_PLAY_PAUSE" },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val pendingNext = PendingIntent.getBroadcast(
            this, 3,
            Intent(this, NotificationActionReceiver::class.java).apply { action = "ACTION_SKIP_NEXT" },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val pendingForward = PendingIntent.getBroadcast(
            this, 4,
            Intent(this, NotificationActionReceiver::class.java).apply { action = "ACTION_FORWARD_10" },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Construire notification MediaStyle
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.music_note)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingOpenApp)
            .setOngoing(true)
            .setAutoCancel(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.drawable.ic_rewind_10, "Rewind 10", pendingRewind)
            .addAction(R.drawable.ic_previous, "Prev", pendingPrev)
            .addAction(R.drawable.ic_play_pause, "Play/Pause", pendingPlay)
            .addAction(R.drawable.ic_next, "Next", pendingNext)
            .addAction(R.drawable.ic_forward_10, "Forward 10", pendingForward)
            .setStyle(
                MediaStyle()
                    .setShowActionsInCompactView()
                    .setMediaSession(mediaSession?.sessionToken)
            )
            .build()
            .apply { flags = flags or Notification.FLAG_NO_CLEAR or Notification.FLAG_ONGOING_EVENT }
    }
}

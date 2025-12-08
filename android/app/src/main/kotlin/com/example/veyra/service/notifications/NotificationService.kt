package com.example.veyra.service.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.core.graphics.scale
import com.example.veyra.MainActivity
import com.example.veyra.R
import com.example.veyra.model.data.MusicPlayerManager
import com.example.veyra.service.NotificationActionReceiver
import java.io.File

class NotificationService : Service() {

    private lateinit var mediaSession: MediaSessionCompat

    companion object {
        private const val CHANNEL_ID = "custom_channel"
        private const val NOTIF_ID = 1

        fun startOrUpdate(context: Context) {
            val appCtx = context.applicationContext
            val current = MusicPlayerManager.getCurrentMusic()

            // S'il n'y a plus de musique → on arrête le service et on enlève la notif
            if (current == null) {
                appCtx.stopService(Intent(appCtx, NotificationService::class.java))
                return
            }

            val intent = Intent(appCtx, NotificationService::class.java).apply {
                putExtra("NOTIF_TITLE", current.name)
                putExtra(
                    "NOTIF_TEXT",
                    "${current.artist ?: "Unknown artist"} - ${current.album ?: "Unknown album"}"
                )
                putExtra("NOTIF_COVER_PATH", current.coverPath)
                putExtra("NOTIF_IMAGE_RES", current.image)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appCtx.startForegroundService(intent)
            } else {
                appCtx.startService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        mediaSession = MediaSessionCompat(this, "VeyraMediaSession").apply {
            isActive = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mediaSession.isInitialized) {
            mediaSession.release()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Si le système relance le service avec intent null → on arrête
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (!::mediaSession.isInitialized) {
            mediaSession = MediaSessionCompat(this, "VeyraMediaSession").apply {
                isActive = true
            }
        }

        val title = intent.getStringExtra("NOTIF_TITLE") ?: "Veyra"
        val text = intent.getStringExtra("NOTIF_TEXT") ?: "Unknown artist - Unknown album"
        val coverPath = intent.getStringExtra("NOTIF_COVER_PATH")
        val imageRes = intent.getIntExtra("NOTIF_IMAGE_RES", R.drawable.default_album_cover)

        startForeground(NOTIF_ID, buildNotification(title, text, coverPath, imageRes))

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
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(
        title: String,
        text: String,
        coverPath: String?,
        imageRes: Int
    ): Notification {
        // Vérifier permission Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Permission non accordée → ne rien afficher
                return NotificationCompat.Builder(this, CHANNEL_ID).build()
            }
        }

        val largeIcon: Bitmap? = try {
            when {
                coverPath != null -> {
                    val file = File(coverPath)
                    if (file.exists()) {
                        BitmapFactory.decodeFile(file.absolutePath)
                    } else {
                        BitmapFactory.decodeResource(resources, imageRes)
                    }
                }

                else -> BitmapFactory
                    .decodeResource(resources, imageRes)
                    .scale(512, 512, false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            BitmapFactory
                .decodeResource(resources, R.drawable.default_album_cover)
                .scale(512, 512, false)
        }

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

        val playPauseIcon = if (MusicPlayerManager.isPlaying()) {
            R.drawable.ic_pause
        } else {
            R.drawable.ic_play
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.music_note)
            .setContentTitle(title)
            .setContentText(text)
            .setLargeIcon(largeIcon)
            .setContentIntent(pendingOpenApp)
            .setOngoing(true)
            .setAutoCancel(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.drawable.ic_rewind_10, "Rewind 10", pendingRewind)
            .addAction(R.drawable.ic_previous, "Prev", pendingPrev)
            .addAction(playPauseIcon, "Play/Pause", pendingPlay)
            .addAction(R.drawable.ic_next, "Next", pendingNext)
            .addAction(R.drawable.ic_forward_10, "Forward 10", pendingForward)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView()
                    .setMediaSession(mediaSession.sessionToken)
            )
            .build()
            .apply {
                flags = flags or Notification.FLAG_NO_CLEAR or Notification.FLAG_ONGOING_EVENT
            }
    }
}
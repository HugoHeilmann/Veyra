package com.example.vibra.model

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import com.example.vibra.service.NotificationReceiver

import com.example.vibra.R

object MusicHolder {
    private var currentMusic: Music? = null
    private var musicList: List<Music> = emptyList()

    private var originalContextList: List<Music> = emptyList()
    private var shuffledContextList: List<Music> = emptyList()

    private val artistMap = mutableMapOf<String, List<Music>>()
    private val albumMap = mutableMapOf<String, List<Music>>()

    var isShuffled by mutableStateOf(false)
        private set

    fun setMusicList(list: List<Music>) {
        musicList = list.sortedBy { it.name.lowercase() }

        // Mise à jour des maps artistes et albums
        artistMap.clear()
        artistMap.putAll(
            musicList
                .filter { !it.artist.isNullOrBlank() }
                .groupBy {
                    it.artist?.split(Regex("(?i) ft\\."))?.get(0)?.trim() ?: "Unknown"
                }
                .mapValues { entry -> entry.value.sortedBy { it.name.lowercase() } }
        )

        albumMap.clear()
        albumMap.putAll(
            musicList
                .filter { !it.album.isNullOrBlank() }
                .groupBy { it.album ?: "Unfinished" }
                .mapValues { entry -> entry.value.sortedBy { it.name.lowercase() } }
        )
    }

    fun setPlayedMusic(context: Context, music: Music) {
        currentMusic = music

        // Launch Notification
        showNotification(context, music)
    }

    fun setCurrentMusic(context: Context, music: Music, contextList: List<Music>? = null) {
        currentMusic = music
        originalContextList = (contextList ?: musicList).sortedBy { it.name.lowercase() }
        shuffledContextList = originalContextList.shuffled()

        // Launch Notification
        showNotification(context, music)
    }

    fun enableShuffle(enabled: Boolean) {
        isShuffled = enabled
    }

    fun isShuffleEnabled(): Boolean = isShuffled

    fun getMusicList(): List<Music> = musicList
    fun getArtistSongs(artist: String): List<Music> = artistMap[artist] ?: emptyList()
    fun getAlbumSongs(album: String): List<Music> = albumMap[album] ?: emptyList()
    fun getCurrentMusic(): Music? = currentMusic

    private fun getActiveList(): List<Music> {
        return if (isShuffled) shuffledContextList else originalContextList
    }

    fun getNext(): Music? {
        val list = getActiveList()
        val index = list.indexOf(currentMusic)
        return if (list.isNotEmpty() && index != -1) {
            list[(index + 1) % list.size]
        } else null
    }

    fun getPrevious(): Music? {
        val list = getActiveList()
        val index = list.indexOf(currentMusic)
        return if (list.isNotEmpty() && index != -1) {
            list[(index - 1 + list.size) % list.size]
        } else null
    }

    fun getMusicContext(): List<Music> = getActiveList()
}

fun showNotification(context: Context, music: Music) {
    val channelId = "vibra_channel"
    val notificationId = 42

    // Créer le canal (si nécessaire)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = "Vibra Notification Channel"
        val descriptionText = "Notifications pour Vibra"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelId, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    // Créer les PendingIntent pour chaque action
    val previousIntent = Intent(context, NotificationReceiver::class.java).apply {
        action = "ACTION_PREVIOUS"
    }
    val playPauseIntent = Intent(context, NotificationReceiver::class.java).apply {
        action = "ACTION_PLAY_PAUSE"
    }
    val nextIntent = Intent(context, NotificationReceiver::class.java).apply {
        action = "ACTION_NEXT"
    }

    val previousPendingIntent = PendingIntent.getBroadcast(
        context, 0, previousIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val playPausePendingIntent = PendingIntent.getBroadcast(
        context, 1, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val nextPendingIntent = PendingIntent.getBroadcast(
        context, 2, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val playText = if (MusicPlayerManager.isPlaying()) {
        "Pause"
    } else {
        "Play"
    }

    // Créer la notification
    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.music_note)
        .setContentTitle(music.name)
        .setContentText("Lecture en cours")
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .addAction(R.drawable.ic_previous, "Précédent", previousPendingIntent)
        .addAction(if(MusicPlayerManager.isPlaying()) {
            R.drawable.ic_pause
        } else {
            R.drawable.ic_play
        }, "Play/Pause", playPausePendingIntent)
        .addAction(R.drawable.ic_next, "Suivant", nextPendingIntent)

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.notify(notificationId, builder.build())
}
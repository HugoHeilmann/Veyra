package com.example.vibra.model

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.State
import androidx.core.app.ServiceCompat.startForeground
import androidx.core.net.toUri
import com.example.vibra.service.NotificationService

object MusicPlayerManager {
    private var mediaPlayer: MediaPlayer? = null
    private var currentMusic: Music? = null

    private var _isPlaying by mutableStateOf(false)
    val isPlaying: State<Boolean> get() = androidx.compose.runtime.mutableStateOf(_isPlaying)

    fun playMusic(context: Context, music: Music, onPrepared: (Int) -> Unit = {}) {
        if (mediaPlayer != null && currentMusic?.uri == music.uri) {
            if (mediaPlayer?.isPlaying == false) {
                mediaPlayer?.start()
                _isPlaying = true
            }
            return
        }

        stopMusic()
        currentMusic = music

        mediaPlayer = MediaPlayer().apply {
            setDataSource(context, music.uri.toUri())
            prepareAsync()
            setOnPreparedListener {
                start()
                _isPlaying = true
                onPrepared.invoke(duration)
            }
            setOnCompletionListener {
                _isPlaying = false
            }
        }
    }

    fun play() {
        mediaPlayer?.start()
        _isPlaying = true
    }

    fun pauseMusic() {
        mediaPlayer?.pause()
        _isPlaying = false
    }

    fun isPlaying(): Boolean {
        return isPlaying.value == true
    }

    fun stopMusic() {
        mediaPlayer?.release()
        mediaPlayer = null
        currentMusic = null
        _isPlaying = false
    }

    fun seekTo(positionMs: Int) {
        mediaPlayer?.seekTo(positionMs)
    }

    fun isCurrentlyPlaying(music: Music): Boolean {
        return currentMusic?.uri == music.uri && mediaPlayer?.isPlaying == true
    }

    fun getCurrentMusic(): Music? {
        return currentMusic
    }

    fun getCurrentPosition(): Int {
        return mediaPlayer?.currentPosition ?: 0
    }

    fun getDuration(): Int {
        return mediaPlayer?.duration ?: 0
    }
}

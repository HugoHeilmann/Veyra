package com.example.vibra.model

import android.content.Context
import android.media.MediaPlayer
import androidx.core.net.toUri

object MusicPlayerManager {
    private var mediaPlayer: MediaPlayer? = null
    private var currentMusic: Music? = null

    fun playMusic(context: Context, music: Music, onPrepared: (Int) -> Unit = {}) {
        if (mediaPlayer != null && currentMusic?.uri == music.uri) {
            if (mediaPlayer?.isPlaying == false) {
                mediaPlayer?.start()
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
                onPrepared.invoke(duration)
            }
        }
    }

    fun pauseMusic() {
        mediaPlayer?.pause()
    }

    fun stopMusic() {
        mediaPlayer?.release()
        mediaPlayer = null
        currentMusic = null
    }

    fun seekTo(positionMs: Int) {
        mediaPlayer?.seekTo(positionMs)
    }

    fun isPlaying() : Boolean {
        return mediaPlayer?.isPlaying == true
    }

    fun getCurrentPosition(): Int {
        return mediaPlayer?.currentPosition ?: 0
    }

    fun getDuration(): Int {
        return mediaPlayer?.duration ?: 0
    }

    fun getCurrentMusic(): Music? {
        return currentMusic
    }

    fun isCurrentlyPlaying(music: Music): Boolean {
        return currentMusic?.uri == music.uri && mediaPlayer?.isPlaying == true
    }
}
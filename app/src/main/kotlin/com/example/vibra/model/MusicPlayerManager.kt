package com.example.vibra.model

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri

object MusicPlayerManager {
    private var mediaPlayer: MediaPlayer? = null
    private var currentMusic: Music? = null
    private var audioManager: AudioManager? = null

    private var _isPlaying by mutableStateOf(false)

    // Audio focus change listener
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Immediat pause
                pauseMusicInternal()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Lower volume if another sound wants to go on top of the music
                mediaPlayer?.setVolume(0.2f, 0.2f)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Retrieve focus -> initial volume
                mediaPlayer?.setVolume(1f, 1f)
            }
        }
    }

    fun playMusic(context: Context, music: Music, onPrepared: (Int) -> Unit = {}) {
        if (audioManager == null) {
            audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        }

        if (mediaPlayer != null && currentMusic?.uri == music.uri) {
            if (mediaPlayer?.isPlaying == false) {
                requestAudioFocus()
                mediaPlayer?.start()
                _isPlaying = true
                MediaSessionManager.updatePlaybackState(true)
            }
            return
        }

        stopMusic() // libère l'ancien player
        currentMusic = music

        mediaPlayer = MediaPlayer().apply {
            setDataSource(context, music.uri.toUri())
            prepareAsync()
            setOnPreparedListener {
                requestAudioFocus()
                start()
                _isPlaying = true
                MediaSessionManager.updatePlaybackState(true)
                onPrepared.invoke(duration)
            }
            setOnCompletionListener {
                _isPlaying = false
                MediaSessionManager.updatePlaybackState(false)
                abandonAudioFocus()
            }
        }
    }

    fun pauseMusic(context: Context) {
        pauseMusicInternal()
        MediaSessionManager.updatePlaybackState(false)
        abandonAudioFocus()
    }

    private fun pauseMusicInternal() {
        mediaPlayer?.pause()
        _isPlaying = false
    }

    fun stopMusic() {
        abandonAudioFocus()
        mediaPlayer?.release()
        mediaPlayer = null
        currentMusic = null
        _isPlaying = false
        MediaSessionManager.updatePlaybackState(false)
    }

    fun isPlaying(): Boolean = _isPlaying

    fun seekTo(positionMs: Int) {
        mediaPlayer?.seekTo(positionMs)
    }

    fun rewind10Seconds(): Float {
        mediaPlayer?.let {
            val newPos = (it.currentPosition - 10_000).coerceAtLeast(0)
            it.seekTo(newPos)
            return newPos / 1000f
        }

        return 0f
    }

    fun forward10Seconds(): Float {
        mediaPlayer?.let {
            val newPos = (it.currentPosition + 10_000).coerceAtMost(it.duration - 1_000)
            it.seekTo(newPos)
            return newPos / 1000f
        }

        return 0f
    }

    fun isCurrentlyPlaying(music: Music): Boolean {
        return currentMusic?.uri == music.uri && mediaPlayer?.isPlaying == true
    }

    fun getCurrentMusic(): Music? = currentMusic

    fun getCurrentPosition(): Int = mediaPlayer?.currentPosition ?: 0

    fun getDuration(): Int = mediaPlayer?.duration ?: 0

    // Ask for audio focus
    private fun requestAudioFocus(): Boolean {
        val result = audioManager?.requestAudioFocus(
            audioFocusChangeListener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )

        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    // Release audio focus
    private fun abandonAudioFocus() {
        audioManager?.abandonAudioFocus(audioFocusChangeListener)
    }
}

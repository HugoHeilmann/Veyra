package com.example.veyra.model

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
    private var appContext: Context? = null

    private var _isPlaying by mutableStateOf(false)

    private var onCompletionListener: (() ->  Unit)? = null

    private var receiversRegistered = false

    private val audioNoisyReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent?.action) {
                pauseMusicInternal()
                MediaSessionManager.updatePlaybackState(false)
                abandonAudioFocus()
            }
        }
    }

    private val bluetoothReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED,
                    BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                        val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)

                        if (state == BluetoothProfile.STATE_DISCONNECTED) {
                            pauseMusicInternal()
                            MediaSessionManager.updatePlaybackState(false)
                            abandonAudioFocus()
                        }
                    }

                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    pauseMusicInternal()
                    MediaSessionManager.updatePlaybackState(false)
                    abandonAudioFocus()
                }
            }
        }
    }

    // Audio focus change listener
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pauseMusicInternal()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> mediaPlayer?.setVolume(0.2f, 0.2f)
            AudioManager.AUDIOFOCUS_GAIN -> mediaPlayer?.setVolume(1f, 1f)
        }
    }

    fun init(context: Context) {
        if (appContext == null) appContext = context.applicationContext
        if (audioManager == null) {
            audioManager = appContext!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        }
        registerReceivers()
    }

    private fun registerReceivers() {
        if (receiversRegistered) return
        val ctx = appContext ?: return

        ctx.registerReceiver(
            audioNoisyReceiver,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        )

        val btFilter = IntentFilter().apply {
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        ctx.registerReceiver(bluetoothReceiver, btFilter)

        receiversRegistered = true
    }

    private fun unregisterReceivers() {
        if (!receiversRegistered) return
        val ctx = appContext ?: return
        try { ctx.unregisterReceiver(audioNoisyReceiver) } catch (_: Exception) {}
        try { ctx.unregisterReceiver(bluetoothReceiver) } catch (_: Exception) {}
        receiversRegistered = false
    }

    fun playMusic(context: Context, music: Music, onPrepared: (Int) -> Unit = {}) {
        if (appContext == null) init(context)

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
                onCompletionListener?.invoke()
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
        stopMusicInternal()
        abandonAudioFocus()
        MediaSessionManager.updatePlaybackState(false)
    }

    private fun stopMusicInternal() {
        mediaPlayer?.release()
        mediaPlayer = null
        currentMusic = null
        _isPlaying = false
    }

    fun release() {
        stopMusicInternal()
        abandonAudioFocus()
        unregisterReceivers()
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

    fun getCurrentMusic(): Music? = currentMusic

    fun getCurrentPosition(): Int = mediaPlayer?.currentPosition ?: 0

    fun getDuration(): Int = mediaPlayer?.duration ?: 0

    fun setOnCompletionListener(listener: () -> Unit) {
        onCompletionListener = listener
    }

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

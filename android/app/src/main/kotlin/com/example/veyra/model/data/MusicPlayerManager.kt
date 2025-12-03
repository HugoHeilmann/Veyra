package com.example.veyra.model.data

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
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.example.veyra.model.Music
import com.example.veyra.model.controllers.WaveBarsController
import com.example.veyra.service.notifications.NotificationService
import com.example.veyra.service.widget.Widget
import com.example.veyra.service.widget.WidgetPrefsKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object MusicPlayerManager {
    private var mediaPlayer: MediaPlayer? = null
    private var currentMusic: Music? = null
    private var audioManager: AudioManager? = null
    private var appContext: Context? = null

    private var _isPlaying by mutableStateOf(false)
    private var _isQueuePlaying by mutableStateOf(false)

    // Callback externe éventuel (UI) quand un morceau se termine
    private var onCompletionListener: (() -> Unit)? = null

    private var receiversRegistered = false

    // scope pour updateAll() du widget
    private val widgetScope = CoroutineScope(Dispatchers.Default)

    private val audioNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent?.action) {
                pauseMusicInternal()
                MediaSessionManager.updatePlaybackState(false)
                abandonAudioFocus()
            }
        }
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
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
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                pauseMusicInternal()
                try {
                    NotificationService.startOrUpdate(appContext ?: return@OnAudioFocusChangeListener)
                    saveWidgetState(currentMusic, false)
                } catch (_: Exception) {}
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> mediaPlayer?.setVolume(0.2f, 0.2f)
            AudioManager.AUDIOFOCUS_GAIN -> {
                mediaPlayer?.setVolume(1f, 1f)
                try {
                    NotificationService.startOrUpdate(appContext ?: return@OnAudioFocusChangeListener)
                    saveWidgetState(currentMusic, _isPlaying)
                } catch (_: Exception) {}
            }
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

    private fun saveWidgetState(music: Music?, isPlaying: Boolean) {
        val ctx = appContext ?: return

        widgetScope.launch {
            try {
                val manager = GlanceAppWidgetManager(ctx)
                val ids = manager.getGlanceIds(Widget::class.java)

                ids.forEach { glanceId ->
                    updateAppWidgetState(ctx, glanceId) { prefs ->
                        prefs[WidgetPrefsKeys.TITLE] = music?.name ?: "No music"
                        prefs[WidgetPrefsKeys.ARTIST] = music?.artist ?: "Unknown artist"
                        prefs[WidgetPrefsKeys.ALBUM] = music?.album ?: "Unknown album"
                        prefs[WidgetPrefsKeys.IS_PLAYING] = isPlaying
                    }
                }

                // Redessine tous les widgets Veyra
                Widget().updateAll(ctx)
            } catch (_: Exception) { }
        }
    }

    /**
     * API historique : joue une musique.
     */
    fun playMusic(context: Context, music: Music, onPrepared: (Int) -> Unit = {}) {
        if (appContext == null) init(context)

        playInternal(music, context, onPrepared)
        WaveBarsController.start()
    }

    fun playFromQueueIndex(context: Context, index: Int) {
        if (appContext == null) init(context)
        val ctx = appContext ?: context

        val music = QueueManager.playFromIndex(index) ?: return
        playInternal(music, ctx)
        WaveBarsController.start()
    }

    private fun playInternal(
        music: Music,
        context: Context,
        onPrepared: (Int) -> Unit = {}
    ) {
        if (appContext == null) init(context)
        val ctx = appContext ?: context

        // même morceau déjà chargé → reprendre
        if (mediaPlayer != null && currentMusic?.uri == music.uri) {
            if (mediaPlayer?.isPlaying == false) {
                requestAudioFocus()
                mediaPlayer?.start()
                _isPlaying = true
                MediaSessionManager.updatePlaybackState(true)

                try {
                    NotificationService.startOrUpdate(ctx)
                    saveWidgetState(currentMusic, true)
                } catch (_: Exception) {}
            }
            return
        }

        // nouveau morceau
        stopMusicInternal()
        currentMusic = music

        mediaPlayer = MediaPlayer().apply {
            if (music.uri.startsWith("content://")) {
                setDataSource(ctx, music.uri.toUri())
            } else {
                setDataSource(music.uri)
            }
            prepareAsync()
            setOnPreparedListener {
                requestAudioFocus()
                start()
                _isPlaying = true
                MediaSessionManager.updatePlaybackState(true)
                onPrepared.invoke(duration)

                try {
                    NotificationService.startOrUpdate(ctx)
                    saveWidgetState(music, true)
                } catch (_: Exception) {}
            }
            setOnCompletionListener {
                _isPlaying = false
                MediaSessionManager.updatePlaybackState(false)

                val appCtx = appContext ?: ctx

                val next = QueueManager.getNext()
                if (next != null) {
                    // enchaîne sur le suivant
                    playInternal(next, appCtx)
                } else {
                    // plus rien → efface
                    try {
                        NotificationService.startOrUpdate(appCtx)
                        saveWidgetState(null, false)
                    } catch (_: Exception) {}
                }

                onCompletionListener?.invoke()
            }
        }
    }

    fun pauseMusic(context: Context) {
        pauseMusicInternal()
        MediaSessionManager.updatePlaybackState(false)
        abandonAudioFocus()

        try {
            NotificationService.startOrUpdate(appContext ?: context)
            saveWidgetState(currentMusic, false)
        } catch (_: Exception) {}
    }

    private fun pauseMusicInternal() {
        WaveBarsController.stop()
        mediaPlayer?.pause()
        _isPlaying = false

        try {
            NotificationService.startOrUpdate(appContext ?: return)
            saveWidgetState(currentMusic, false)
        } catch (_: Exception) {}
    }

    fun stopMusic() {
        stopMusicInternal()
        abandonAudioFocus()
        MediaSessionManager.updatePlaybackState(false)

        try {
            NotificationService.startOrUpdate(appContext ?: return)
            saveWidgetState(null, false)
        } catch (_: Exception) {}
    }

    private fun stopMusicInternal() {
        WaveBarsController.stop()
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

    private fun requestAudioFocus(): Boolean {
        val result = audioManager?.requestAudioFocus(
            audioFocusChangeListener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonAudioFocus() {
        audioManager?.abandonAudioFocus(audioFocusChangeListener)
    }
}

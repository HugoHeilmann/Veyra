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
import com.example.veyra.model.Music
import com.example.veyra.service.NotificationService

object MusicPlayerManager {
    private var mediaPlayer: MediaPlayer? = null
    private var currentMusic: Music? = null
    private var audioManager: AudioManager? = null
    private var appContext: Context? = null

    private var _isPlaying by mutableStateOf(true)

    // Callback externe éventuel (UI) quand un morceau se termine
    private var onCompletionListener: (() -> Unit)? = null

    private var receiversRegistered = false

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
                    NotificationService.startOrUpdate(appContext!!)
                } catch (_: Exception) {
                }
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> mediaPlayer?.setVolume(0.2f, 0.2f)
            AudioManager.AUDIOFOCUS_GAIN -> {
                mediaPlayer?.setVolume(1f, 1f)
                try {
                    NotificationService.startOrUpdate(appContext!!)
                } catch (_: Exception) {
                }
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
        try {
            ctx.unregisterReceiver(audioNoisyReceiver)
        } catch (_: Exception) {
        }
        try {
            ctx.unregisterReceiver(bluetoothReceiver)
        } catch (_: Exception) {
        }
        receiversRegistered = false
    }

    /**
     * API historique : joue une musique.
     * Maintenant, cela crée une file de lecture de taille 1
     * et passe par la logique de queue.
     */
    fun playMusic(context: Context, music: Music, onPrepared: (Int) -> Unit = {}) {
        // S'assure d'avoir un contexte appli
        if (appContext == null) init(context)

        // La file devient uniquement ce morceau
        QueueManager.setQueue(listOf(music), startIndex = 0)

        // On joue le morceau via la logique commune
        playInternal(music, context, onPrepared)
    }

    /**
     * Joue une seule musique en recréant une file de lecture de taille 1.
     * (Alias plus explicite de playMusic)
     */
    fun playSingle(music: Music, context: Context, onPrepared: (Int) -> Unit = {}) {
        if (appContext == null) init(context)

        QueueManager.setQueue(listOf(music), startIndex = 0)
        playInternal(music, context, onPrepared)
    }

    /**
     * Lance la lecture d'une liste de musiques (playlist, album, etc.)
     */
    fun playFromList(
        musics: List<Music>,
        startIndex: Int,
        context: Context,
        onPrepared: (Int) -> Unit = {}
    ) {
        if (musics.isEmpty()) return
        if (appContext == null) init(context)

        QueueManager.setQueue(musics, startIndex)
        val current = QueueManager.getCurrent() ?: return
        playInternal(current, context, onPrepared)
    }

    /**
     * Passe au morceau suivant dans la file de lecture.
     */
    fun playNextInQueue(context: Context) {
        if (appContext == null) init(context)
        val ctx = appContext ?: context

        val next = QueueManager.getNext()
        if (next != null) {
            playInternal(next, ctx)
        } else {
            // Fin de file : on met en pause / stop
            pauseMusic(ctx)
        }
    }

    /**
     * Passe au morceau précédent dans la file de lecture.
     */
    fun playPreviousInQueue(context: Context) {
        if (appContext == null) init(context)
        val ctx = appContext ?: context

        val prev = QueueManager.getPrevious()
        if (prev != null) {
            playInternal(prev, ctx)
        }
    }

    /**
     * Joue un élément de la file à un index donné.
     * Utilisé par l'écran de file de lecture quand l'utilisateur clique sur un morceau.
     */
    fun playFromQueueIndex(context: Context, index: Int) {
        if (appContext == null) init(context)
        val ctx = appContext ?: context

        val music = QueueManager.playFromIndex(index) ?: return
        playInternal(music, ctx)
    }

    /**
     * Joue le morceau courant de la file (si existant).
     */
    private fun playCurrentFromQueue(context: Context, onPrepared: (Int) -> Unit = {}) {
        val current = QueueManager.getCurrent() ?: return
        playInternal(current, context, onPrepared)
    }

    /**
     * Fonction interne centrale qui gère réellement la lecture d'un Music
     * (création/prepare du MediaPlayer, audio focus, notification, etc.)
     */
    private fun playInternal(
        music: Music,
        context: Context,
        onPrepared: (Int) -> Unit = {}
    ) {
        if (appContext == null) init(context)
        val ctx = appContext ?: context

        // Si on rejoue le même morceau déjà chargé, on reprend simplement
        if (mediaPlayer != null && currentMusic?.uri == music.uri) {
            if (mediaPlayer?.isPlaying == false) {
                requestAudioFocus()
                mediaPlayer?.start()
                _isPlaying = true
                MediaSessionManager.updatePlaybackState(true)
            }
            return
        }

        // Libère l'ancien player
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
                } catch (_: Exception) {
                }
            }
            setOnCompletionListener {
                _isPlaying = false
                MediaSessionManager.updatePlaybackState(false)

                val appCtx = appContext ?: ctx

                // On tente de passer automatiquement au morceau suivant dans la file
                val next = QueueManager.getNext()
                if (next != null) {
                    // Lecture du morceau suivant via la même logique interne
                    playInternal(next, appCtx)
                } else {
                    // Pas de morceau suivant : on met simplement à jour la notif
                    try {
                        NotificationService.startOrUpdate(appCtx)
                    } catch (_: Exception) {
                    }
                }

                // Callback externe éventuel (UI)
                onCompletionListener?.invoke()
            }
        }
    }

    fun pauseMusic(context: Context) {
        pauseMusicInternal()
        MediaSessionManager.updatePlaybackState(false)
        abandonAudioFocus()

        try {
            NotificationService.startOrUpdate(
                appContext ?: context
            )
        } catch (_: Exception) {
        }
    }

    private fun pauseMusicInternal() {
        mediaPlayer?.pause()
        _isPlaying = false

        try {
            NotificationService.startOrUpdate(appContext ?: return)
        } catch (_: Exception) {
        }
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
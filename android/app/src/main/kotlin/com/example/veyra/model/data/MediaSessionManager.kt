package com.example.veyra.model.data

import android.content.Context
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.example.veyra.model.data.MusicHolder
import com.example.veyra.model.data.MusicPlayerManager

object MediaSessionManager {

    private var mediaSession: MediaSessionCompat? = null

    fun init(context: Context) {
        if (mediaSession != null) return // déjà initialisée

        mediaSession = MediaSessionCompat(context, "VeyraSession").apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )

            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    val current = MusicHolder.getCurrentMusic()
                    if (current != null) {
                        MusicHolder.setPlayedMusic(context, current)
                    }
                    updatePlaybackState(true)
                }

                override fun onPause() {
                    MusicPlayerManager.pauseMusic(context)
                    updatePlaybackState(false) // ✅ corrigé
                }

                override fun onSkipToNext() {
                    val nextMusic = MusicHolder.getNext()
                    if (nextMusic != null) {
                        MusicHolder.setPlayedMusic(context, nextMusic)
                    }
                }

                override fun onSkipToPrevious() {
                    val previousMusic = MusicHolder.getPrevious()
                    if (previousMusic != null) {
                        MusicHolder.setPlayedMusic(context, previousMusic)
                    }
                }
            })

            isActive = true
            updatePlaybackState(false)
        }
    }

    fun updatePlaybackState(isPlaying: Boolean) {
        val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED

        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_STOP or
                        PlaybackStateCompat.ACTION_SEEK_TO
            )
            .setState(state, MusicPlayerManager.getCurrentPosition().toLong(), 1.0f)
            .build()

        mediaSession?.setPlaybackState(playbackState)
    }
}
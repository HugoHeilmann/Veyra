package com.example.vibra.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.vibra.model.MediaSessionManager
import com.example.vibra.model.MusicHolder
import com.example.vibra.model.MusicPlayerManager

class NotificationActionReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            "ACTION_REWIND_10" -> {
                MusicPlayerManager.rewind10Seconds()
            }
            "ACTION_SKIP_PREV" -> {
                val previous = MusicHolder.getPrevious()

                if (previous != null) {
                    MusicHolder.setPlayedMusic(context, previous)
                }
            }
            "ACTION_PLAY_PAUSE" -> {
                if (MusicPlayerManager.isPlaying()) {
                    MusicPlayerManager.pauseMusic(context)
                    MediaSessionManager.updatePlaybackState(false)
                } else {
                    val current = MusicPlayerManager.getCurrentMusic()

                    if (current != null) {
                        MusicPlayerManager.playMusic(context, current)
                        MediaSessionManager.updatePlaybackState(true)
                    }
                }
            }
            "ACTION_SKIP_NEXT" -> {
                val next = MusicHolder.getNext()

                if (next != null) {
                    MusicHolder.setPlayedMusic(context, next)
                }
            }
            "ACTION_FORWARD_10" -> {
                MusicPlayerManager.forward10Seconds()
            }
        }
    }
}
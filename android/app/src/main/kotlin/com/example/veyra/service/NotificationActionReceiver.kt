package com.example.veyra.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.veyra.model.data.MediaSessionManager
import com.example.veyra.model.data.MusicHolder
import com.example.veyra.model.data.MusicPlayerManager
import com.example.veyra.service.notifications.NotificationService

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

                NotificationService.startOrUpdate(context)
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

                NotificationService.startOrUpdate(context)
            }
            "ACTION_SKIP_NEXT" -> {
                val next = MusicHolder.getNext()

                if (next != null) {
                    MusicHolder.setPlayedMusic(context, next)
                }

                NotificationService.startOrUpdate(context)
            }
            "ACTION_FORWARD_10" -> {
                MusicPlayerManager.forward10Seconds()
            }
        }
    }
}
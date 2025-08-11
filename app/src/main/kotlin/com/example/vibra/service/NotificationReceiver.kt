package com.example.vibra.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.vibra.model.MusicHolder
import com.example.vibra.model.MusicPlayerManager

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "ACTION_PREVIOUS" -> {
                MusicHolder.getPrevious()?.let {
                    MusicHolder.setPlayedMusic(context, it)
                }
                NotificationService.update(context)
            }
            "ACTION_REWIND_10" -> {
                MusicPlayerManager.rewind10Seconds()
                NotificationService.update(context)
            }
            "ACTION_PLAY_PAUSE" -> {
                // toggle play/pause
                val current = MusicHolder.getCurrentMusic()
                if (MusicPlayerManager.isPlaying()) {
                    MusicPlayerManager.pauseMusic(context)
                } else {
                    current?.let { MusicPlayerManager.playMusic(context, it) }
                }
                NotificationService.update(context)
            }
            // Compatibility: si on reçoit explicitement ACTION_PLAY / ACTION_PAUSE (rare),
            // on les traite également.
            "ACTION_PLAY" -> {
                val current = MusicHolder.getCurrentMusic()
                current?.let { MusicPlayerManager.playMusic(context, it) }
                NotificationService.update(context)
            }
            "ACTION_PAUSE" -> {
                MusicPlayerManager.pauseMusic(context)
                NotificationService.update(context)
            }
            "ACTION_FORWARD_10" -> {
                MusicPlayerManager.forward10Seconds()
                NotificationService.update(context)
            }
            "ACTION_NEXT" -> {
                MusicHolder.getNext()?.let {
                    MusicHolder.setPlayedMusic(context, it)
                }
                NotificationService.update(context)
            }
        }
    }
}

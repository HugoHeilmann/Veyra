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
                val previous = MusicHolder.getPrevious()
                previous?.let {
                    MusicHolder.setPlayedMusic(context, it)
                }
            }
            "ACTION_PLAY_PAUSE" -> {
                // À adapter selon ton PlayerManager
                val current = MusicHolder.getCurrentMusic()
                if (MusicPlayerManager.isPlaying()) {
                    MusicPlayerManager.pauseMusic()
                } else {
                    current?.let { MusicPlayerManager.playMusic(context, it) }
                }
            }
            "ACTION_NEXT" -> {
                val next = MusicHolder.getNext()
                next?.let {
                    MusicHolder.setPlayedMusic(context, it)
                }
            }
        }
    }
}

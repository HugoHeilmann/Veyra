package com.example.veyra.model.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.veyra.model.metadata.MetadataManager
import com.example.veyra.utils.loadMusicFromDevice
import com.example.veyra.utils.scanMusicFolder

object MusicLibrarySyncManager {
    var isSyncing by mutableStateOf(false)
        private set

    suspend fun sync(context: Context) {
        if (isSyncing) return

        isSyncing = true

        try {
            scanMusicFolder(context)
            val updatedMusics = loadMusicFromDevice(context)
            MetadataManager.cleanup(context)

            MusicHolder.setMusicList(updatedMusics)
        } finally {
            isSyncing = false
        }
    }
}
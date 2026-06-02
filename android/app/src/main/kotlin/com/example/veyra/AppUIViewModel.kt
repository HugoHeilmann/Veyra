package com.example.veyra

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.veyra.model.Music
import com.example.veyra.model.data.MusicHolder
import com.example.veyra.model.metadata.MetadataManager
import com.example.veyra.model.metadata.toMusic
import com.example.veyra.utils.loadMusicFromDevice
import com.example.veyra.utils.scanMusicFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppUIViewModel : ViewModel() {

    var isBottomBarEnabled by mutableStateOf(false)
        private set

    var isSyncingLibrary by mutableStateOf(false)
        private set

    var displayedMusics by mutableStateOf<List<Music>>(emptyList())
        private set

    fun updateBottomBarEnabled(enabled: Boolean) {
        isBottomBarEnabled = enabled
    }

    fun initializeLibrary(context: Context) {
        val appContext = context.applicationContext

        if (displayedMusics.isNotEmpty()) return

        viewModelScope.launch {
            updateBottomBarEnabled(false)

            val cachedMusics = withContext(Dispatchers.IO) {
                MetadataManager.readAll(appContext)
                    .map { it.toMusic() }
            }

            MusicHolder.setMusicList(cachedMusics)
            displayedMusics = cachedMusics
            updateBottomBarEnabled(true)

            syncLibrary(appContext)
        }
    }

    fun syncLibrary(context: Context) {
        val appContext = context.applicationContext

        if (isSyncingLibrary) return

        viewModelScope.launch {
            isSyncingLibrary = true

            try {
                val updatedMusics = withContext(Dispatchers.IO) {
                    scanMusicFolder(appContext)

                    val musics = loadMusicFromDevice(appContext)

                    MetadataManager.cleanup(appContext)

                    musics
                }

                MusicHolder.setMusicList(updatedMusics)
                displayedMusics = updatedMusics
            } finally {
                isSyncingLibrary = false
                updateBottomBarEnabled(true)
            }
        }
    }

    fun refreshDisplayedMusicsFromHolder() {
        displayedMusics = MusicHolder.getMusicList().toList()
    }
}
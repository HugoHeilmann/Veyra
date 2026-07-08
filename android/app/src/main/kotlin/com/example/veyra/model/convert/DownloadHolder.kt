package com.example.veyra.model.convert

import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.example.veyra.model.Music

object DownloadHolder {

    var status = mutableStateOf("En attente d'un téléchargement...")
    var state = mutableIntStateOf(0)
    var progress = mutableFloatStateOf(0f)
    var isLoading = mutableStateOf(false)

    var downloadedMusic = mutableStateOf<Music?>(null)

    fun reset() {
        status.value = "En attente d'un téléchargement..."
        state.intValue = 0
        progress.floatValue = 0f
        downloadedMusic.value = null
    }
}
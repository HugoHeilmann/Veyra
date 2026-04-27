package com.example.veyra.model.convert

import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf

object DownloadHolder {

    var status = mutableStateOf("En attente d'un téléchargement...")
    var state = mutableIntStateOf(0)
    var progress = mutableFloatStateOf(0f)
    var isLoading = mutableStateOf(false)

    fun reset() {
        status.value = "En attente d'un téléchargement..."
        state.intValue = 0
        progress.floatValue = 0f
    }
}
package com.example.veyra.model.convert

import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf

object DownloadHolder {

    var status = mutableStateOf("OK")
    var progress = mutableFloatStateOf(0f)

    fun reset() {
        status.value = "OK"
        progress.floatValue = 0f
    }
}
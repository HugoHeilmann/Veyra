package com.example.veyra.model.convert

import androidx.compose.runtime.mutableStateOf

object DownloadHolder {

    var status = mutableStateOf("OK")

    fun reset() {
        status.value = "OK"
    }
}
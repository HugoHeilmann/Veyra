package com.example.veyra.model.controllers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object WaveBarsController {

    var isPlaying by mutableStateOf(false)
        private set

    fun start() { isPlaying = true }
    fun stop() { isPlaying = false }
}
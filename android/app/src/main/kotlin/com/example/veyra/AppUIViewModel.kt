package com.example.veyra

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class AppUIViewModel: ViewModel() {
    var isBottomBarEnabled by mutableStateOf(false)
        private set

    fun updateBottomBarEnabled(enabled: Boolean) {
        isBottomBarEnabled = enabled
    }
}
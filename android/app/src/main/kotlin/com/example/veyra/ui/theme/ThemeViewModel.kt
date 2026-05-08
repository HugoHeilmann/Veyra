package com.example.veyra.ui.theme

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ThemeViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = ThemePreferences(app.applicationContext)

    val primaryColor: StateFlow<Color> =
        prefs.primaryColorArgb
            .map { Color(it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = Color(0xFF00FF00)
            )

    val isDarkTheme: StateFlow<Boolean> =
        prefs.isDarkTheme
            .map { it }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = true
            )

    fun setPrimaryColor(color: Color) {
        viewModelScope.launch {
            prefs.setPrimaryColorArgb(color.toArgb())
        }
    }

    fun setTheme(isDarkTheme: Boolean) {
        viewModelScope.launch {
            prefs.setTheme(isDarkTheme)
        }
    }
}

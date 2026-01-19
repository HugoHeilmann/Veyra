package com.example.veyra.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "veyra_settings")

object ThemePreferencesKeys {
    val PRIMARY_COLOR_ARGB = intPreferencesKey("primary_color_argb")
}

class ThemePreferences(private val context: Context) {

    val primaryColorArgb: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[ThemePreferencesKeys.PRIMARY_COLOR_ARGB] ?: 0xFF00FF00.toInt() // vert par défaut
    }

    suspend fun setPrimaryColorArgb(argb: Int) {
        context.dataStore.edit { prefs ->
            prefs[ThemePreferencesKeys.PRIMARY_COLOR_ARGB] = argb
        }
    }
}

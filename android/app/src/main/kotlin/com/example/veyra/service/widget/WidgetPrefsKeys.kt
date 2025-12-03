package com.example.veyra.service.widget

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object WidgetPrefsKeys {
    val TITLE = stringPreferencesKey("widget_title")
    val ARTIST = stringPreferencesKey("widget_artist")
    val ALBUM = stringPreferencesKey("widget_album")
    val IS_PLAYING = booleanPreferencesKey("widget_is_playing")
}
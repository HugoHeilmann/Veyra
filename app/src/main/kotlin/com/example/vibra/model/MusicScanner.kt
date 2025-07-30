package com.example.vibra.model

import android.content.ContentUris
import android.util.Log
import android.content.Context
import android.provider.MediaStore
import com.example.vibra.R

fun loadMusicFromDevice(context: Context): List<Music> {
    val musicList = mutableListOf<Music>()

    val contentResolver = context.contentResolver
    val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.DATA
    )

    val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
    val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

    val cursor = contentResolver.query(
        uri,
        projection,
        selection,
        null,
        sortOrder
    )

    cursor?.use { it ->
        val dataColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
        val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)

        while (it.moveToNext()) {
            val data = it.getString(dataColumn)

            val rawTitle = it.getString(titleColumn)

            // Filtrer uniquement les .mp3 dans le dossier /Music/
            if (data.endsWith(".mp3", ignoreCase = true) && data.contains("/Music/")) {
                val parts = rawTitle.split(" - ")

                val artist = parts.getOrNull(0)?.takeIf { it.isNotBlank() } ?: "Unknown Artist"
                val title = parts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: "Unknown Title"
                val album = parts.getOrNull(2)?.takeIf { it.isNotBlank() } ?: "Unknown Album"

                musicList.add(
                    Music(
                        name = title,
                        artist = artist,
                        album = album,
                        image = R.drawable.default_album_cover,
                        uri = data
                    )
                )
            }
        }
    }

    return musicList
}
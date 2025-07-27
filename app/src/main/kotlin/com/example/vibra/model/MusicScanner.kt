package com.example.vibra.model

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

    cursor?.use {
        val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val dataColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

        while (it.moveToNext()) {
            val title = it.getString(titleColumn) ?: "Unknown Title"
            val artist = it.getString(artistColumn)
            val album = it.getString(albumColumn)
            val data = it.getString(dataColumn)

            // Filtrer uniquement les .mp3 dans le dossier /Music/
            if (data.endsWith(".mp3", ignoreCase = true) && data.contains("/Music/")) {
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
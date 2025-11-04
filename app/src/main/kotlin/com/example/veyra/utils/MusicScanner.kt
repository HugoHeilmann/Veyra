package com.example.veyra.utils

import android.content.Context
import android.provider.MediaStore
import com.example.veyra.R
import com.example.veyra.model.Music
import com.example.veyra.model.metadata.MetadataManager
import com.example.veyra.model.metadata.MusicMetadata
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

suspend fun loadMusicFromDevice(context: Context): List<Music> = coroutineScope {
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

    val tempList = mutableListOf<Music>()
    cursor?.use { it ->
        val dataColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
        val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)

        val deferredList = mutableListOf<Deferred<Music?>>()

        while (it.moveToNext()) {
            val data = it.getString(dataColumn)
            val rawTitle = it.getString(titleColumn)

            deferredList += async(Dispatchers.Default) {
                if (data.endsWith(".mp3", ignoreCase = true) && data.contains("/Music/")) {
                    val parts = rawTitle.split(" - ")

                    val filename = data.substringAfterLast("/")
                    val title = parts.getOrNull(0)?.takeIf { it.isNotBlank() } ?: "Unknown Title"
                    val artist = parts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: "Unknown Artist"
                    val album = parts.getOrNull(2)?.takeIf { it.isNotBlank() } ?: "Unknown Album"

                    val existingMetadata = MetadataManager.getByPath(context, data)
                    val coverPath = existingMetadata?.coverPath

                    val metadata = MusicMetadata(
                        fileName = filename,
                        title = title,
                        artist = artist,
                        album = album,
                        filePath = data,
                        coverPath = coverPath
                    )
                    MetadataManager.addIfNotExists(context, metadata)

                    Music(
                        name = title,
                        artist = artist,
                        album = album,
                        image = if (coverPath != null) 0 else R.drawable.default_album_cover,
                        uri = data
                    )
                } else null
            }
        }

        tempList.addAll(deferredList.awaitAll().filterNotNull())
    }

    return@coroutineScope tempList
}
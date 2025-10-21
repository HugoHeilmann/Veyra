package com.example.veyra.utils

import android.content.Context
import android.provider.MediaStore
import com.example.veyra.R
import com.example.veyra.model.Music
import com.example.veyra.model.metadata.MetadataManager
import com.example.veyra.model.metadata.MusicMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun loadMusicFromDeviceStream(
    context: Context,
    onBatchLoaded: suspend (List<Music>) -> Unit
) = withContext(Dispatchers.IO) {
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
    val batch = mutableListOf<Music>()
    var count = 0

    cursor?.use { it ->
        val dataColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
        val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)

        while (it.moveToNext()) {
            val path = it.getString(dataColumn)
            if (!path.endsWith(".mp3", true) || !path.contains("/Music/")) continue

            val titleRaw = it.getString(titleColumn)
            val artistRaw = it.getString(artistColumn)
            val albumRaw = it.getString(albumColumn)

            val parts = titleRaw?.split(" - ") ?: emptyList()
            val filename = path.substringAfterLast("/")

            val title = parts.getOrNull(0)?.takeIf { it.isNotBlank() } ?: titleRaw ?: "Unknown Title"
            val artist = parts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: artistRaw ?: "Unknown Artist"
            val album = parts.getOrNull(2)?.takeIf { it.isNotBlank() } ?: albumRaw ?: "Unknown Album"

            val existingMetadata = MetadataManager.getByPath(context, path)
            val coverPath = existingMetadata?.coverPath

            batch.add(
                Music(
                    name = title,
                    artist = artist,
                    album = album,
                    image = R.drawable.default_album_cover,
                    uri = path
                )
            )

            val metadata = MusicMetadata(
                fileName = filename,
                title = title,
                artist = artist,
                album = album,
                filePath = path,
                coverPath = coverPath
            )
            MetadataManager.addIfNotExists(context, metadata)

            if (++count % 50 == 0) {
                onBatchLoaded(batch.toList())
                batch.clear()
            }
        }
    }

    if (batch.isNotEmpty()) onBatchLoaded(batch.toList())
}
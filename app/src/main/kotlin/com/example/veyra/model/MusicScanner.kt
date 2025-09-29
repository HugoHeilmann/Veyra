package com.example.veyra.model

import android.content.Context
import android.provider.MediaStore
import com.example.veyra.R
import com.example.veyra.model.metadata.MetadataManager

fun loadMusicFromDevice(context: Context): List<Music> {
    val musicList = mutableListOf<Music>()
    val cr = context.contentResolver
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

    cr.query(uri, projection, selection, null, sortOrder)?.use { c ->
        val dataCol   = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
        val titleCol  = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val albumCol  = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)

        while (c.moveToNext()) {
            val path = c.getString(dataCol) ?: continue

            // Filtre: uniquement /Music/*.mp3
            if (!path.endsWith(".mp3", ignoreCase = true) || !path.contains("/Music/")) continue

            val filename = path.substringAfterLast('/')

            // Valeurs brutes depuis MediaStore
            val rawTitle  = c.getString(titleCol)
            val rawArtist = c.getString(artistCol)
            val rawAlbum  = c.getString(albumCol)

            // MediaStore peut renvoyer "<unknown>"
            val artist = rawArtist?.takeIf { it.isNotBlank() && it != MediaStore.UNKNOWN_STRING }
            val album  = rawAlbum ?.takeIf { it.isNotBlank() && it != MediaStore.UNKNOWN_STRING }

            // Toujours un titre : tag si dispo, sinon nom de fichier sans extension
            val title = rawTitle?.takeIf { it.isNotBlank() && it != MediaStore.UNKNOWN_STRING }
                ?: filename.removeSuffix(".mp3")

            val existingMeta = MetadataManager.getByPath(context, path)
            val coverPath = existingMeta?.coverPath

            musicList += Music(
                name = title,
                artist = artist,
                album = album,
                image = if (coverPath != null) 0 else R.drawable.default_album_cover,
                uri = path
            )

            val md = MusicMetadata(
                fileName = filename,
                title = title,
                artist = artist ?: "Unknown Artist",
                album = album ?: "Unknown Album",
                filePath = path,
                playlists = existingMeta?.playlists ?: mutableListOf(),
                coverPath = coverPath
            )
            MetadataManager.addIfNotExists(context, md)
        }
    }
    return musicList
}

package com.example.veyra.utils

import android.content.Context
import android.provider.MediaStore
import com.example.veyra.R
import com.example.veyra.model.Music
import com.example.veyra.model.metadata.MetadataManager
import com.example.veyra.model.metadata.MusicMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

private val SUPPORTED_EXTENSIONS = listOf(
    ".mp3",
    ".flac",
    ".aac",
    ".wav",
    ".ogg",
    ".oga",
    ".m4a",
    ".opus"
)

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun loadMusicFromDevice(context: Context): List<Music> = coroutineScope {
    withContext(Dispatchers.IO) {
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
        ) ?: return@withContext emptyList<Music>()

        data class Row(
            val path: String,
            val rawTitle: String?,
            val rawArtist: String?,
            val rawAlbum: String?
        )

        val rows = mutableListOf<Row>()

        cursor.use { it ->
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)

            while (it.moveToNext()) {
                val data = it.getString(dataColumn) ?: continue
                val rawTitle = it.getString(titleColumn)
                val rawArtist = it.getString(artistColumn)
                val rawAlbum = it.getString(albumColumn)

                val isSupportedExtension = SUPPORTED_EXTENSIONS.any { ext ->
                    data.endsWith(ext, ignoreCase = true)
                }

                val isInMusicFolder = data.contains("/Music/", ignoreCase = true)

                if (isSupportedExtension && isInMusicFolder) {
                    rows += Row(
                        path = data,
                        rawTitle = rawTitle,
                        rawArtist = rawArtist,
                        rawAlbum = rawAlbum
                    )
                }
            }
        }

        if (rows.isEmpty()) return@withContext emptyList<Music>()

        coroutineScope {
            val workerDispatcher = Dispatchers.Default.limitedParallelism(8)

            val deferredList = rows.map { row ->
                async(workerDispatcher) {
                    val data = row.path
                    val filename = data.substringAfterLast("/").substringBeforeLast(".")

                    val existingMetadata = MetadataManager.getByPath(context, data)
                    val coverPath = existingMetadata?.coverPath

                    val title = row.rawTitle
                        ?.takeIf { it.isNotBlank() && !it.equals("<unknown>", ignoreCase = true) }
                        ?: existingMetadata?.title
                        ?: filename

                    val artist = row.rawArtist
                        ?.takeIf { it.isNotBlank() && !it.equals("<unknown>", ignoreCase = true) }
                        ?: existingMetadata?.artist
                        ?: "Unknown Artist"

                    val album = row.rawAlbum
                        ?.takeIf { it.isNotBlank() && !it.equals("<unknown>", ignoreCase = true) }
                        ?: existingMetadata?.album
                        ?: "Unknown Album"

                    val metadata = MusicMetadata(
                        fileName = data.substringAfterLast("/"),
                        title = title,
                        artist = artist,
                        album = album,
                        filePath = data,
                        coverPath = coverPath
                    )

                    MetadataManager.addIfNotExists(context, metadata)
                    MetadataManager.updateMetadata(
                        context = context,
                        filePath = data,
                        title = title,
                        artist = artist,
                        album = album,
                        coverPath = coverPath
                    )

                    Music(
                        name = title,
                        artist = artist,
                        album = album,
                        image = if (coverPath != null) 0 else R.drawable.default_album_cover,
                        uri = data,
                        coverPath = coverPath
                    )
                }
            }

            deferredList.awaitAll()
        }
    }
}
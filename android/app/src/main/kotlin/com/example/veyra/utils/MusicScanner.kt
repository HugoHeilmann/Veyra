package com.example.veyra.utils

import android.content.Context
import android.provider.MediaStore
import com.example.veyra.R
import com.example.veyra.model.Music
import com.example.veyra.model.metadata.MetadataManager
import com.example.veyra.model.metadata.MusicMetadata
import kotlinx.coroutines.Deferred
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

        data class Row(val path: String, val rawTitle: String)
        val rows = mutableListOf<Row>()

        cursor.use { it ->
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)

            while (it.moveToNext()) {
                val data = it.getString(dataColumn) ?: continue
                val rawTitle = it.getString(titleColumn) ?: ""

                val isSupportedExtension = SUPPORTED_EXTENSIONS.any { ext ->
                    data.endsWith(ext, ignoreCase = true)
                }

                val isInMusicFolder = data.contains("/Music/", ignoreCase = true)

                if (isSupportedExtension && isInMusicFolder) {
                    rows += Row(data, rawTitle)
                }
            }
        }

        if (rows.isEmpty()) return@withContext emptyList<Music>()

        coroutineScope {
            val workerDispatcher = Dispatchers.Default.limitedParallelism(8)

            val deferredList = rows.map { row ->
                async(workerDispatcher) {
                    val data = row.path
                    val rawTitle = row.rawTitle

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
                }
            }

            deferredList.awaitAll()
        }
    }
}
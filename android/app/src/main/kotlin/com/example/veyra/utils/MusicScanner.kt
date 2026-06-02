package com.example.veyra.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
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
import java.io.File
import java.io.FileOutputStream

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

fun scanMusicFolder(context: Context) {
    val musicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath)

    if (musicDir.exists()) {
        musicDir.listFiles()?.forEach { file ->
            if (file.extension.equals("mp3", ignoreCase = true)) {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(file.absolutePath),
                    arrayOf("audio/mpeg")
                ) { path, uri ->
                    Log.d("Scan", "Fichier scanné : $path -> $uri")
                }
            }
        }
    } else {
        Log.d("Scan", "Dossier /Music/ introuvable")
    }
}

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

                    val file = File(data)
                    val fileLastModified = file.lastModified()

                    val hasChanged = existingMetadata?.lastModified != fileLastModified

                    val coverPath = if (hasChanged) {
                        extractEmbeddedCoverToCache(context, data) ?: existingMetadata?.coverPath
                    } else {
                        existingMetadata.coverPath
                    }

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
                        coverPath = coverPath,
                        lastModified = fileLastModified
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

            val musics = deferredList.awaitAll()

            MetadataManager.rebuildFromMusics(context, musics)

            musics
        }
    }
}

private fun extractEmbeddedCoverToCache(context: Context, audioPath: String): String? {
    val retriever = MediaMetadataRetriever()

    return try {
        retriever.setDataSource(audioPath)

        val artBytes = retriever.embeddedPicture
        retriever.release()

        if (artBytes == null) return null

        val coversDir = File(context.cacheDir, "covers")
        if (!coversDir.exists()) coversDir.mkdirs()

        val safeName = audioPath.hashCode().toString()
        val coverFile = File(coversDir, "$safeName.jpg")

        FileOutputStream(coverFile).use { output ->
            output.write(artBytes)
        }

        coverFile.absolutePath
    } catch (e: Exception) {
        e.message?.let { Log.e("EXTRACT COVER", it) }
        null
    } finally {
        retriever.release()
    }
}
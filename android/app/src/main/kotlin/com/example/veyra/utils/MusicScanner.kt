package com.example.veyra.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.example.veyra.model.Music
import com.example.veyra.model.metadata.MetadataManager
import com.example.veyra.model.metadata.MusicMetadata
import com.example.veyra.model.metadata.toMusic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

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

suspend fun scanMusicFolder(
    context: Context
) = withContext(Dispatchers.IO) {
    val musicDir = File(
        Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_MUSIC
        ).absolutePath
    )

    if (!musicDir.exists()) {
        Log.d("Scan", "Dossier /Music/ introuvable")
        return@withContext
    }

    val mediaStorePaths = mutableSetOf<String>()

    context.contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        arrayOf(MediaStore.Audio.Media.DATA),
        null,
        null,
        null
    )?.use { cursor ->
        val dataColumn = cursor.getColumnIndexOrThrow(
            MediaStore.Audio.Media.DATA
        )

        while (cursor.moveToNext()) {
            cursor.getString(dataColumn)?.let(mediaStorePaths::add)
        }
    }

    val filesToScan = musicDir
        .walkTopDown()
        .filter { file ->
            file.isFile &&
                    SUPPORTED_EXTENSIONS.any { extension ->
                        file.name.endsWith(extension, ignoreCase = true)
                    }
        }
        .filter { file ->
            file.absolutePath !in mediaStorePaths
        }
        .toList()

    if (filesToScan.isEmpty()) {
        return@withContext
    }

    suspendCancellableCoroutine { continuation ->
        val remainingFiles = AtomicInteger(filesToScan.size)

        MediaScannerConnection.scanFile(
            context,
            filesToScan
                .map { it.absolutePath }
                .toTypedArray(),
            filesToScan
                .map { getAudioMimeType(it) }
                .toTypedArray()
        ) { path, uri ->
            Log.d("Scan", "Fichier scanné : $path -> $uri")

            if (
                remainingFiles.decrementAndGet() == 0 &&
                continuation.isActive
            ) {
                continuation.resume(Unit)
            }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun loadMusicFromDevice(
    context: Context
): List<Music> = withContext(Dispatchers.IO) {
    val metadataByPath = MetadataManager.readAll(context)
        .associateBy { it.filePath }

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

    data class Row(
        val path: String,
        val rawTitle: String?,
        val rawArtist: String?,
        val rawAlbum: String?
    )

    val rows = mutableListOf<Row>()

    contentResolver.query(
        uri,
        projection,
        selection,
        null,
        sortOrder
    )?.use { cursor ->
        val dataColumn = cursor.getColumnIndexOrThrow(
            MediaStore.Audio.Media.DATA
        )
        val titleColumn = cursor.getColumnIndexOrThrow(
            MediaStore.Audio.Media.TITLE
        )
        val artistColumn = cursor.getColumnIndexOrThrow(
            MediaStore.Audio.Media.ARTIST
        )
        val albumColumn = cursor.getColumnIndexOrThrow(
            MediaStore.Audio.Media.ALBUM
        )

        while (cursor.moveToNext()) {
            val data = cursor.getString(dataColumn) ?: continue

            val isSupportedExtension = SUPPORTED_EXTENSIONS.any { extension ->
                data.endsWith(extension, ignoreCase = true)
            }

            val isInMusicFolder = data.contains(
                "/Music/",
                ignoreCase = true
            )

            if (!isSupportedExtension || !isInMusicFolder) {
                continue
            }

            rows += Row(
                path = data,
                rawTitle = cursor.getString(titleColumn),
                rawArtist = cursor.getString(artistColumn),
                rawAlbum = cursor.getString(albumColumn)
            )
        }
    }

    val workerDispatcher = Dispatchers.IO.limitedParallelism(4)

    val metadataList = coroutineScope {
        rows.map { row ->
            async(workerDispatcher) {
                val file = File(row.path)
                val fileLastModified = file.lastModified()

                val existingMetadata = metadataByPath[row.path]

                if (
                    existingMetadata != null &&
                    existingMetadata.lastModified == fileLastModified
                ) {
                    return@async existingMetadata
                }

                val coverPath = extractEmbeddedCoverToCache(
                    context = context,
                    audioPath = row.path
                ) ?: existingMetadata?.coverPath

                val title = row.rawTitle
                    ?.takeIf {
                        it.isNotBlank() &&
                                !it.equals("<unknown>", ignoreCase = true)
                    }
                    ?: existingMetadata?.title
                    ?: file.nameWithoutExtension

                val artist = row.rawArtist
                    ?.takeIf {
                        it.isNotBlank() &&
                                !it.equals("<unknown>", ignoreCase = true)
                    }
                    ?: existingMetadata?.artist
                    ?: "Unknown Artist"

                val album = row.rawAlbum
                    ?.takeIf {
                        it.isNotBlank() &&
                                !it.equals("<unknown>", ignoreCase = true)
                    }
                    ?: existingMetadata?.album
                    ?: "Unknown Album"

                MusicMetadata(
                    fileName = file.name,
                    title = title,
                    artist = artist,
                    album = album,
                    filePath = row.path,
                    coverPath = coverPath,
                    lastModified = fileLastModified
                )
            }
        }.awaitAll()
    }

    MetadataManager.replaceAll(
        context = context,
        metadataList = metadataList
    )

    metadataList.map { metadata ->
        metadata.toMusic()
    }
}

private fun extractEmbeddedCoverToCache(
    context: Context,
    audioPath: String
): String? {
    val retriever = MediaMetadataRetriever()

    return try {
        retriever.setDataSource(audioPath)

        val artBytes = retriever.embeddedPicture
            ?: return null

        val coversDir = File(context.cacheDir, "covers")

        if (!coversDir.exists()) {
            coversDir.mkdirs()
        }

        val safeName = audioPath.hashCode().toString()
        val coverFile = File(coversDir, "$safeName.jpg")

        FileOutputStream(coverFile).use { output ->
            output.write(artBytes)
        }

        coverFile.absolutePath
    } catch (exception: Exception) {
        Log.e(
            "EXTRACT COVER",
            exception.message ?: "Erreur inconnue",
            exception
        )

        null
    } finally {
        retriever.release()
    }
}

private fun getAudioMimeType(
    file: File
): String {
    return when (file.extension.lowercase()) {
        "mp3" -> "audio/mpeg"
        "flac" -> "audio/flac"
        "aac" -> "audio/aac"
        "wav" -> "audio/wav"
        "ogg",
        "oga" -> "audio/ogg"

        "m4a" -> "audio/mp4"
        "opus" -> "audio/opus"

        else -> "audio/*"
    }
}
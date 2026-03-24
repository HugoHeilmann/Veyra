package com.example.veyra.utils

import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.net.toUri
import com.example.veyra.model.Music
import com.example.veyra.model.metadata.AudioTagWriteResult
import com.example.veyra.model.metadata.AudioTagWriter

sealed class BulkTagEditResult {
    data class Ready(
        val requests: List<BulkTagEditRequest>,
        val uris: List<Uri>
    ) : BulkTagEditResult()

    data class Failure(val message: String) : BulkTagEditResult()
}

data class BulkTagEditRequest(
    val oldFilePath: String,
    val contentUri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val coverPath: String?
)

object BulkTagEditManager {

    sealed class CustomEditPreparation {
        data class Success(val request: BulkTagEditRequest) : CustomEditPreparation()
        data class Failure(val message: String) : CustomEditPreparation()
    }

    fun prepareCustomEdit(
        context: Context,
        oldFilePath: String,
        title: String,
        artist: String,
        album: String,
        coverPath: String?
    ): CustomEditPreparation {
        val contentUri = findMediaStoreAudioUri(context, oldFilePath)
            ?: return CustomEditPreparation.Failure("Impossible de trouver le fichier dans MediaStore")

        return CustomEditPreparation.Success(
            BulkTagEditRequest(
                oldFilePath = oldFilePath,
                contentUri = contentUri,
                title = title.trim(),
                artist = artist.trim().ifBlank { "Unknown Artist" },
                album = album.trim().ifBlank { "Unknown Album" },
                coverPath = coverPath
            )
        )
    }

    fun prepareDeleteArtist(
        context: Context,
        musics: List<Music>
    ): BulkTagEditResult {
        val requests = musics.mapNotNull { music ->
            val contentUri = findMediaStoreAudioUri(context, music.uri) ?: return@mapNotNull null

            BulkTagEditRequest(
                oldFilePath = music.uri,
                contentUri = contentUri,
                title = music.name.trim(),
                artist = "Unknown Artist",
                album = music.album?.trim().takeUnless { it.isNullOrBlank() } ?: "Unknown Album",
                coverPath = music.coverPath
            )
        }

        if (requests.isEmpty()) {
            return BulkTagEditResult.Failure("Aucun fichier modifiable trouvé")
        }

        return BulkTagEditResult.Ready(
            requests = requests,
            uris = requests.map { it.contentUri }
        )
    }

    fun prepareDeleteAlbum(
        context: Context,
        musics: List<Music>
    ): BulkTagEditResult {
        val requests = musics.mapNotNull { music ->
            val contentUri = findMediaStoreAudioUri(context, music.uri) ?: return@mapNotNull null

            BulkTagEditRequest(
                oldFilePath = music.uri,
                contentUri = contentUri,
                title = music.name.trim(),
                artist = music.artist?.trim().takeUnless { it.isNullOrBlank() } ?: "Unknown Artist",
                album = "Unknown Album",
                coverPath = music.coverPath
            )
        }

        if (requests.isEmpty()) {
            return BulkTagEditResult.Failure("Aucun fichier modifiable trouvé")
        }

        return BulkTagEditResult.Ready(
            requests = requests,
            uris = requests.map { it.contentUri }
        )
    }

    fun createWriteRequestIntentSender(
        context: Context,
        uris: List<Uri>
    ): IntentSender? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return null
        }

        return MediaStore.createWriteRequest(
            context.contentResolver,
            uris
        ).intentSender
    }

    suspend fun applyAll(
        context: Context,
        requests: List<BulkTagEditRequest>
    ): List<Pair<BulkTagEditRequest, AudioTagWriteResult>> {
        return requests.map { request ->
            val result = AudioTagWriter.saveTagsAfterPermission(
                context = context,
                contentUri = request.contentUri,
                title = request.title,
                artist = request.artist,
                album = request.album,
                coverPath = request.coverPath,
                renameFileName = false
            )
            request to result
        }
    }

    @Suppress("DEPRECATION")
    private fun findMediaStoreAudioUri(
        context: Context,
        filePath: String
    ): Uri? {
        if (filePath.startsWith("content://")) {
            return filePath.toUri()
        }

        val projection = arrayOf(MediaStore.Audio.Media._ID)
        val selection = "${MediaStore.Audio.Media.DATA} = ?"
        val selectionArgs = arrayOf(filePath)

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(idIndex)
                return ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )
            }
        }

        return null
    }
}
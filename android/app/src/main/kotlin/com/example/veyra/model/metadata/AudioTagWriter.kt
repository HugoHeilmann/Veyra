package com.example.veyra.model.metadata

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.File
import java.io.FileOutputStream

sealed class AudioTagWriteResult {
    data class Success(
        val updatedFilePath: String? = null,
        val updatedContentUri: Uri? = null
    ) : AudioTagWriteResult()

    data class NeedsUserPermission(val contentUri: Uri) : AudioTagWriteResult()
    data class Failure(val message: String, val cause: Throwable? = null) : AudioTagWriteResult()
}

object AudioTagWriter {

    private const val TAG = "AudioTagWriter"

    fun saveTags(
        context: Context,
        filePath: String,
        title: String,
        artist: String,
        album: String,
        coverPath: String? = null,
        renameFileName: Boolean = false
    ): AudioTagWriteResult {
        return try {
            if (filePath.startsWith("content://")) {
                val contentUri = filePath.toUri()
                return writeThroughContentUri(
                    context = context,
                    contentUri = contentUri,
                    title = title,
                    artist = artist,
                    album = album,
                    coverPath = coverPath,
                    askPermissionIfNeeded = true,
                    renameFileName = renameFileName
                )
            }

            val file = File(filePath)
            if (!file.exists()) {
                return AudioTagWriteResult.Failure("Fichier audio introuvable")
            }

            if (isAppOwnedFile(context, filePath)) {
                val renamedFile = if (renameFileName) {
                    renameLocalFileIfNeeded(
                        originalFile = file,
                        requestedTitle = title
                    )
                } else {
                    file
                }

                writeMetadataToLocalFile(
                    file = renamedFile,
                    title = title,
                    artist = artist,
                    album = album,
                    coverPath = coverPath
                )

                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(renamedFile.absolutePath),
                    null,
                    null
                )

                if (renamedFile.absolutePath != file.absolutePath) {
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(file.absolutePath),
                        null,
                        null
                    )
                }

                AudioTagWriteResult.Success(
                    updatedFilePath = renamedFile.absolutePath,
                    updatedContentUri = null
                )
            } else {
                val contentUri = findMediaStoreAudioUriByPath(context, filePath)
                    ?: return AudioTagWriteResult.Failure(
                        "Impossible de retrouver ce fichier dans MediaStore"
                    )

                writeThroughContentUri(
                    context = context,
                    contentUri = contentUri,
                    title = title,
                    artist = artist,
                    album = album,
                    coverPath = coverPath,
                    askPermissionIfNeeded = true,
                    renameFileName = renameFileName
                )
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Erreur lors de la modification du fichier audio", t)
            AudioTagWriteResult.Failure(
                message = t.message ?: "Impossible de modifier le fichier audio",
                cause = t
            )
        }
    }

    fun saveTagsAfterPermission(
        context: Context,
        contentUri: Uri,
        title: String,
        artist: String,
        album: String,
        coverPath: String? = null,
        renameFileName: Boolean = false
    ): AudioTagWriteResult {
        return try {
            writeThroughContentUri(
                context = context,
                contentUri = contentUri,
                title = title,
                artist = artist,
                album = album,
                coverPath = coverPath,
                askPermissionIfNeeded = false,
                renameFileName = renameFileName
            )
        } catch (t: Throwable) {
            Log.e(TAG, "Erreur lors de la modification du fichier audio apres autorisation", t)
            AudioTagWriteResult.Failure(
                message = t.message ?: "Impossible de modifier le fichier audio",
                cause = t
            )
        }
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

    private fun writeThroughContentUri(
        context: Context,
        contentUri: Uri,
        title: String,
        artist: String,
        album: String,
        coverPath: String?,
        askPermissionIfNeeded: Boolean,
        renameFileName: Boolean
    ): AudioTagWriteResult {
        val originalDisplayName = getDisplayName(context, contentUri) ?: "audio.mp3"
        val extension = extractExtension(originalDisplayName)

        val finalDisplayName = if (renameFileName) {
            buildDisplayName(title, extension)
        } else {
            originalDisplayName
        }

        val tempSuffix = if (extension.isNotBlank()) ".$extension" else ".tmp"
        val tempFile = File.createTempFile("veyra_tag_edit_", tempSuffix, context.cacheDir)

        return try {
            context.contentResolver.openInputStream(contentUri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return AudioTagWriteResult.Failure(
                "Impossible de lire le fichier audio"
            )

            writeMetadataToLocalFile(
                file = tempFile,
                title = title,
                artist = artist,
                album = album,
                coverPath = coverPath
            )

            try {
                context.contentResolver.openFileDescriptor(contentUri, "rwt")?.use { pfd ->
                    FileOutputStream(pfd.fileDescriptor).use { output ->
                        tempFile.inputStream().use { input ->
                            input.copyTo(output)
                        }
                        output.flush()

                        output.channel.truncate(tempFile.length())
                        output.channel.force(true)
                    }
                } ?: return AudioTagWriteResult.Failure(
                    "Impossible d'ouvrir le fichier audio en ecriture"
                )

                if (finalDisplayName != originalDisplayName) {
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, finalDisplayName)
                    }

                    val updatedRows = context.contentResolver.update(
                        contentUri,
                        values,
                        null,
                        null
                    )

                    if (updatedRows <= 0) {
                        return AudioTagWriteResult.Failure(
                            "Impossible de renommer le fichier audio"
                        )
                    }
                }

                val updatedPath = getFilePathFromContentUri(context, contentUri)
                if (!updatedPath.isNullOrBlank()) {
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(updatedPath),
                        null,
                        null
                    )
                } else {
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(contentUri.toString()),
                        null,
                        null
                    )
                }

                AudioTagWriteResult.Success(
                    updatedFilePath = updatedPath,
                    updatedContentUri = contentUri
                )
            } catch (securityException: SecurityException) {
                if (askPermissionIfNeeded) {
                    AudioTagWriteResult.NeedsUserPermission(contentUri)
                } else {
                    AudioTagWriteResult.Failure(
                        "Autorisation d'ecriture refusee pour ce fichier",
                        securityException
                    )
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Erreur lors de la modification via MediaStore", t)
            AudioTagWriteResult.Failure(
                message = t.message ?: "Impossible de modifier le fichier audio",
                cause = t
            )
        } finally {
            tempFile.delete()
        }
    }

    private fun writeMetadataToLocalFile(
        file: File,
        title: String,
        artist: String,
        album: String,
        coverPath: String?
    ) {
        val audioFile = AudioFileIO.read(file)
        val tag = audioFile.tagOrCreateAndSetDefault

        tag.setField(FieldKey.TITLE, title)
        tag.setField(FieldKey.ARTIST, artist)
        tag.setField(FieldKey.ALBUM, album)

        tag.deleteArtworkField()

        if (!coverPath.isNullOrBlank()) {
            val coverFile = File(coverPath)

            if (coverFile.exists() && coverFile.length() > 0L) {
                val artwork = ArtworkFactory.createArtworkFromFile(coverFile)
                tag.setField(artwork)
            } else {
                Log.w(TAG, "Image introuvable ou vide : $coverPath")
            }
        }

        audioFile.commit()
    }

    private fun renameLocalFileIfNeeded(
        originalFile: File,
        requestedTitle: String
    ): File {
        val extension = extractExtension(originalFile.name)
        val desiredName = buildDisplayName(requestedTitle, extension)

        if (desiredName == originalFile.name) {
            return originalFile
        }

        val renamedFile = File(originalFile.parentFile, desiredName)

        if (renamedFile.exists() && renamedFile.absolutePath != originalFile.absolutePath) {
            throw IllegalStateException("Un fichier du meme nom existe deja")
        }

        val renamed = originalFile.renameTo(renamedFile)
        if (!renamed) {
            throw IllegalStateException("Impossible de renommer le fichier audio")
        }

        return renamedFile
    }

    private fun buildDisplayName(
        requestedTitle: String,
        extension: String
    ): String {
        val trimmedTitle = requestedTitle.trim()
        if (trimmedTitle.isBlank()) {
            throw IllegalStateException("Le nom du fichier ne peut pas etre vide")
        }

        val sanitizedBaseName = sanitizeFileName(
            trimmedTitle.removeSuffix(".$extension")
        )

        if (sanitizedBaseName.isBlank()) {
            throw IllegalStateException("Nom de fichier invalide")
        }

        return if (extension.isBlank()) {
            sanitizedBaseName
        } else {
            "$sanitizedBaseName.$extension"
        }
    }

    private fun sanitizeFileName(value: String): String {
        return value
            .replace(Regex("""[\\/:*?"<>|]"""), "_")
            .trim()
            .trimEnd('.')
    }

    private fun extractExtension(fileName: String): String {
        val lastDot = fileName.lastIndexOf('.')
        if (lastDot <= 0 || lastDot == fileName.lastIndex) {
            return ""
        }
        return fileName.substring(lastDot + 1)
    }

    private fun getDisplayName(context: Context, contentUri: Uri): String? {
        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)

        context.contentResolver.query(
            contentUri,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            val index = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) {
                return cursor.getString(index)
            }
        }

        return null
    }

    private fun isAppOwnedFile(context: Context, filePath: String): Boolean {
        val internalPath = context.filesDir.absolutePath
        val cachePath = context.cacheDir.absolutePath
        val externalAppPath = context.getExternalFilesDir(null)?.absolutePath
        val externalCachePath = context.externalCacheDir?.absolutePath

        return filePath.startsWith(internalPath) ||
                filePath.startsWith(cachePath) ||
                (externalAppPath != null && filePath.startsWith(externalAppPath)) ||
                (externalCachePath != null && filePath.startsWith(externalCachePath))
    }

    @Suppress("DEPRECATION")
    private fun findMediaStoreAudioUriByPath(
        context: Context,
        filePath: String
    ): Uri? {
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

    @Suppress("DEPRECATION")
    private fun getFilePathFromContentUri(
        context: Context,
        contentUri: Uri
    ): String? {
        val projection = arrayOf(MediaStore.Audio.Media.DATA)

        context.contentResolver.query(
            contentUri,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            val index = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
            if (index >= 0 && cursor.moveToFirst()) {
                return cursor.getString(index)
            }
        }

        return null
    }
}
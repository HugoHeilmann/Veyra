package com.example.veyra.model.metadata

import android.content.Context
import androidx.core.net.toUri
import com.example.veyra.model.MusicHolder
import com.example.veyra.model.MusicMetadata
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

object MetadataManager {

    private const val FILE_NAME = "metadata.json"
    private val gson = Gson()

    private fun getFile(context: Context): File {
        return File(context.filesDir, FILE_NAME)
    }

    fun initializeIfNeeded(context: Context) {
        val file = getFile(context)
        if (!file.exists()) {
            file.writeText("[]")
        }
    }

    // Lire tout le JSON comme une liste d’objets MusicMetadata
    fun readAll(context: Context): MutableList<MusicMetadata> {
        val file = getFile(context)
        if (!file.exists()) initializeIfNeeded(context)
        val json = file.readText()
        val type = object : TypeToken<MutableList<MusicMetadata>>() {}.type
        return gson.fromJson(json, type) ?: mutableListOf()
    }

    // Sauvegarder la liste complète
    fun writeAll(context: Context, list: List<MusicMetadata>) {
        val file = getFile(context)
        file.writeText(gson.toJson(list))
    }

    // Add entry if does not exists
    fun addIfNotExists(context: Context, metadata: MusicMetadata) {
        val list = readAll(context)

        // verify by fileName
        val exists = list.any { it.fileName == metadata.fileName }

        if (!exists) {
            list.add(metadata)
            writeAll(context, list)
        }
    }

    fun updateMetadata(
        context: Context,
        filePath: String,
        title: String,
        artist: String,
        album: String,
        coverPath: String? = null
    ) {
        val list = readAll(context)
        val index = list.indexOfFirst { it.filePath == filePath }

        if (index >= 0) {
            val existing = list[index]
            list[index] = existing.copy(
                title = title,
                artist = artist,
                album = album,
                coverPath = coverPath ?: existing.coverPath
            )
            writeAll(context, list)
        }
    }

    // Récupérer une entrée par chemin
    fun getByPath(context: Context, filePath: String): MusicMetadata? {
        return readAll(context).find { it.filePath == filePath }
    }

    // Remove all unused data
    fun cleanup(context: Context) {
        val list = readAll(context).toMutableList()
        val existingPaths = MusicHolder.getMusicList().map { it.uri }.toSet()

        // Remove suppressed musics
        val cleanedList = list.filter { metadata ->
            existingPaths.contains(metadata.filePath)
        }.toMutableList()

        // Covers verification
        for (i in cleanedList.indices) {
            val meta = cleanedList[i]

            if (!meta.coverPath.isNullOrEmpty()) {
                val path = meta.coverPath!!

                val isValid = when {
                    path.startsWith("content://") -> {
                        try {
                            val uri = path.toUri()
                            context.contentResolver.openInputStream(uri)?.close()
                            true
                        } catch (_: Exception) {
                            false
                        }
                    }
                    path.startsWith("file://") || path.startsWith("/") -> {
                        File(path).exists()
                    }
                    else -> false
                }

                if (!isValid) {
                    cleanedList[i] = meta.copy(coverPath = null)
                }
            }
        }

        writeAll(context, cleanedList)
    }
}
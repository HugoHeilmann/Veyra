package com.example.veyra.model.metadata

import android.content.Context
import androidx.core.net.toUri
import com.example.veyra.model.MusicHolder
import com.example.veyra.model.MusicMetadata
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.arthenica.ffmpegkit.FFmpegKit
import java.io.File
import java.io.IOException

object MetadataManager {

    private const val FILE_NAME = "metadata.json"
    private const val TMP_SUFFIX = ".tmp"
    private val gson = Gson()

    private fun getFile(context: Context): File = File(context.filesDir, FILE_NAME)

    private fun stableKey(raw: String): String {
        return if (raw.startsWith("content://")) {
            raw
        } else {
            try {
                File(raw).canonicalPath
            } catch (_: Exception) {
                raw
            }
        }
    }

    private fun writeTextAtomic(target: File, content: String) {
        val tmp = File(target.parentFile, target.name + TMP_SUFFIX)
        tmp.writeText(content)

        if (!tmp.renameTo(target)) {
            target.writeText(tmp.readText())
            tmp.delete()
        }
    }

    fun initializeIfNeeded(context: Context) {
        val file = getFile(context)
        if (!file.exists()) {
            writeTextAtomic(file, "[]")
        }
    }

    // Lire tout le JSON comme une liste d’objets MusicMetadata
    fun readAll(context: Context): MutableList<MusicMetadata> {
        val file = getFile(context)
        if (!file.exists()) initializeIfNeeded(context)
        val json = try {
            file.readText()
        } catch (_: IOException) {
            "[]"
        }

        val type = object : TypeToken<MutableList<MusicMetadata>>() {}.type
        return try {
            gson.fromJson<MutableList<MusicMetadata>>(json, type) ?: mutableListOf()
        } catch (_: JsonSyntaxException) {
            mutableListOf()
        }
    }

    // Sauvegarder la liste complète
    fun writeAll(context: Context, list: List<MusicMetadata>) {
        val file = getFile(context)
        val json = gson.toJson(list)
        writeTextAtomic(file, json)
    }

    // Add entry if does not exists
    fun addIfNotExists(context: Context, metadata: MusicMetadata) {
        val list = readAll(context)
        val keyNew = stableKey(metadata.filePath)
        val exists = list.any { stableKey(it.filePath) == keyNew }

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
        val key = stableKey(filePath)
        val index = list.indexOfFirst { stableKey(it.filePath) == key }

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
        val key = stableKey(filePath)
        return readAll(context).find { stableKey(it.filePath) == key }
    }

    // Remove all unused data
    fun cleanup(context: Context) {
        /*
        val list = readAll(context).toMutableList()

        val existingKey: Set<String> = MusicHolder.getMusicList()
            .map { it.uri }
            .map { stableKey(it) }
            .toSet()

        val cleanedList = list.filter { metadata ->
            existingKey.contains(stableKey(metadata.filePath))
        }.toMutableList()

        for (i in cleanedList.indices) {
            val meta = cleanedList[i]
            val path = meta.coverPath ?: continue

            val isValid = try {
                when {
                    path.startsWith("content://") -> {
                        context.contentResolver.openInputStream(path.toUri())?.close()
                        true
                    }
                    path.startsWith("file://") || path.startsWith("/") -> {
                        File(path).exists()
                    }
                    else -> false
                }
            } catch (_: Exception) {
                false
            }

            if (!isValid) {
                cleanedList[i] = meta.copy(coverPath = null)
            }
        }

        writeAll(context, cleanedList)*/
    }
}
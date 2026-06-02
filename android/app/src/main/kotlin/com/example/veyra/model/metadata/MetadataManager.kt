package com.example.veyra.model.metadata

import android.content.Context
import com.example.veyra.model.Music
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.IOException

object MetadataManager {

    private const val FILE_NAME = "metadata.json"
    private const val TMP_SUFFIX = ".tmp"
    private val gson = Gson()

    private fun getFile(context: Context): File = File(context.filesDir, FILE_NAME)

    private fun stableKey(raw: String?): String? {
        if (raw.isNullOrBlank()) return null

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
        val dir = target.parentFile
        if (dir != null && !dir.exists()) {
            dir.mkdirs()
        }

        val tmp = File(dir, target.name + TMP_SUFFIX)

        tmp.writeText(content)

        if (!tmp.renameTo(target)) {
            target.writeText(content)
            if (tmp.exists()) tmp.delete()
        }
    }

    fun initializeIfNeeded(context: Context) {
        val file = getFile(context)
        if (!file.exists()) {
            writeTextAtomic(file, "[]")
        }
    }

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

    fun writeAll(context: Context, list: List<MusicMetadata>) {
        val file = getFile(context)
        val json = gson.toJson(list)
        writeTextAtomic(file, json)
    }

    fun addIfNotExists(context: Context, metadata: MusicMetadata) {
        val keyNew = stableKey(metadata.filePath) ?: return

        val list = readAll(context)
        val exists = list.any { stableKey(it.filePath) == keyNew }

        if (!exists) {
            list.add(metadata)
            writeAll(context, list)
        }
    }

    fun rebuildFromMusics(context: Context, musics: List<Music>) {
        val existingByPath = readAll(context).associateBy { stableKey(it.filePath) }

        val metadataList = musics.map { music ->
            val existing = existingByPath[stableKey(music.uri)]

            MusicMetadata(
                fileName = File(music.uri).name,
                title = music.name,
                artist = music.artist ?: existing?.artist ?: "Unknown Artist",
                album = music.album ?: existing?.album ?: "Unknown Album",
                filePath = music.uri,
                coverPath = music.coverPath ?: existing?.coverPath
            )
        }

        writeAll(context, metadataList)
    }

    fun updateMetadata(
        context: Context,
        filePath: String?,
        title: String,
        artist: String,
        album: String,
        coverPath: String? = null,
        lastModified: Long? = null
    ) {
        val key = stableKey(filePath) ?: return

        val list = readAll(context)
        val index = list.indexOfFirst { stableKey(it.filePath) == key }

        if (index >= 0) {
            val existing = list[index]
            list[index] = existing.copy(
                fileName = File(filePath!!).name,
                title = title,
                artist = artist,
                album = album,
                coverPath = coverPath ?: existing.coverPath,
                lastModified = lastModified ?: existing.lastModified
            )
            writeAll(context, list)
        }
    }

    fun getByPath(context: Context, filePath: String?): MusicMetadata? {
        val key = stableKey(filePath) ?: return null
        return readAll(context).find { stableKey(it.filePath) == key }
    }

    fun renameFilePath(
        context: Context,
        oldPath: String?,
        newPath: String?
    ) {
        val oldKey = stableKey(oldPath) ?: return
        val newKey = stableKey(newPath) ?: return

        val list = readAll(context)

        val oldIndex = list.indexOfFirst { stableKey(it.filePath) == oldKey }
        val newIndex = list.indexOfFirst { stableKey(it.filePath) == newKey }

        if (oldIndex < 0) return

        val existing = list[oldIndex]

        if (newIndex >= 0) {
            list.removeAt(oldIndex)
        } else {
            list[oldIndex] = existing.copy(
                filePath = newPath!!,
                fileName = File(newPath).name
            )
        }

        writeAll(context, list)
    }

    fun cleanup(context: Context) {
        val list = readAll(context)
        val cleaned = linkedMapOf<String, MusicMetadata>()

        for (item in list) {
            val path = item.filePath
            if (path.isBlank()) continue

            val file = File(path)
            if (!file.exists() || !file.isFile) continue

            val key = stableKey(path) ?: continue
            val existing = cleaned[key]

            if (existing == null) {
                cleaned[key] = item.copy(fileName = file.name)
            } else {
                cleaned[key] = existing.copy(
                    fileName = file.name,
                    title = existing.title.ifBlank { item.title },
                    artist = existing.artist.ifBlank { item.artist },
                    album = existing.album.ifBlank { item.album },
                    coverPath = existing.coverPath ?: item.coverPath
                )
            }
        }

        writeAll(context, cleaned.values.toList())
    }
}
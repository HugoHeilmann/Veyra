package com.example.veyra.model.metadata

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.IOException

data class PlaylistMetadata(
    val name: String,
    val musicFiles: MutableList<String> = mutableListOf() // liste d'uris/chemins
)

object PlaylistManager {
    private const val FILE_NAME = "playlists.json"
    private const val TMP_SUFFIX = ".tmp"
    private val gson = Gson()

    // ---------- Utils ----------

    private fun getFile(context: Context): File =
        File(context.filesDir, FILE_NAME)

    /** Retourne une clé stable pour un chemin/uri */
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

    /** Écriture atomique : écrit dans un .tmp puis rename */
    private fun writeTextAtomic(target: File, content: String) {
        val tmp = File(target.parentFile, target.name + TMP_SUFFIX)
        tmp.writeText(content)
        if (!tmp.renameTo(target)) {
            // fallback
            target.writeText(tmp.readText())
            tmp.delete()
        }
    }

    // ---------- Initialisation ----------

    fun initializeIfNeeded(context: Context) {
        val file = getFile(context)
        if (!file.exists()) {
            writeTextAtomic(file, "[]")
        }
    }

    // ---------- Lecture / Écriture ----------

    fun readAll(context: Context): MutableList<PlaylistMetadata> {
        val file = getFile(context)
        if (!file.exists()) initializeIfNeeded(context)

        val json = try {
            file.readText()
        } catch (_: IOException) {
            "[]"
        }

        val type = object : TypeToken<MutableList<PlaylistMetadata>>() {}.type
        return try {
            gson.fromJson<MutableList<PlaylistMetadata>>(json, type) ?: mutableListOf()
        } catch (_: JsonSyntaxException) {
            mutableListOf()
        }
    }

    fun writeAll(context: Context, list: List<PlaylistMetadata>) {
        val file = getFile(context)
        val json = gson.toJson(list)
        writeTextAtomic(file, json)
    }

    // ---------- Gestion des playlists ----------

    /** Ajouter une playlist si elle n'existe pas (nom unique) */
    fun addIfNotExists(context: Context, playlist: PlaylistMetadata) {
        val list = readAll(context)
        if (list.none { it.name == playlist.name }) {
            // Canonicaliser les chemins/uris
            val normalized = playlist.copy(
                musicFiles = playlist.musicFiles.map { stableKey(it) }.toMutableList()
            )
            list.add(normalized)
            writeAll(context, list)
        }
    }

    /** Supprimer une playlist par nom */
    fun remove(context: Context, playlistName: String) {
        val list = readAll(context)
        val updated = list.filter { it.name != playlistName }
        writeAll(context, updated)
    }

    /** Récupérer une playlist par nom */
    fun getByName(context: Context, playlistName: String): PlaylistMetadata? {
        return readAll(context).find { it.name == playlistName }
    }

    /** Ajouter un morceau dans une playlist donnée */
    fun addMusicToPlaylist(context: Context, playlistName: String, filePathOrUri: String) {
        val list = readAll(context)
        val idx = list.indexOfFirst { it.name == playlistName }
        if (idx >= 0) {
            val key = stableKey(filePathOrUri)
            val playlist = list[idx]
            if (!playlist.musicFiles.any { stableKey(it) == key }) {
                playlist.musicFiles.add(key)
                list[idx] = playlist
                writeAll(context, list)
            }
        }
    }

    /** Retirer un morceau d’une playlist */
    fun removeMusicFromPlaylist(context: Context, playlistName: String, filePathOrUri: String) {
        val list = readAll(context)
        val idx = list.indexOfFirst { it.name == playlistName }
        if (idx >= 0) {
            val key = stableKey(filePathOrUri)
            val playlist = list[idx]
            val newFiles = playlist.musicFiles.filter { stableKey(it) != key }.toMutableList()
            list[idx] = playlist.copy(musicFiles = newFiles)
            writeAll(context, list)
        }
    }
}

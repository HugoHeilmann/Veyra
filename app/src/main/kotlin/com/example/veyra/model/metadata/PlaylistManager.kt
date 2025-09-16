package com.example.veyra.model.metadata

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

data class PlaylistMetadata(
    val name: String,
    val musicFiles: MutableList<String> = mutableListOf() // music path
)

object PlaylistManager {
    private const val FILE_NAME = "playlists.json"
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

    // Lire tout le JSON comme une liste de playlists
    fun readAll(context: Context): MutableList<PlaylistMetadata> {
        val file = getFile(context)
        if (!file.exists()) initializeIfNeeded(context)
        val json = file.readText()
        val type = object : TypeToken<MutableList<PlaylistMetadata>>() {}.type
        return gson.fromJson(json, type) ?: mutableListOf()
    }

    // Sauvegarder la liste complète
    fun writeAll(context: Context, list: List<PlaylistMetadata>) {
        val file = getFile(context)
        file.writeText(gson.toJson(list))
    }

    // Ajouter une playlist si elle n'existe pas
    fun addIfNotExists(context: Context, playlist: PlaylistMetadata) {
        val list = readAll(context)
        if (list.none { it.name == playlist.name }) {
            list.add(playlist)
            writeAll(context, list)
        }
    }

    // Supprimer une playlist par nom
    fun remove(context: Context, playlistName: String) {
        val list = readAll(context)
        val updated = list.filter { it.name != playlistName }
        writeAll(context, updated)
    }

    // Récupérer une playlist par nom
    fun getByName(context: Context, playlistName: String): PlaylistMetadata? {
        return readAll(context).find { it.name == playlistName }
    }
}
package com.example.veyra.model.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.veyra.model.Music
import com.example.veyra.model.metadata.PlaylistManager

object MusicHolder {
    private var currentMusic by mutableStateOf<Music?>(null)
    private var musicList: List<Music> = emptyList()

    private var originalContextList: List<Music> = emptyList()
    private var shuffledContextList: List<Music> = emptyList()

    private val artistMap = mutableMapOf<String, List<Music>>()
    private val albumMap = mutableMapOf<String, List<Music>>()
    private val playlistMap = mutableMapOf<String, List<Music>>()

    var isShuffled by mutableStateOf(true)

    fun buildPlaylistMap(context: Context, allMusic: List<Music>) {
        playlistMap.clear()
        val allPlaylists = PlaylistManager.readAll(context)

        allPlaylists.forEach { playlist ->
            val musics = playlist.musicFiles.mapNotNull { filePath ->
                allMusic.find { it.uri == filePath }
            }
            playlistMap[playlist.name] = musics
        }
    }

    fun setMusicList(list: List<Music>) {
        musicList = list.sortedBy { it.name.lowercase() }

        artistMap.clear()
        artistMap.putAll(
            musicList
                .filter { !it.artist.isNullOrBlank() }
                .groupBy {
                    it.artist?.split(Regex("(?i) ft\\."))?.get(0)?.trim() ?: "Unknown Artist"
                }
                .mapValues { entry ->
                    entry.value
                        .sortedBy { it.name.lowercase() }
                        .toMutableList()
                }
        )

        albumMap.clear()
        albumMap.putAll(
            musicList
                .filter { !it.album.isNullOrBlank() }
                .groupBy { it.album ?: "Unknown Album" }
                .mapValues { entry ->
                    entry.value
                        .sortedBy { it.name.lowercase() }
                        .toMutableList()
                }
        )

        playlistMap.clear()
    }

    fun setPlayedMusic(context: Context, music: Music) {
        currentMusic = music
        // La lecture déclenche la mise à jour de la notif via MusicPlayerManager
        MusicPlayerManager.playMusic(context, music)
    }

    fun setCurrentMusic(
        context: Context,
        music: Music,
        contextList: List<Music>? = null,
        keepOrder: Boolean = false
    ) {
        currentMusic = music

        originalContextList = when {
            contextList != null && keepOrder -> contextList
            contextList != null && !keepOrder -> contextList.sortedBy { it.name.lowercase() }
            else -> musicList.sortedBy { it.name.lowercase() }
        }
        shuffledContextList = originalContextList.shuffled()

        // On ne démarre plus directement le service de notif ici.
        // La notif sera gérée quand la musique sera effectivement lancée.
    }

    fun addMusic(music: Music) {
        val updatedList = musicList.toMutableList()
        updatedList.add(music)
        setMusicList(updatedList)
    }

    fun addArtist(name: String) {
        artistMap[name] = mutableListOf()
    }

    fun addAlbum(name: String) {
        albumMap[name] = mutableListOf()
    }

    fun enableShuffle(enabled: Boolean) {
        isShuffled = enabled
    }

    fun getMusicList(): List<Music> = musicList
    fun getArtistList(): List<String> =
        artistMap.keys.toMutableList().sortedWith(String.CASE_INSENSITIVE_ORDER)

    fun getArtistSongs(artist: String): List<Music> = artistMap[artist] ?: emptyList()
    fun getAlbumList(): List<String> =
        albumMap.keys.toMutableList().sortedWith(String.CASE_INSENSITIVE_ORDER)

    fun getAlbumSongs(album: String): List<Music> = albumMap[album] ?: emptyList()
    fun getPlaylistSongs(playlist: String): List<Music> = playlistMap[playlist] ?: emptyList()
    fun getCurrent(): Music? = currentMusic

    private fun getActiveList(): List<Music> {
        return if (isShuffled) shuffledContextList else originalContextList
    }

    fun getNext(): Music? {
        val list = getActiveList()
        val index = list.indexOf(currentMusic)
        return if (list.isNotEmpty() && index != -1) {
            list[(index + 1) % list.size]
        } else null
    }

    fun getPrevious(): Music? {
        val list = getActiveList()
        val index = list.indexOf(currentMusic)
        return if (list.isNotEmpty() && index != -1) {
            list[(index - 1 + list.size) % list.size]
        } else null
    }

    fun sanitizeMaps() {
        artistMap.entries.removeIf { it.value.isEmpty() }
        albumMap.entries.removeIf { it.value.isEmpty() }
    }

    fun addElement(
        music: Music,
        artist: String,
        album: String
    ) {
        if (artist !in artistMap.keys) {
            artistMap[artist] = mutableListOf(music)
        }

        if (album !in albumMap.keys) {
            albumMap[album] = mutableListOf(music)
        }

        // Remove empty keys
        artistMap.entries.removeAll { it.value.isEmpty() }
        albumMap.entries.removeAll { it.value.isEmpty() }
    }

    fun refreshMapsForMusic(music: Music) {
        artistMap.forEach { (key, list) ->
            val mutable = list.toMutableList()
            mutable.removeAll { it.uri == music.uri }
            artistMap[key] = mutable
        }

        val artistKey = music.artist?.split(Regex("(?i) ft\\."))?.getOrNull(0)?.trim()
        if (!artistKey.isNullOrBlank()) {
            val mutable = artistMap.getOrPut(artistKey) { mutableListOf() }.toMutableList()
            mutable.add(music)
            artistMap[artistKey] = mutable.sortedBy { it.name.lowercase() }
        }

        artistMap.entries.removeAll { it.value.isEmpty() }

        albumMap.forEach { (key, list) ->
            val mutable = list.toMutableList()
            mutable.removeAll { it.uri == music.uri }
            albumMap[key] = mutable
        }

        val albumKey = music.album?.trim()
        if (!albumKey.isNullOrBlank()) {
            val mutable = albumMap.getOrPut(albumKey) { mutableListOf() }.toMutableList()
            mutable.add(music)
            albumMap[albumKey] = mutable.sortedBy { it.name.lowercase() }
        }

        albumMap.entries.removeAll { it.value.isEmpty() }
    }

    fun reset() {
        currentMusic = null
        musicList = emptyList()
        originalContextList = emptyList()
        shuffledContextList = emptyList()
        artistMap.clear()
        albumMap.clear()
        playlistMap.clear()
        isShuffled = false
    }

    fun updateMusic(
        filePath: String,
        title: String,
        artist: String,
        album: String,
        coverPath: String? = null
    ) {
        this.musicList.forEach { music ->
            if (music.uri == filePath) {
                music.name = title
                music.artist = artist
                music.album = album
                music.coverPath = coverPath
            }
        }
    }
}
package com.example.veyra.model.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.veyra.model.Music
import com.example.veyra.model.metadata.PlaylistManager

object MusicHolder {
    var currentMusic by mutableStateOf<Music?>(null)
        private set

    var currentIndex by mutableIntStateOf(-1)
        private set

    private var musicList: List<Music> = emptyList()

    private val originalContextList = mutableStateListOf<Music>()
    private val shuffledContextList = mutableStateListOf<Music>()
    private val queueList = mutableStateListOf<Music>()

    var contextListName by mutableStateOf("")
        private set

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

    fun playMusic(context: Context, music: Music) {
        currentMusic = music
        syncCurrentIndexWithMusic()
        MusicPlayerManager.playMusic(context, music)
    }

    fun setPlayedMusic(context: Context, music: Music) {
        playMusic(context, music)
    }

    fun setCurrentMusic(
        context: Context,
        music: Music,
        contextName: String,
        contextList: List<Music>? = null,
        keepOrder: Boolean = false,
    ) {
        setContextName(contextName)
        currentMusic = music

        val newOriginal = when {
            contextList != null && keepOrder -> contextList
            contextList != null && !keepOrder -> contextList.sortedBy { it.name.lowercase() }
            else -> musicList.sortedBy { it.name.lowercase() }
        }

        originalContextList.clear()
        originalContextList.addAll(newOriginal)

        shuffledContextList.clear()
        shuffledContextList.addAll(originalContextList.shuffled())

        syncCurrentIndexWithMusic()
    }

    fun setContextName(name: String) {
        contextListName = name
    }

    fun playNext(context: Context) {
        val list = getActiveList()
        if (list.isEmpty()) return

        val nextIndex = when {
            currentIndex in list.indices -> (currentIndex + 1) % list.size
            else -> 0
        }

        currentIndex = nextIndex
        currentMusic = list[nextIndex]

        if (currentMusic in queueList) {
            queueList.remove(currentMusic)
        }

        MusicPlayerManager.playMusic(context, list[nextIndex])
    }

    fun playPrevious(context: Context) {
        val list = getActiveList()
        if (list.isEmpty()) return

        val previousIndex = when {
            currentIndex in list.indices -> (currentIndex - 1 + list.size) % list.size
            else -> list.lastIndex
        }

        currentIndex = previousIndex
        currentMusic = list[previousIndex]
        MusicPlayerManager.playMusic(context, list[previousIndex])
    }

    fun clearCurrentMusic() {
        currentMusic = null
        currentIndex = -1
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
        if (isShuffled == enabled) return

        isShuffled = enabled
        syncCurrentIndexWithMusic()
    }

    fun getMusicList(): List<Music> = musicList

    fun getArtistList(): List<String> =
        artistMap.keys.toMutableList().sortedWith(String.CASE_INSENSITIVE_ORDER)

    fun getArtistSongs(artist: String): List<Music> = artistMap[artist] ?: emptyList()

    fun getAlbumList(): List<String> =
        albumMap.keys.toMutableList().sortedWith(String.CASE_INSENSITIVE_ORDER)

    fun getAlbumSongs(album: String): List<Music> = albumMap[album] ?: emptyList()
    fun getQueue(): List<Music> = queueList
    fun getPlaylistSongs(playlist: String): List<Music> = playlistMap[playlist] ?: emptyList()
    fun getCurrent(): Music? = currentMusic

    fun getActiveList(): List<Music> {
        return if (isShuffled) shuffledContextList else originalContextList
    }

    fun isInQueue(music: Music): Boolean {
        return queueList.contains(music)
    }

    fun isNext(music: Music): Boolean {
        return getNext()?.uri == music.uri
    }

    fun addInQueue(music: Music) {
        val current = currentMusic ?: return

        originalContextList.remove(music)
        shuffledContextList.remove(music)
        queueList.remove(music)
        queueList.add(0, music)

        val originalIndex = originalContextList.indexOfFirst { it.uri == current.uri }
        if (originalIndex != -1) {
            originalContextList.add(originalIndex + 1, music)
        } else {
            originalContextList.add(music)
        }

        val shuffledIndex = shuffledContextList.indexOfFirst { it.uri == current.uri }
        if (shuffledIndex != -1) {
            shuffledContextList.add(shuffledIndex + 1, music)
        } else {
            shuffledContextList.add(music)
        }

        syncCurrentIndexWithMusic()
    }

    fun removeFromQueue(music: Music) {
        originalContextList.remove(music)
        shuffledContextList.remove(music)
        queueList.remove(music)

        val index = originalContextList.indexOfFirst {
            it.name.lowercase() > music.name.lowercase()
        }

        if (index == -1) {
            originalContextList.add(music)
        } else {
            originalContextList.add(index, music)
        }

        shuffledContextList.add(music)

        syncCurrentIndexWithMusic()
    }

    fun getNext(): Music? {
        val list = getActiveList()
        if (list.isEmpty() || currentIndex !in list.indices) return null
        return list[(currentIndex + 1) % list.size]
    }

    fun getPrevious(): Music? {
        val list = getActiveList()
        if (list.isEmpty() || currentIndex !in list.indices) return null
        return list[(currentIndex - 1 + list.size) % list.size]
    }

    private fun syncCurrentIndexWithMusic() {
        val current = currentMusic
        if (current == null) {
            currentIndex = -1
            return
        }

        val list = getActiveList()
        currentIndex = list.indexOfFirst { it.uri == current.uri }
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

        syncCurrentIndexWithMusic()
    }

    fun reset() {
        currentMusic = null
        currentIndex = -1
        musicList = emptyList()
        originalContextList.clear()
        shuffledContextList.clear()
        queueList.clear()
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

        syncCurrentIndexWithMusic()
    }

    private fun normalizeArtistKey(value: String?): String {
        return value
            ?.split(Regex("(?i) ft\\."))
            ?.getOrNull(0)
            ?.trim()
            .orEmpty()
    }

    fun getMusicsForArtistDeletion(artistName: String): List<Music> {
        val targetArtist = artistName.trim()
        if (targetArtist.isBlank()) return emptyList()

        return musicList.filter { music ->
            normalizeArtistKey(music.artist).equals(targetArtist, ignoreCase = true)
        }
    }

    fun getMusicsForAlbumDeletion(albumName: String): List<Music> {
        val targetAlbum = albumName.trim()
        if (targetAlbum.isBlank()) return emptyList()

        return musicList.filter { music ->
            music.album?.trim().equals(targetAlbum, ignoreCase = true)
        }
    }

    fun applyBulkLocalUpdate(
        oldFilePath: String,
        newFilePath: String,
        title: String,
        artist: String,
        album: String,
        coverPath: String?
    ) {
        musicList.forEach { music ->
            if (music.uri == oldFilePath) {
                music.uri = newFilePath
                music.name = title
                music.artist = artist
                music.album = album
                music.coverPath = coverPath
            }
        }

        refreshAllMaps()
    }

    fun deleteArtist(artistName: String) {
        return
    }

    fun deleteAlbum(albumName: String) {
        return
    }

    private fun refreshAllMaps() {
        musicList = musicList.sortedBy { it.name.lowercase() }

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

        syncCurrentIndexWithMusic()
    }
}
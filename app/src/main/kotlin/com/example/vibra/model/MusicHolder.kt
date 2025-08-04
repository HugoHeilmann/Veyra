package com.example.vibra.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object MusicHolder {
    private var currentMusic: Music? = null
    private var musicList: List<Music> = emptyList()

    private var originalContextList: List<Music> = emptyList()
    private var shuffledContextList: List<Music> = emptyList()

    private val artistMap = mutableMapOf<String, List<Music>>()
    private val albumMap = mutableMapOf<String, List<Music>>()

    var isShuffled by mutableStateOf(false)
        private set

    fun setMusicList(list: List<Music>) {
        musicList = list.sortedBy { it.name.lowercase() }

        // Mise à jour des maps artistes et albums
        artistMap.clear()
        artistMap.putAll(
            musicList
                .filter { !it.artist.isNullOrBlank() }
                .groupBy {
                    it.artist?.split(Regex("(?i) ft\\."))?.get(0)?.trim() ?: "Unknown"
                }
                .mapValues { entry -> entry.value.sortedBy { it.name.lowercase() } }
        )

        albumMap.clear()
        albumMap.putAll(
            musicList
                .filter { !it.album.isNullOrBlank() }
                .groupBy { it.album ?: "Unfinished" }
                .mapValues { entry -> entry.value.sortedBy { it.name.lowercase() } }
        )
    }

    fun setPlayedMusic(music: Music) {
        currentMusic = music
    }

    fun setCurrentMusic(music: Music, contextList: List<Music>? = null) {
        currentMusic = music
        originalContextList = (contextList ?: musicList).sortedBy { it.name.lowercase() }
        shuffledContextList = originalContextList.shuffled()
    }

    fun enableShuffle(enabled: Boolean) {
        isShuffled = enabled
    }

    fun isShuffleEnabled(): Boolean = isShuffled

    fun getMusicList(): List<Music> = musicList
    fun getArtistSongs(artist: String): List<Music> = artistMap[artist] ?: emptyList()
    fun getAlbumSongs(album: String): List<Music> = albumMap[album] ?: emptyList()
    fun getCurrentMusic(): Music? = currentMusic

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

    fun getMusicContext(): List<Music> = getActiveList()
}

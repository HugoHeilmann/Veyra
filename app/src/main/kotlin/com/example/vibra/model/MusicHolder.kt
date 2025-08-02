package com.example.vibra.model

object MusicHolder {
    private var currentMusic: Music? = null
    private var musicList: List<Music> = emptyList()
    private var currentContextList: List<Music>? = null
    private val artistMap = mutableMapOf<String, List<Music>>()
    private val albumMap = mutableMapOf<String, List<Music>>()

    fun setMusicList(list: List<Music>) {
        musicList = list.sortedBy { it.name.lowercase() }

        // Artists map
        artistMap.clear()
        artistMap.putAll(
            list
                .filter { !it.artist.isNullOrBlank() }
                .groupBy {
                    it.artist?.split(Regex("(?i) ft\\."))?.get(0)?.trim() ?: "Unknown"
                }
                .mapValues { entry ->
                    entry.value.sortedBy { it.name.lowercase() }
                }
        )

        // Albums map
        albumMap.clear()
        albumMap.putAll(
            list
                .filter { !it.album.isNullOrBlank() }
                .groupBy { it.album ?: "Unfinished" }
                .mapValues { entry ->
                    entry.value.sortedBy { it.name.lowercase() }
                }
        )
    }

    fun getMusicList(): List<Music> = musicList

    fun getMusicContext(): List<Music>? = currentContextList

    fun getArtistSongs(artist: String): List<Music> = artistMap[artist] ?: emptyList()

    fun getAlbumSongs(album: String): List<Music> = albumMap[album] ?: emptyList()

    fun getCurrentMusic(): Music? = currentMusic

    fun setCurrentMusic(music: Music, contextList: List<Music>? = null) {
        currentMusic = music
        currentContextList = contextList?.sortedBy { it.name.lowercase() }
    }

    fun getNext(): Music? {
        val list = currentContextList ?: musicList
        val index = list.indexOf(currentMusic)
        return if (list.isNotEmpty()) {
            list[(index + 1) % list.size]
        } else null
    }

    fun getPrevious(): Music? {
        val list = currentContextList ?: musicList
        val index = list.indexOf(currentMusic)
        return if (list.isNotEmpty()) {
            list[(index - 1 + list.size) % list.size]
        } else null
    }

    fun clearContext() {
        currentContextList = null
    }
}
package com.example.vibra.model

object MusicHolder {
    private var currentMusic: Music? = null
    private var fullMusicList: List<Music> = emptyList()
    private val artistMap = mutableMapOf<String, List<Music>>()
    private val albumMap = mutableMapOf<String, List<Music>>()

    fun setMusicList(list: List<Music>) {
        fullMusicList = list

        // Artists map
        artistMap.clear()
        artistMap.putAll(
            list.filter { !it.artist.isNullOrBlank() }
                .groupBy {
                    it.artist?.split(Regex("(?i) ft\\."))?.get(0)?.trim() ?: "Unknown"
                }
        )

        // Albums map
        albumMap.clear()
        albumMap.putAll(
            list.filter { !it.album.isNullOrBlank() }
                .groupBy { it.album ?: "Unfinished" }
        )
    }

    fun getMusicList(): List<Music> = fullMusicList

    fun getArtistMap(): Map<String, List<Music>> = artistMap

    fun getArtistSongs(artist: String): List<Music> = artistMap[artist] ?: emptyList()

    fun getAlbumMap(): Map<String, List<Music>> = albumMap

    fun getAlbumSongs(album: String): List<Music> = albumMap[album] ?: emptyList()

    fun getCurrentMusic(): Music? = currentMusic

    fun setCurrentMusic(music: Music, list: List<Music>) {
        currentMusic = music
        fullMusicList = list
    }

    fun getNext(): Music? {
        val index = fullMusicList.indexOf(currentMusic)
        return if (fullMusicList.isNotEmpty()) {
            fullMusicList[(index + 1) % fullMusicList.size]
        } else null
    }

    fun getPrevious(): Music? {
        val index = fullMusicList.indexOf(currentMusic)
        return if (fullMusicList.isNotEmpty()) {
            fullMusicList[(index - 1 + fullMusicList.size) % fullMusicList.size]
        } else null
    }
}
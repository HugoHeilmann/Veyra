package com.example.veyra.model

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.veyra.model.metadata.PlaylistManager

import com.example.veyra.service.NotificationService

object MusicHolder {
    private var currentMusic: Music? = null
    private var musicList: List<Music> = emptyList()

    private var originalContextList: List<Music> = emptyList()
    private var shuffledContextList: List<Music> = emptyList()

    private val artistMap = mutableMapOf<String, List<Music>>()
    private val albumMap = mutableMapOf<String, List<Music>>()
    private val playlistMap = mutableMapOf<String, List<Music>>()

    var isShuffled by mutableStateOf(false)
        private set

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

        playlistMap.clear()
    }

    fun setPlayedMusic(context: Context, music: Music) {
        currentMusic = music
        MusicPlayerManager.playMusic(context, music)

        // update notification
        val intent = Intent(context, NotificationService::class.java).apply {
            putExtra("NOTIF_TITLE", music.name)
            putExtra("NOTIF_TEXT", "${music.artist} - ${music.album}")
        }
        context.startService(intent)
    }

    fun setCurrentMusic(context: Context, music: Music, contextList: List<Music>? = null) {
        currentMusic = music
        originalContextList = (contextList ?: musicList).sortedBy { it.name.lowercase() }
        shuffledContextList = originalContextList.shuffled()

        // Launch notification
        val intent = Intent(context, NotificationService::class.java).apply {
            putExtra("NOTIF_TITLE", music.name)
            putExtra("NOTIF_TEXT", "${music.artist} - ${music.album}")
        }
        context.startForegroundService(intent)
    }

    fun enableShuffle(enabled: Boolean) {
        isShuffled = enabled
    }

    fun getMusicList(): List<Music> = musicList
    fun getArtistSongs(artist: String): List<Music> = artistMap[artist] ?: emptyList()
    fun getAlbumSongs(album: String): List<Music> = albumMap[album] ?: emptyList()
    fun getPlaylistSongs(playlist: String): List<Music> = playlistMap[playlist] ?: emptyList()
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

    fun reset() {
        currentMusic = null
        musicList = emptyList()
        originalContextList = emptyList()
        shuffledContextList = emptyList()
        artistMap.clear()
        albumMap.clear()
        isShuffled = false
    }
}
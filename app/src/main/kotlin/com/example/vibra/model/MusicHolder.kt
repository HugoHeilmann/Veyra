package com.example.vibra.model

object MusicHolder {
    var currentMusic: Music? = null
    var musicList: List<Music> = emptyList()

    fun setCurrentMusic(music: Music, list: List<Music>) {
        currentMusic = music
        musicList = list
    }

    fun getNext(): Music? {
        val index = musicList.indexOf(currentMusic)
        return if (musicList.isNotEmpty()) {
            musicList[(index + 1) % musicList.size]
        } else null
    }

    fun getPrevious(): Music? {
        val index = musicList.indexOf(currentMusic)
        return if (musicList.isNotEmpty()) {
            musicList[(index - 1 + musicList.size) % musicList.size]
        } else null
    }
}
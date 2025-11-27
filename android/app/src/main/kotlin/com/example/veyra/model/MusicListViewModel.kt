package com.example.veyra.model

import androidx.lifecycle.ViewModel

class MusicListViewModel : ViewModel() {
    var songsScroll: Pair<Int, Int> = 0 to 0
    var artistsScroll: Pair<Int, Int> = 0 to 0
    var albumsScroll: Pair<Int, Int> = 0 to 0
}

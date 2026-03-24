package com.example.veyra.model

import com.example.veyra.R

data class Music(
    var name: String,
    var artist: String? = null,
    var album: String? = null,
    var image: Int = R.drawable.default_album_cover,
    var uri: String,
    var coverPath: String? = null
)

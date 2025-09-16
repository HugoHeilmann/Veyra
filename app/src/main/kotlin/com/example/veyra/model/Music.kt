package com.example.veyra.model

import com.example.veyra.R

data class Music(
    val name: String,
    val artist: String? = null,
    val album: String? = null,
    val image: Int = R.drawable.default_album_cover,
    val uri: String,
    var coverPath: String? = null
)

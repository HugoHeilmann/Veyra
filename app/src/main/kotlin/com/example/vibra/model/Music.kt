package com.example.vibra.model

import com.example.vibra.R

data class Music(
    val name: String,
    val artist: String? = null,
    val album: String? = null,
    val image: Int = R.drawable.default_album_cover
)

fun createMusic(
    name: String,
    artist: String? = null,
    album: String? = null,
    image: Int? = null
): Music {
    val defaultImageId = R.drawable.default_album_cover
    return Music(
        name = name,
        artist = artist,
        album = album,
        image = image ?: defaultImageId
    )
}

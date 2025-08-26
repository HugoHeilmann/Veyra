package com.example.veyra.model

import com.example.veyra.R

data class Music(
    val name: String,
    val artist: String? = null,
    val album: String? = null,
    val image: Int = R.drawable.default_album_cover,
    val uri: String,
    val coverPath: String? = null
)

fun createMusic(
    name: String,
    artist: String? = null,
    album: String? = null,
    image: Int? = null,
    uri: String,
    coverPath: String? = null
): Music {
    val defaultImageId = R.drawable.default_album_cover
    return Music(
        name = name,
        artist = artist,
        album = album,
        image = image ?: defaultImageId,
        uri = uri,
        coverPath = coverPath
    )
}

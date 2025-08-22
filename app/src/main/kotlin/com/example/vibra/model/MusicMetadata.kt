package com.example.vibra.model

import com.example.vibra.R

data class MusicMetadata(
    val fileName: String,                                // nom du fichier brut
    var title: String,                                   // titre de la chanson (modifiable en local)
    var artist: String,                                  // artiste (modifiable en local)
    var album: String,                                   // album (modifiable en local)
    val filePath: String,                                // chemin complet pour le MediaPlayer
    var playlists: MutableList<String> = mutableListOf() // playlists associées
)

fun MusicMetadata.toMusic(): Music {
    return Music(
        name = this.title,
        artist = this.artist,
        album = this.album,
        image = R.drawable.default_album_cover,
        uri = this.filePath
    )
}

package com.example.veyra.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.veyra.model.Music
import com.example.veyra.model.data.MusicHolder

@Composable
fun RandomPlay(
    navController: NavHostController,
    artist: String,
    album: String,
    playlist: String
) {
    val context = LocalContext.current

    val songs: List<Music> = when {
        artist.isNotBlank() -> MusicHolder.getArtistSongs(artist)
        album.isNotBlank() -> MusicHolder.getAlbumSongs(album)
        playlist.isNotBlank() -> MusicHolder.getPlaylistSongs(playlist)
        else -> MusicHolder.getMusicList() // fallback
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Lecture aléatoire",
            style = MaterialTheme.typography.titleMedium
        )

        Button(
            onClick = {
                if (songs.isNotEmpty()) {
                    val track = songs.random()
                    MusicHolder.setCurrentMusic(context, track, songs)
                    navController.navigate("player")
                }
            },
            enabled = songs.isNotEmpty()
        ) {
            Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = "Play")
        }
    }
}
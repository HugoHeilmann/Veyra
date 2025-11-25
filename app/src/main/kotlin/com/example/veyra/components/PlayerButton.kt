package com.example.veyra.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
fun PlayerButton(
    navController: NavHostController,
    artist: String? = "",
    album: String? = "",
    playlist: String? = "",
    list: List<Music> = emptyList(),
    random: Boolean = true,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current

    val songs: List<Music> = when {
        !artist.isNullOrBlank() -> MusicHolder.getArtistSongs(artist)
        !album.isNullOrBlank() -> MusicHolder.getAlbumSongs(album)
        !playlist.isNullOrBlank() -> MusicHolder.getPlaylistSongs(playlist)
        list.isNotEmpty() -> list
        else -> MusicHolder.getMusicList()
    }

    val useProvidedOrder = list.isNotEmpty()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = if (random) {
                "Lecture aléatoire"
            } else {
                "Lecture ordonnée"
            },
            style = MaterialTheme.typography.titleMedium
        )

        Button(
            onClick = {
                onClick()

                if (songs.isNotEmpty()) {
                    val track = if (random) {
                        songs.random()
                    } else {
                        songs[0]
                    }
                    MusicHolder.isShuffled = random
                    MusicHolder.setCurrentMusic(
                        context = context,
                        music = track,
                        contextList = songs,
                        keepOrder = useProvidedOrder
                    )
                    navController.navigate("player")
                }
            },
            enabled = songs.isNotEmpty()
        ) {
            Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = "Play")
        }
    }
}
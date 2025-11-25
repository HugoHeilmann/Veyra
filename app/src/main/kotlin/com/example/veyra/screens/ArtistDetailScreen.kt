package com.example.veyra.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.veyra.components.MusicRow
import com.example.veyra.components.PlayerButton
import com.example.veyra.components.TopBar
import com.example.veyra.model.data.MusicHolder
import com.example.veyra.model.data.QueueManager
import com.example.veyra.model.metadata.MetadataManager
import com.example.veyra.model.metadata.toMusic
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(artistName: String, navController: NavHostController) {
    val context = LocalContext.current
    val songs = MusicHolder.getArtistSongs(artistName)

    Scaffold(
        topBar = {
            TopBar(artistName, "music_list?selectedTab=Artistes", navController)
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            item {
                PlayerButton(
                    navController = navController,
                    artist = artistName
                )
            }

            items(songs) { song ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    val musicReference = MetadataManager.getByPath(context, song.uri)?.toMusic() ?: song

                    MusicRow(
                        music = musicReference,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 16.dp),
                        onClick = {
                            MusicHolder.isShuffled = true
                            MusicHolder.setCurrentMusic(context, song, songs)
                            navController.navigate("player")
                        },
                        onEditClick = { _ ->
                            val encodedUri = URLEncoder.encode(musicReference.uri, StandardCharsets.UTF_8.toString())
                            navController.navigate("editMusic/${encodedUri}")
                        },
                        onAddClick = { _ ->
                            QueueManager.addToEnd(song)
                        }
                    )
                }
            }
        }
    }
}
package com.example.veyra.screens

import androidx.compose.foundation.clickable
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
import com.example.veyra.components.BlandMusicRow
import com.example.veyra.components.RandomPlay
import com.example.veyra.components.TopBar
import com.example.veyra.model.MusicHolder

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
                RandomPlay(
                    navController = navController,
                    artist = artistName,
                    album = "",
                    playlist = ""
                )
            }

            items(songs) { song ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            MusicHolder.setCurrentMusic(context, song, songs)
                            navController.navigate("player")
                        }
                        .padding(16.dp)
                ) {
                    BlandMusicRow(
                        song.name,
                        song.album ?: "Unknown Album"
                    )
                }
            }
        }
    }
}
package com.example.veyra.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.veyra.components.BlandMusicRow
import com.example.veyra.components.RandomPlay
import com.example.veyra.components.TopBar
import com.example.veyra.model.data.MusicHolder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(albumName: String, navController: NavHostController) {
    val context = LocalContext.current
    val songs = MusicHolder.getAlbumSongs(albumName)

    Scaffold(
        topBar = {
            TopBar(albumName, "music_list?selectedTab=Albums", navController)
        }
    ){ innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            item {
                RandomPlay(
                    navController = navController,
                    artist = "",
                    album = albumName,
                    playlist = ""
                )
            }

            items(songs) { song ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            MusicHolder.setCurrentMusic(context,song, songs)
                            navController.navigate("player")
                        }
                        .padding(16.dp)
                ) {
                    BlandMusicRow(
                        song.name,
                        song.artist ?: "Unknown Artist"
                    )
                }
            }
        }
    }
}
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
import com.example.veyra.components.RandomPlay
import com.example.veyra.model.MusicHolder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(artistName: String, navController: NavHostController) {
    val context = LocalContext.current
    val songs = MusicHolder.getArtistSongs(artistName)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = artistName)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.navigate("music_list?selectedTab=Artistes") {
                            popUpTo("music_list") { inclusive = true }
                            launchSingleTop = true
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                }
            )
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
                    Text(
                        text = song.name,
                        color = Color.White
                    )
                    Text(
                        text = song.album ?: "Unknown Album",
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
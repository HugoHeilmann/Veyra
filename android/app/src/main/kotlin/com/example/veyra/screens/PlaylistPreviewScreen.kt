package com.example.veyra.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.veyra.components.BlandMusicRow
import com.example.veyra.components.TopBar
import com.example.veyra.model.Music
import com.example.veyra.model.data.MusicHolder
import com.example.veyra.model.metadata.PlaylistManager
import com.example.veyra.model.metadata.PlaylistMetadata

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistPreviewScreen(
    playlistName: String,
    navController: NavHostController
) {
    val context = LocalContext.current

    // Liste complète de chansons
    val allSongs: List<Music> = remember { MusicHolder.getMusicList() }

    val metadata: PlaylistMetadata? = PlaylistManager.getByName(context, playlistName)

    val playlistSongs: List<Music> = if (metadata == null) {
        allSongs
    } else {
        allSongs.filter { music -> metadata.musicFiles.contains(music.uri) }
    }


    // --- 🔎 Recherche ---
    var searchText by remember { mutableStateOf("") }
    // Liste filtrée (nom, artiste, album)
    val filteredSongs by remember(playlistSongs, searchText) {
        derivedStateOf {
            if (searchText.isBlank()) playlistSongs
            else {
                val q = searchText.trim().lowercase()
                playlistSongs.filter { m ->
                    m.name.lowercase().contains(q) ||
                            (m.artist?.lowercase()?.contains(q) == true) ||
                            (m.album?.lowercase()?.contains(q) == true)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopBar(playlistName, "playlists", navController)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Spacer(Modifier.height(12.dp))

            // --- 🔎 Barre de recherche ---
            BasicTextField(
                value = searchText,
                onValueChange = { searchText = it },
                singleLine = true,
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .background(MaterialTheme.colorScheme.inverseOnSurface, shape = MaterialTheme.shapes.small)
                    .padding(12.dp),
                decorationBox = { innerTextField ->
                    if (searchText.isEmpty()) {
                        Text("Rechercher...", color = Color.Gray)
                    }
                    innerTextField()
                }
            )

            Spacer(Modifier.height(12.dp))

            // --- Liste des morceaux filtrés ---
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = filteredSongs,
                    key = { it.uri }
                ) { music ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                MusicHolder.setCurrentMusic(context, music, playlistSongs)
                                navController.navigate("player")
                            }
                    ) {
                        BlandMusicRow(
                            music.name,
                            (music.artist ?: "Unknown Artist") + " - " + (music.album ?: "Unknown Album")
                        )
                    }
                }

                if (filteredSongs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (searchText.isBlank())
                                    "Aucune musique trouvée"
                                else
                                    "Aucun résultat pour “$searchText”"
                            )
                        }
                    }
                }
            }
        }
    }
}

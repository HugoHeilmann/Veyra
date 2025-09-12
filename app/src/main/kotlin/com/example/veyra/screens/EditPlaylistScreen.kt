package com.example.veyra.screens

import android.R.attr.title
import android.view.Surface
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.veyra.model.Music
import com.example.veyra.model.MusicHolder
import com.example.veyra.model.metadata.PlaylistManager
import com.example.veyra.model.metadata.PlaylistMetadata

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPlaylistScreen(
    playlistName: String,
    navController: NavHostController
) {
    val context = LocalContext.current

    val allSongs: List<Music> = remember { MusicHolder.getMusicList() }

    val initialSelected: List<String> = remember {
        PlaylistManager.readAll(context)
            .find { it.name == playlistName }
            ?.musicFiles
            ?: emptyList()
    }

    val selected = remember {
        mutableStateListOf<String>().apply { addAll(initialSelected) }
    }

    fun toggleSelection(filePath: String) {
        if (selected.contains(filePath)) selected.remove(filePath) else selected.add(filePath)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = playlistName, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { navController.popBackStack() }
                    ) {
                        Text("Annuler")
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            // Écrire les sélections dans les métadonnées
                            val all = PlaylistManager.readAll(context).toMutableList()
                            val idx = all.indexOfFirst { it.name == playlistName }
                            if (idx >= 0) {
                                val current = all[idx]
                                all[idx] = current.copy(musicFiles = selected.toMutableList())
                            } else {
                                // Si la playlist n’existait pas encore, on la crée
                                all.add(
                                    PlaylistMetadata(
                                        name = playlistName,
                                        musicFiles = selected.toMutableList()
                                    )
                                )
                            }
                            PlaylistManager.writeAll(context, all)
                            navController.popBackStack()
                        }
                    ) {
                        Text("Confirmer")
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = allSongs,
                key = { it.uri }
            ) { music ->
                val isSelected = selected.contains(music.uri)
                val bg = if (isSelected)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else
                    MaterialTheme.colorScheme.surface

                ListItem(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            width = 2.dp,
                            color = bg,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { toggleSelection(music.uri) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    headlineContent = {
                        Text(
                            text = music.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    supportingContent = {
                        val artist = music.artist ?: "Artiste inconnu"
                        Text(artist, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    trailingContent = {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { toggleSelection(music.uri) }
                        )
                    }
                )
            }
            if (allSongs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Aucune musique trouvée")
                    }
                }
            }
        }
    }
}
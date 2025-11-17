package com.example.veyra.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.veyra.components.TopBar
import com.example.veyra.model.data.MusicHolder
import com.example.veyra.model.metadata.MetadataManager
import com.example.veyra.model.metadata.MusicMetadata
import org.schabi.newpipe.extractor.timeago.patterns.fil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditArtistOrAlbumScreen(
    name: String,
    isArtistCreated: Boolean,
    navController: NavHostController
) {
    val context = LocalContext.current

    val allMusics = MusicHolder.getMusicList()

    Log.d("TEST", "all " + allMusics.size)

    val musics = if (isArtistCreated) {
        MusicHolder.getMusicList().filter {
            it.artist.isNullOrBlank() || it.artist == "Unknown Artist" || it.artist == "Unknown"
        }
    } else {
        MusicHolder.getMusicList().filter {
            it.album.isNullOrBlank() || it.album == "Unknown Album"
        }
    }

    Log.d("TEST", "filtered " + musics.size)

    // Sélection
    val selected = remember { mutableStateListOf<String>() }
    fun toggleSelection(uri: String) {
        if (selected.contains(uri)) selected.remove(uri) else selected.add(uri)
    }

    Scaffold(
        topBar = { TopBar(name, "music_list", navController) },
        bottomBar = {
            Surface(tonalElevation = 3.dp, color = Color.Transparent) {
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
                    ) { Text("Annuler") }

                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (selected.isNotEmpty()) {
                                selected.forEach { uri ->
                                    val music = musics.find { it.uri == uri }
                                    if (music != null) {
                                        val updated = if (isArtistCreated)
                                            music.copy(artist = name)
                                        else
                                            music.copy(album = name)

                                        MetadataManager.updateMetadata(
                                            context,
                                            updated.uri,
                                            updated.name,
                                            updated.artist ?: "Unknown Artist",
                                            updated.album ?: "Unknown Album",
                                            updated.coverPath
                                        )

                                        MusicHolder.updateMusic(
                                            updated.uri,
                                            updated.name,
                                            updated.artist ?: "Unknown Artist",
                                            updated.album ?: "Unknown Album",
                                            updated.coverPath
                                        )

                                        MusicHolder.refreshMapsForMusic(updated)
                                    }
                                }
                            }
                            navController.popBackStack()
                        }
                    ) { Text("Appliquer (${selected.size})") }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(musics, key = { it.uri }) { music ->
                    val isSelected = selected.contains(music.uri)
                    val borderColor = if (isSelected)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else
                        MaterialTheme.colorScheme.surface

                    ListItem(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { toggleSelection(music.uri) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        headlineContent = {
                            Text(music.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        supportingContent = {
                            val artist = music.artist ?: "Artiste inconnu"
                            val album = music.album ?: "Album inconnu"
                            Text("$artist • $album", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        trailingContent = {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { toggleSelection(music.uri) }
                            )
                        }
                    )
                }

                if (musics.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Aucune musique à attribuer")
                        }
                    }
                }
            }
        }
    }
}

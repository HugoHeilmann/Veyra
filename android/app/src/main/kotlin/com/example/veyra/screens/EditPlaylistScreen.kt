package com.example.veyra.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.veyra.components.TopBar
import com.example.veyra.components.form.SearchField
import com.example.veyra.model.Music
import com.example.veyra.model.data.MusicHolder
import com.example.veyra.model.metadata.PlaylistManager
import com.example.veyra.model.metadata.PlaylistMetadata

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPlaylistScreen(
    playlistName: String,
    navController: NavHostController
) {
    val context = LocalContext.current

    // Liste complète de chansons
    val allSongs: List<Music> = remember { MusicHolder.getMusicList() }

    // Sélections initiales de la playlist
    val initialSelected: List<String> = remember {
        PlaylistManager.readAll(context)
            .find { it.name == playlistName }
            ?.musicFiles
            ?: emptyList()
    }

    // État de sélection courant
    val selected = remember {
        mutableStateListOf<String>().apply { addAll(initialSelected) }
    }

    var showSelectedOnly by remember { mutableStateOf(false) }

    // --- 🔎 Recherche ---
    var searchText by remember { mutableStateOf("") }

    val filteredSongs by remember(allSongs, searchText, selected, showSelectedOnly) {
        derivedStateOf {
            val base = if (showSelectedOnly) {
                allSongs.filter { music -> selected.contains(music.uri) }
            } else {
                allSongs
            }

            if (searchText.isBlank()) base
            else {
                val q = searchText.trim().lowercase()
                base.filter { music ->
                    music.name.lowercase().contains(q) ||
                            (music.artist?.lowercase()?.contains(q) == true) ||
                            (music.album?.lowercase()?.contains(q) == true)
                }
            }
        }
    }

    var showDeleteDialog by remember { mutableStateOf(false) }

    fun toggleSelection(filePath: String) {
        if (selected.contains(filePath)) selected.remove(filePath) else selected.add(filePath)
    }

    Scaffold(
        topBar = {
            TopBar(playlistName, navController)
        },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                color = Color.Transparent
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    // Petite ligne récap en haut de la bottom bar
                    Text(
                        text = if (selected.isEmpty())
                            "Aucune musique sélectionnée"
                        else if (selected.size == 1)
                            "${selected.size} morceau dans la playlist"
                        else
                            "${selected.size} morceaux dans la playlist",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (selected.isEmpty()) {
                                    val all = PlaylistManager.readAll(context).toMutableList()
                                    val idx = all.indexOfFirst { it.name == playlistName }
                                    if (idx >= 0) all.removeAt(idx)
                                    PlaylistManager.writeAll(context, all)
                                }
                                navController.popBackStack()
                            }
                        ) {
                            Text("Annuler")
                        }

                        Button(
                            modifier = Modifier.weight(1f),
                            colors = if (selected.isEmpty()) {
                                ButtonDefaults.buttonColors(
                                    containerColor = Color.Red,
                                    contentColor = Color.White
                                )
                            } else {
                                ButtonDefaults.buttonColors()
                            },
                            onClick = {
                                if (selected.isEmpty()) {
                                    showDeleteDialog = true
                                } else {
                                    val all = PlaylistManager.readAll(context).toMutableList()
                                    val idx = all.indexOfFirst { it.name == playlistName }

                                    if (idx >= 0) {
                                        val current = all[idx]
                                        all[idx] = current.copy(musicFiles = selected.toMutableList())
                                    } else {
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
                            }
                        ) {
                            Text(if (selected.isEmpty()) "Supprimer" else "Confirmer")
                        }
                    }

                    if (showDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text("Supprimer la playlist ?") },
                            text = {
                                Text(
                                    "Cette action est irréversible. La playlist \"$playlistName\" sera définitivement supprimée."
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        val all = PlaylistManager.readAll(context).toMutableList()
                                        val idx = all.indexOfFirst { it.name == playlistName }
                                        if (idx >= 0) all.removeAt(idx)
                                        PlaylistManager.writeAll(context, all)
                                        showDeleteDialog = false
                                        navController.popBackStack()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Red,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text("Supprimer")
                                }
                            },
                            dismissButton = {
                                OutlinedButton(onClick = { showDeleteDialog = false }) {
                                    Text("Annuler")
                                }
                            },
                            containerColor = Color(0xFF1A1A1A)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // --- 🔎 Barre de recherche en "pill" ---
            Box(
                modifier = Modifier
                    .padding(12.dp)
                    .padding(bottom = 0.dp)
            ) {
                SearchField(
                    onValueChange = {
                        searchText = it
                    }
                )
            }

            // Ligne filtre + compteur
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Chip "seulement sélection"
                FilterChip(
                    selected = showSelectedOnly,
                    onClick = { showSelectedOnly = !showSelectedOnly },
                    label = {
                        Text("Seulement la sélection (${selected.size})")
                    },
                    leadingIcon = if (showSelectedOnly) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null
                            )
                        }
                    } else null,
                    shape = RoundedCornerShape(999.dp)
                )

                Text(
                    text = "${filteredSongs.size} / ${allSongs.size} morceaux",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(12.dp))

            // --- Liste des morceaux filtrés ---
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    bottom = 80.dp // pour ne pas être caché par la bottom bar
                )
            ) {
                items(
                    items = filteredSongs,
                    key = { it.uri }
                ) { music ->
                    val isSelected = selected.contains(music.uri)

                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .then(
                                if (isSelected) Modifier.border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(12.dp)
                                ) else Modifier
                            )
                            .clickable { toggleSelection(music.uri) },
                        headlineContent = {
                            Text(
                                text = music.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        supportingContent = {
                            val artist = music.artist ?: "Artiste inconnu"
                            val album = music.album
                            val line = if (!album.isNullOrBlank()) "$artist • $album" else artist
                            Text(
                                text = line,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingContent = {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { toggleSelection(music.uri) }
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = Color(0xFF1A1A1A)
                        )
                    )
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
                                text = if (searchText.isBlank())
                                    "Aucune musique trouvée"
                                else
                                    "Aucun résultat pour “$searchText”",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

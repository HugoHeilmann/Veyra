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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
    // Liste filtrée (nom, artiste, album)
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // --- 🔎 Barre de recherche ---
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Rechercher une musique") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchText.isNotBlank()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Effacer",
                            modifier = Modifier
                                .clickable { searchText = "" }
                                .padding(4.dp)
                        )
                    }
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showSelectedOnly = !showSelectedOnly }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = showSelectedOnly,
                        onCheckedChange = { showSelectedOnly = it }
                    )
                    Text("Afficher seulement la sélection (${selected.size})")
                }
            }

            Spacer(Modifier.height(12.dp))

            // --- Liste des morceaux filtrés ---
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = filteredSongs,
                    key = { it.uri } // clé stable
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
                            val album = music.album
                            val line = if (!album.isNullOrBlank()) "$artist • $album" else artist
                            Text(line, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        trailingContent = {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { toggleSelection(music.uri) }
                            )
                        }
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

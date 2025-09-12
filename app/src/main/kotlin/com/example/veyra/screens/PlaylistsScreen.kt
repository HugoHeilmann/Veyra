package com.example.veyra.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.example.veyra.components.Playlist
import com.example.veyra.components.PlaylistItem
import com.example.veyra.model.metadata.PlaylistManager
import com.example.veyra.model.metadata.PlaylistMetadata

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(navController: NavController) {
    var showDialog by remember { mutableStateOf(false) }
    var playlistName by remember { mutableStateOf("") }

    var playlists by remember { mutableStateOf(mutableListOf<Playlist>()) }

    LaunchedEffect(Unit) {
        val storedPlaylists = PlaylistManager.readAll(context = navController.context)
        playlists = storedPlaylists.map { Playlist(it.name, it.musicFiles.size) }.toMutableList()
    }

    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(text = "Playlist")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(playlists) { playlist ->
                        PlaylistItem(
                            playlist = playlist,
                            onEditClick = {
                                val encoded = playlist.name.toUri()
                                navController.navigate("edit_playlist/$encoded")
                            },
                            onDeleteClick = {
                                playlistToDelete = playlist
                                showDeleteDialog = true
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = { showDialog = true }) {
                    Text(text = "Créer une nouvelle playlist")
                }
            }

            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text(text = "Nouvelle Playlist") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = playlistName,
                                onValueChange = { playlistName = it },
                                label = { Text("Nom de la playlist") }
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (playlistName.isNotBlank()) {
                                playlists = (playlists + Playlist(playlistName, 0)).toMutableList()

                                PlaylistManager.addIfNotExists(
                                    navController.context,
                                    PlaylistMetadata(name = playlistName)
                                )
                            }
                            showDialog = false
                            playlistName = ""
                        }) {
                            Text("Enregistrer")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showDialog = false
                                playlistName = ""
                            }
                        ) {
                            Text(
                                "Annuler",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    containerColor = Color(0xFF2C2C2C)
                )
            }

            if (showDeleteDialog && playlistToDelete != null) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Supprimer la playlist ?") },
                    text = { Text("Voulez-vous vraiment supprimer \"${playlistToDelete!!.name}\" ?") },
                    confirmButton = {
                        Button(
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red,
                                contentColor = Color.White
                            ),
                            onClick = {
                                playlists = playlists.filter { it != playlistToDelete }.toMutableList()
                                PlaylistManager.remove(context = navController.context, playlistToDelete!!.name)
                                playlistToDelete = null
                                showDeleteDialog = false
                            })
                        {
                            Text("Confirmer")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            playlistToDelete = null
                            showDeleteDialog = false
                        }) {
                            Text("Annuler")
                        }
                    },
                    containerColor = Color(0xFF2C2C2C)
                )
            }
        }
    }
}

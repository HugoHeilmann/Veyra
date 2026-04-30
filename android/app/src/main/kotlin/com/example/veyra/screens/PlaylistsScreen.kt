package com.example.veyra.screens

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.example.veyra.components.Playlist
import com.example.veyra.components.PlaylistItem
import com.example.veyra.model.metadata.PlaylistManager
import com.example.veyra.model.metadata.PlaylistMetadata
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.res.painterResource
import com.example.veyra.model.data.MusicHolder
import com.example.veyra.R
import com.example.veyra.components.animations.AnimatedPlaylistIcon

@SuppressLint("MutableCollectionMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(navController: NavController) {
    val context = LocalContext.current

    var showDialog by remember { mutableStateOf(false) }
    var playlistName by remember { mutableStateOf("") }

    var playlists by remember { mutableStateOf(mutableListOf<Playlist>()) }

    LaunchedEffect(Unit) {
        val storedPlaylists = PlaylistManager.readAll(context = navController.context)
        playlists = storedPlaylists
            .map { Playlist(it.name, it.musicFiles.size) }
            .toMutableList()
    }

    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val nameFocusRequester = remember { FocusRequester() }
    LaunchedEffect(showDialog) {
        if (showDialog) {
            nameFocusRequester.requestFocus()
        }
    }

    fun tryCreatePlaylist(): Boolean {
        val name = playlistName.trim()
        if (name.isEmpty()) {
            Toast.makeText(context, "Le nom ne peut pas être vide.", Toast.LENGTH_SHORT).show()
            return false
        }

        val alreadyExists = playlists.any { it.name.equals(name, ignoreCase = true) }
        if (alreadyExists) {
            Toast.makeText(context, "Une playlist avec ce nom existe déjà.", Toast.LENGTH_SHORT).show()
            return false
        }

        playlists = (playlists + Playlist(name, 0)).toMutableList()
        PlaylistManager.addIfNotExists(
            navController.context,
            PlaylistMetadata(name = name)
        )
        return true
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedPlaylistIcon(direction = -1f)

                        Spacer(Modifier.width(8.dp))

                        Text(
                            text = "Playlists",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(Modifier.width(8.dp))

                        AnimatedPlaylistIcon(direction = 1f)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Nouvelle playlist"
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                if (playlists.isEmpty()) {
                    // État vide sympa
                    Column(
                        modifier = Modifier
                            .fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_playlist), // à adapter
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Aucune playlist pour le moment",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Crée ta première playlist pour commencer.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(playlists) { playlist ->
                            PlaylistItem(
                                playlist = playlist,
                                onClick = {
                                    navController.navigate("playlist_preview/${playlist.name}")
                                },
                                onPlayClick = {
                                    MusicHolder.buildPlaylistMap(context, MusicHolder.getMusicList())
                                    val songs = MusicHolder.getPlaylistSongs(playlist.name)

                                    if (songs.isNotEmpty()) {
                                        MusicHolder.setCurrentMusic(context, songs.random(), "Playlist : ${playlist.name}", songs)
                                        navController.navigate("player")
                                    } else {
                                        Toast.makeText(context, "La playlist est vide", Toast.LENGTH_SHORT).show()
                                    }
                                },
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
                }
            }

            // Dialog création
            if (showDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showDialog = false
                        playlistName = ""
                    },
                    title = { Text(text = "Nouvelle playlist") },
                    text = {
                        OutlinedTextField(
                            value = playlistName,
                            onValueChange = { playlistName = it },
                            label = { Text("Nom de la playlist") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(nameFocusRequester),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (tryCreatePlaylist()) {
                                        val encoded = playlistName.toUri()
                                        showDialog = false
                                        navController.navigate("edit_playlist/$encoded")
                                        playlistName = ""
                                    }
                                }
                            )
                        )
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (tryCreatePlaylist()) {
                                val encoded = playlistName.toUri()
                                showDialog = false
                                navController.navigate("edit_playlist/$encoded")
                                playlistName = ""
                            }
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
                            Text("Annuler", color = Color.Red)
                        }
                    },
                    containerColor = Color(0xFF2C2C2C)
                )
            }

            // Dialog suppression
            if (showDeleteDialog && playlistToDelete != null) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Supprimer la playlist ?") },
                    text = {
                        Text("Voulez-vous vraiment supprimer \"${playlistToDelete!!.name}\" ?")
                    },
                    confirmButton = {
                        TextButton(
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color.Red
                            ),
                            onClick = {
                                playlists = playlists.filter { it != playlistToDelete }.toMutableList()
                                PlaylistManager.remove(
                                    context = navController.context,
                                    playlistName = playlistToDelete!!.name
                                )
                                playlistToDelete = null
                                showDeleteDialog = false
                            }
                        ) {
                            Text("Confirmer")
                        }
                    },
                    dismissButton = {
                        Button(onClick = {
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

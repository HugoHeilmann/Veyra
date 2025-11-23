package com.example.veyra.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.veyra.components.SelectorInput
import com.example.veyra.model.data.MusicHolder
import com.example.veyra.model.convert.DownloadHolder
import com.example.veyra.model.metadata.PlaylistManager
import com.example.veyra.service.DownloadService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(context: Context = LocalContext.current) {
    var url by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }
    var artist by rememberSaveable { mutableStateOf("") }
    var album by rememberSaveable { mutableStateOf("") }

    val status by DownloadHolder.status
    val progress by DownloadHolder.progress

    var restoreArtistSelector by remember { mutableStateOf<(() -> Unit)?>(null) }
    var restoreAlbumSelector by remember { mutableStateOf<(() -> Unit)?>(null) }

    val playlistName = PlaylistManager.getAllNames(context)
    var expanded by remember { mutableStateOf(false) }
    val selectedPlaylists = remember { mutableStateListOf<String>() }

    var showCancelDialog by remember { mutableStateOf(false) }

    // ✅ Vider les inputs en cas de succès
    LaunchedEffect(status) {
        if (status.startsWith("✅") || status.startsWith("OK")) {
            url = ""
            title = ""
            artist = ""
            album = ""
            restoreArtistSelector?.invoke()
            restoreAlbumSelector?.invoke()
        }
    }

    val isLoading by remember {
        derivedStateOf {
            status.startsWith("Extraction") ||
            status.startsWith("Téléchargement") ||
            status.startsWith("Conversion")
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Téléchargement",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp)
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                enabled = !isLoading,
                label = { Text("YouTube URL") },
                singleLine = true,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                enabled = !isLoading,
                label = { Text("Nom de la musique") },
                singleLine = true,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            SelectorInput(
                list = MusicHolder.getArtistList(),
                placeholder = "Artiste",
                enabled = !isLoading,
                onValueChange = { artist = it },
                onRefCreated = { restoreArtistSelector = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            SelectorInput(
                list = MusicHolder.getAlbumList(),
                placeholder = "Album",
                enabled = !isLoading,
                onValueChange = { album = it },
                onRefCreated = { restoreAlbumSelector = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box {
                OutlinedButton(
                    onClick = { expanded = !expanded },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (selectedPlaylists.isEmpty()) {
                            "Ajouter à une ou plusieurs playlists"
                        } else {
                            "Playlists : ${selectedPlaylists.joinToString(", ")}"
                        }
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .padding(horizontal = 8.dp)
                        .background(Color(0xFF2C2C2C)),
                ) {
                    if (playlistName.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Aucune playlist existante") },
                            onClick = {}
                        )
                    } else {
                        playlistName.forEach { name ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(name)
                                        Checkbox(
                                            checked = selectedPlaylists.contains(name),
                                            onCheckedChange = { checked ->
                                                if (checked) selectedPlaylists.add(name)
                                                else selectedPlaylists.remove(name)
                                            }
                                        )
                                    }
                                },
                                onClick = {
                                    val currentlySelected = selectedPlaylists.contains(name)
                                    if (currentlySelected) selectedPlaylists.remove(name)
                                    else selectedPlaylists.add(name)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (isLoading) {
                        showCancelDialog = true
                    } else {
                        DownloadHolder.status.value = "Extraction…"

                        val intent = Intent(context, DownloadService::class.java).apply {
                            putExtra("url", url)
                            putExtra("title", title)
                            putExtra("artist", artist)
                            putExtra("album", album)
                            putStringArrayListExtra("playlists", ArrayList(selectedPlaylists))
                        }
                        context.startService(intent)
                    }
                },
                enabled = url.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = if (isLoading) {
                    ButtonDefaults.buttonColors(
                        containerColor = Color.Red,
                        contentColor = Color.White
                    )
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(
                    if (isLoading) "Arrêter le téléchargement"
                    else "Télécharger MP3"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("Status: $status")

            if (isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                if (showCancelDialog) {
                    AlertDialog(
                        onDismissRequest = { showCancelDialog = false },
                        title = { Text("Arrêter le téléchargement ?") },
                        text = { Text("Es-tu sûr de vouloir annuler ce téléchargement en cours ?") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showCancelDialog = false

                                    DownloadHolder.status.value = "❌ Téléchargement annulé"
                                    DownloadHolder.progress.floatValue = 0f

                                    val cancelIntent = Intent(context, DownloadService::class.java).apply {
                                        action = DownloadService.ACTION_CANCEL
                                    }
                                    context.startService(cancelIntent)
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = Color.Red
                                )
                            ) {
                                Text("Oui, arrêter")
                            }
                        },
                        dismissButton = {
                            Button(
                                onClick = { showCancelDialog = false },
                            ) {
                                Text("Non, continuer")
                            }
                        },
                        containerColor = Color(0xFF2C2C2C)
                    )
                }
            }
        }
    }
}

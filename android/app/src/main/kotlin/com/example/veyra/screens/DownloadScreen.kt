package com.example.veyra.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import com.example.veyra.components.ArtistSelectorInput
import com.example.veyra.components.PlaylistSelector
import com.example.veyra.components.animations.AnimatedDownloadIcon
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
    val feats = remember { mutableStateListOf<String>() }
    var album by rememberSaveable { mutableStateOf("") }

    val status by DownloadHolder.status
    val progress by DownloadHolder.progress

    var restoreArtistSelector by remember { mutableStateOf<(() -> Unit)?>(null) }
    var restoreAlbumSelector by remember { mutableStateOf<(() -> Unit)?>(null) }

    val playlistName = PlaylistManager.getAllNames(context)
    var expanded by remember { mutableStateOf(false) }
    val selectedPlaylists = remember { mutableStateListOf<String>() }

    var showCancelDialog by remember { mutableStateOf(false) }
    var showVerificationDialog by remember { mutableStateOf(false) }

    // Vider les inputs en cas de succès
    LaunchedEffect(status) {
        if (status.startsWith("✅") || status.startsWith("OK")) {
            url = ""
            title = ""
            artist = ""
            album = ""
            feats.clear()
            restoreArtistSelector?.invoke()
            restoreAlbumSelector?.invoke()
            selectedPlaylists.clear()
        }
    }

    val isLoading by remember {
        derivedStateOf {
            status.startsWith("Extraction") ||
                    status.startsWith("Téléchargement") ||
                    status.startsWith("Conversion")
        }
    }

    // ✅ Construit une string "Main ft. A & B" propre, utilisée pour l’envoi au service
    val finalArtistForService by remember(artist, feats) {
        derivedStateOf {
            val main = artist.trim()
            val cleanedFeats = feats
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinctBy { it.lowercase() }

            if (main.isBlank()) {
                // si pas d'artiste principal, on n'ajoute rien
                ""
            } else if (cleanedFeats.isEmpty()) {
                main
            } else {
                "$main ft. ${cleanedFeats.joinToString(" & ")}"
            }
        }
    }

    fun extractMainArtist(rawArtist: String?): String? {
        if (rawArtist.isNullOrBlank()) return null

        return rawArtist
            .split(
                Regex(
                    "\\s+(?i)(ft\\.?|feat\\.?|featuring|with|&|,|\\(|\\[)"
                )
            )
            .first()
            .trim()
    }

    fun startDownload() {
        DownloadHolder.status.value = "Extraction…"

        val intent = Intent(context, DownloadService::class.java).apply {
            putExtra("url", url)
            putExtra("title", title)
            putExtra("artist", finalArtistForService)
            putExtra("album", album)
            putStringArrayListExtra("playlists", ArrayList(selectedPlaylists))
        }
        context.startService(intent)
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
                        AnimatedDownloadIcon()

                        Spacer(Modifier.width(8.dp))

                        Text(
                            text = "Téléchargement",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp)
                        )

                        Spacer(Modifier.width(8.dp))

                        AnimatedDownloadIcon()
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Bloc unique : URL + métadonnées + playlists
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Informations du morceau",
                        style = MaterialTheme.typography.titleMedium
                    )

                    // URL YouTube
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        enabled = !isLoading,
                        label = { Text("URL YouTube") },
                        placeholder = { Text("https://youtu.be/...") },
                        singleLine = true,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Titre
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        enabled = !isLoading,
                        label = { Text("Titre") },
                        singleLine = true,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth()
                    )

                    ArtistSelectorInput(
                        artists = MusicHolder.getArtistList(),
                        enabled = !isLoading,
                        initialArtist = artist,
                        initialFeats = feats.toList(),
                        onArtistChange = { artist = it },
                        onFeatsChange = { newFeats ->
                            feats.clear()
                            feats.addAll(newFeats)
                        },
                        onRefCreated = { restoreArtistSelector = it }
                    )

                    // Album
                    SelectorInput(
                        list = MusicHolder.getAlbumList(),
                        placeholder = "Album",
                        enabled = !isLoading,
                        onValueChange = { album = it },
                        onRefCreated = { restoreAlbumSelector = it }
                    )

                    // Playlists
                    PlaylistSelector(
                        playlists = playlistName,
                        selectedPlaylists = selectedPlaylists,
                        enabled = !isLoading && !playlistName.isEmpty(),
                        onSelectionChange = { newList ->
                            selectedPlaylists.clear()
                            selectedPlaylists.addAll(newList)
                        }
                    )
                }
            }

            // Bloc 2 : Action + statut
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        if (isLoading) {
                            showCancelDialog = true
                        } else {
                            val alreadyExists = MusicHolder.getMusicList().any { music ->
                                music.name.equals(title.trim(), ignoreCase = true)
                                && extractMainArtist(music.artist)?.equals(artist, ignoreCase = true) == true
                            }
                            if (alreadyExists) {
                                showVerificationDialog = true
                            } else {
                                startDownload()
                            }
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
                        else "Télécharger en MP3"
                    )
                }

                // Capsule de statut
                if (status.isNotBlank()) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = when {
                            status.startsWith("✅") ->
                                MaterialTheme.colorScheme.primary

                            status.startsWith("❌") ->
                                MaterialTheme.colorScheme.errorContainer

                            else ->
                                MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = status,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2
                        )
                    }
                }

                if (isLoading) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            // Dialog de verification
            if (showVerificationDialog) {
                AlertDialog(
                    onDismissRequest = { showVerificationDialog = false },
                    title = { Text("Lancer le téléchargement ?") },
                    text = { Text("Une musique de ce titre et de cet artiste existe deja. \nEs-tu sûr de vouloir lancer le téléchargement ?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showVerificationDialog = false
                                startDownload()
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color.Red
                            )
                        ) {
                            Text("Oui, continuer")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showVerificationDialog = false }) {
                            Text("Non, arrêter")
                        }
                    },
                    containerColor = Color(0xFF2C2C2C)
                )
            }

            // Dialog d'annulation
            if (showCancelDialog && isLoading) {
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

                                val cancelIntent =
                                    Intent(context, DownloadService::class.java).apply {
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
                        Button(onClick = { showCancelDialog = false }) {
                            Text("Non, continuer")
                        }
                    },
                    containerColor = Color(0xFF2C2C2C)
                )
            }
        }
    }
}

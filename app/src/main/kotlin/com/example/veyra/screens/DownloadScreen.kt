package com.example.veyra.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.veyra.components.SelectorInput
import com.example.veyra.model.data.MusicHolder
import com.example.veyra.model.convert.DownloadHolder
import com.example.veyra.service.DownloadService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(context: Context = LocalContext.current) {
    var url by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }
    var artist by rememberSaveable { mutableStateOf("") }
    var album by rememberSaveable { mutableStateOf("") }

    val status by DownloadHolder.status

    var restoreArtistSelector by remember { mutableStateOf<(() -> Unit)?>(null) }
    var restoreAlbumSelector by remember { mutableStateOf<(() -> Unit)?>(null) }

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
                label = { Text("YouTube URL") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Nom de la musique") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            SelectorInput(
                list = MusicHolder.getArtistList(),
                placeholder = "Artiste",
                onValueChange = { artist = it },
                onRefCreated = { restoreArtistSelector = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            SelectorInput(
                list = MusicHolder.getAlbumList(),
                placeholder = "Album",
                onValueChange = { album = it },
                onRefCreated = { restoreAlbumSelector = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    DownloadHolder.status.value = "Extraction…"

                    val intent = Intent(context, DownloadService::class.java).apply {
                        putExtra("url", url)
                        putExtra("title", title)
                        putExtra("artist", artist)
                        putExtra("album", album)
                    }
                    context.startService(intent)
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isLoading) "Téléchargement en cours…" else "Télécharger MP3")
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("Status: $status")

            if (isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

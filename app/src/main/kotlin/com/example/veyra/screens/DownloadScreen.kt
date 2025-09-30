package com.example.veyra.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.veyra.components.SelectorInput
import com.example.veyra.model.MusicHolder
import com.example.veyra.model.convert.DownloadBroadcast
import com.example.veyra.service.DownloadService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(context: Context = LocalContext.current) {
    var url by rememberSaveable  { mutableStateOf("") }
    var title by rememberSaveable  { mutableStateOf("") }
    var artist by rememberSaveable  { mutableStateOf("") }
    var album by rememberSaveable  { mutableStateOf("") }
    var status by rememberSaveable  { mutableStateOf("OK") }

    // ✅ Receiver STABLE entre recompositions
    val receiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == DownloadBroadcast.ACTION_STATUS) {
                    intent.getStringExtra(DownloadBroadcast.EXTRA_STATUS)?.let { msg ->
                        status = msg

                        // ✅ Vider les inputs seulement en cas de succès
                        if (msg.startsWith("✅")) {
                            url = ""
                            title = ""
                            artist = ""
                            album = ""
                        }
                    }
                }
            }
        }
    }

    // ✅ Enregistrement compat (gère les flags Android 13+ sous le capot)
    DisposableEffect(Unit) {
        val filter = IntentFilter(DownloadBroadcast.ACTION_STATUS)
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {
                // au cas où il ait déjà été désenregistré
            }
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
                onValueChange = { artist = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            SelectorInput(
                list = MusicHolder.getAlbumList(),
                placeholder = "Album",
                onValueChange = { album = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    status = "Extraction…"

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

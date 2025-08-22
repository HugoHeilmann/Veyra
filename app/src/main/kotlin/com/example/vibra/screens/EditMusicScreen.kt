package com.example.vibra.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.vibra.R
import com.example.vibra.model.Music
import com.example.vibra.model.MusicMetadata

@Composable
fun EditMusicScreen(
    music: Music,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    var title by remember { mutableStateOf(music.name) }
    var artist by remember { mutableStateOf(music.artist ?: "Unknown") }
    var album by remember { mutableStateOf(music.album ?: "Unknown") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.default_album_cover),
            contentDescription = "Music cover",
            modifier = Modifier
                .size(240.dp)
                .padding(24.dp)
        )

        // title input
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Titre") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // artist input
        OutlinedTextField(
            value = artist,
            onValueChange = { artist = it },
            label = { Text("Artiste") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // album input
        OutlinedTextField(
            value = album,
            onValueChange = { album = it },
            label = { Text("Album") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    MetadataManager.updateMetadata(
                        context = context,
                        filePath = music.uri,
                        title = title,
                        artist = artist,
                        album = album
                    )

                    // Redirection
                    onSave()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Sauvegarder")
            }

            OutlinedButton(
                onClick = { onCancel() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Annuler")
            }
        }
    }
}
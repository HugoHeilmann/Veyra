package com.example.veyra.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.example.veyra.components.SelectorInput
import com.example.veyra.model.Music
import com.example.veyra.model.data.MusicHolder
import com.example.veyra.model.metadata.MetadataManager

@Composable
fun EditMusicScreen(
    music: Music,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    var coverPath by remember { mutableStateOf(music.coverPath) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                coverPath = it.toString()
            }
        }
    )

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
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(coverPath ?: music.image)
                .size(Size.ORIGINAL)
                .crossfade(true)
                .error(music.image)
                .fallback(music.image)
                .build(),
            contentDescription = "Music cover",
            modifier = Modifier
                .size(240.dp)
                .padding(24.dp)
                .clickable { imagePicker.launch(arrayOf("image/*")) }
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
        SelectorInput(
            list = MusicHolder.getArtistList(),
            placeholder = music.artist ?: "Artiste",
            onValueChange = { artist = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // album input
        SelectorInput(
            list = MusicHolder.getAlbumList(),
            placeholder = music.album ?: "Album",
            onValueChange = { album = it }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = { onCancel() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Annuler")
            }

            Button(
                onClick = {
                    title = title.trim()
                    artist = artist.trim()
                    album = album.trim()

                    MusicHolder.updateMusic(
                        filePath = music.uri,
                        title = title,
                        artist = artist,
                        album = album,
                        coverPath = coverPath
                    )

                    MusicHolder.addElement(
                        music = music,
                        artist = artist,
                        album = album
                    )

                    MetadataManager.updateMetadata(
                        context = context,
                        filePath = music.uri,
                        title = title,
                        artist = artist,
                        album = album,
                        coverPath = coverPath
                    )

                    music.coverPath = coverPath

                    // Redirection
                    onSave()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Sauvegarder")
            }
        }
    }
}
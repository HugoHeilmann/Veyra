package com.example.veyra.screens

import android.content.Intent
import android.util.Log
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
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.example.veyra.R
import com.example.veyra.components.SelectorInput
import com.example.veyra.model.Music
import com.example.veyra.model.data.MusicHolder
import com.example.veyra.model.metadata.MetadataManager
import com.example.veyra.utils.FileUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMusicScreen(
    music: Music,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    var coverPath by remember { mutableStateOf(music.coverPath) }
    var coverVersion by remember { mutableStateOf(0) }
    val defaultCover = R.drawable.default_album_cover

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                Log.d("EditMusicScreen", "Image sélectionnée : $it")

                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                val copiedPath = FileUtils.copyImageToInternalStorage(
                    context = context,
                    uri = it,
                    musicId = music.uri
                )
                Log.d("EditMusicScreen", "Copie terminée : $copiedPath")

                if (copiedPath != null) {
                    coverPath = copiedPath
                    coverVersion++
                }
            }
        }
    )

    var title by remember { mutableStateOf(music.name) }
    var artist by remember { mutableStateOf(music.artist ?: "Unknown") }
    var album by remember { mutableStateOf(music.album ?: "Unknown") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Éditer le morceau",
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
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ----- Bloc principal : pochette + métadonnées -----
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Pochette & informations",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Log.d("EditMusicScreen", "Chemin affiché : $coverPath")

                    // Pochette
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(
                                when {
                                    !coverPath.isNullOrBlank() -> coverPath
                                    else -> music.image
                                }
                            )
                            .size(Size.ORIGINAL)
                            .crossfade(true)
                            .error(defaultCover)
                            .fallback(defaultCover)
                            .memoryCacheKey("${coverPath ?: music.image}-$coverVersion")
                            .diskCacheKey("${coverPath ?: music.image}-$coverVersion")
                            .build(),
                        contentDescription = "Pochette du morceau",
                        modifier = Modifier
                            .size(220.dp)
                            .padding(top = 8.dp)
                            .clickable {
                                imagePicker.launch(arrayOf("image/*"))
                            }
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                imagePicker.launch(arrayOf("image/*"))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Changer l'image")
                        }

                        OutlinedButton(
                            onClick = {
                                // On repasse sur l'image par défaut (gérée plus haut)
                                coverPath = ""
                                coverVersion++
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Supprimer l'image")
                        }
                    }

                    // Titre
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Titre") },
                        singleLine = true,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Artiste
                    SelectorInput(
                        list = MusicHolder.getArtistList(),
                        placeholder = music.artist ?: "Artiste",
                        onValueChange = { artist = it }
                    )

                    // Album
                    SelectorInput(
                        list = MusicHolder.getAlbumList(),
                        placeholder = music.album ?: "Album",
                        onValueChange = { album = it }
                    )
                }
            }
            
            // ----- Boutons d'action -----
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
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

                        MusicHolder.refreshMapsForMusic(music)

                        MetadataManager.updateMetadata(
                            context = context,
                            filePath = music.uri,
                            title = title,
                            artist = artist,
                            album = album,
                            coverPath = coverPath
                        )

                        music.coverPath = coverPath

                        onSave()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Sauvegarder")
                }
            }
        }
    }
}
package com.example.veyra.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.veyra.components.TopBar
import com.example.veyra.components.animations.CustomLoader
import com.example.veyra.model.data.MusicHolder
import com.example.veyra.model.metadata.AudioTagWriteResult
import com.example.veyra.utils.BulkTagEditManager
import com.example.veyra.model.metadata.MetadataManager
import com.example.veyra.utils.BulkTagEditRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditArtistOrAlbumScreen(
    name: String,
    isArtistCreated: Boolean,
    navController: NavHostController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var searchText by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var pendingRequests by remember { mutableStateOf<List<BulkTagEditRequest>>(emptyList()) }

    val allMusics = MusicHolder.getMusicList()

    val baseMusics = remember(allMusics) {
        if (isArtistCreated) {
            allMusics.filter {
                it.artist.isNullOrBlank() ||
                        it.artist == "Unknown Artist" ||
                        it.artist == "Unknown"
            }
        } else {
            allMusics.filter {
                it.album.isNullOrBlank() || it.album == "Unknown Album"
            }
        }
    }

    val musics by remember(baseMusics, searchText) {
        derivedStateOf {
            if (searchText.isBlank()) {
                baseMusics
            } else {
                val q = searchText.lowercase().trim()

                baseMusics.filter { music ->
                    music.name.lowercase().contains(q) ||
                            (music.artist?.lowercase()?.contains(q) == true) ||
                            (music.album?.lowercase()?.contains(q) == true)
                }
            }
        }
    }

    val selected = remember { mutableStateListOf<String>() }

    fun toggleSelection(uri: String) {
        if (isSaving) return
        if (selected.contains(uri)) {
            selected.remove(uri)
        } else {
            selected.add(uri)
        }
    }

    val writeRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        if (activityResult.resultCode != Activity.RESULT_OK || pendingRequests.isEmpty()) {
            pendingRequests = emptyList()
            isSaving = false
            Toast.makeText(context, "Modification annulée", Toast.LENGTH_LONG).show()
            return@rememberLauncherForActivityResult
        }

        val requests = pendingRequests
        pendingRequests = emptyList()

        scope.launch {
            val results = withContext(Dispatchers.IO) {
                BulkTagEditManager.applyAll(context, requests)
            }

            results.forEach { (request, result) ->
                if (result is AudioTagWriteResult.Success) {
                    val newPath = result.updatedFilePath ?: request.oldFilePath

                    if (newPath != request.oldFilePath) {
                        MetadataManager.renameFilePath(
                            context = context,
                            oldPath = request.oldFilePath,
                            newPath = newPath
                        )
                    }

                    MetadataManager.updateMetadata(
                        context = context,
                        filePath = newPath,
                        title = request.title,
                        artist = request.artist,
                        album = request.album,
                        coverPath = request.coverPath,
                        lastModified = File(newPath).lastModified()
                    )

                    MusicHolder.updateMusic(
                        filePath = request.oldFilePath,
                        title = request.title,
                        artist = request.artist,
                        album = request.album,
                        coverPath = request.coverPath
                    )

                    val updatedMusic = MusicHolder.getMusicList().find { it.uri == request.oldFilePath }
                    if (updatedMusic != null) {
                        updatedMusic.uri = newPath
                        updatedMusic.name = request.title
                        updatedMusic.artist = request.artist
                        updatedMusic.album = request.album
                        updatedMusic.coverPath = request.coverPath
                        MusicHolder.refreshMapsForMusic(updatedMusic)
                    }
                }
            }

            isSaving = false
            navController.popBackStack()
        }
    }

    fun applySelection() {
        if (selected.isEmpty() || isSaving) return

        val selectedMusics = musics.filter { selected.contains(it.uri) }

        if (selectedMusics.isEmpty()) {
            Toast.makeText(context, "Aucune musique sélectionnée", Toast.LENGTH_LONG).show()
            return
        }

        val requests = selectedMusics.mapNotNull { music ->
            val preparation = BulkTagEditManager.prepareCustomEdit(
                context = context,
                oldFilePath = music.uri,
                title = music.name,
                artist = if (isArtistCreated) name else (music.artist ?: "Unknown Artist"),
                album = if (!isArtistCreated) name else (music.album ?: "Unknown Album"),
                coverPath = music.coverPath
            )

            when (preparation) {
                is BulkTagEditManager.CustomEditPreparation.Success -> preparation.request
                is BulkTagEditManager.CustomEditPreparation.Failure -> null
            }
        }

        if (requests.isEmpty()) {
            Toast.makeText(context, "Aucun fichier modifiable trouvé", Toast.LENGTH_LONG).show()
            return
        }

        val intentSender = BulkTagEditManager.createWriteRequestIntentSender(
            context = context,
            uris = requests.map { it.contentUri }
        )

        if (intentSender == null) {
            Toast.makeText(
                context,
                "Autorisation d'écriture non supportée",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        pendingRequests = requests
        isSaving = true

        writeRequestLauncher.launch(
            IntentSenderRequest.Builder(intentSender).build()
        )
    }

    Scaffold(
        topBar = { TopBar(name, navController) },
        bottomBar = {
            Surface(tonalElevation = 3.dp, color = Color.Transparent) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving,
                        onClick = { navController.popBackStack() }
                    ) {
                        Text("Annuler")
                    }

                    Button(
                        colors = if (selected.isEmpty()) {
                            ButtonDefaults.buttonColors(
                                containerColor = Color.Red,
                                contentColor = Color.White
                            )
                        } else {
                            ButtonDefaults.buttonColors()
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving,
                        onClick = {
                            if (selected.isNotEmpty()) {
                                applySelection()
                            } else {
                                navController.popBackStack()
                            }
                        }
                    ) {
                        Text(
                            text = if (selected.isEmpty()) {
                                "Supprimer"
                            } else {
                                "Appliquer (${selected.size})"
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                BasicTextField(
                    value = searchText,
                    onValueChange = {
                        if (!isSaving) {
                            searchText = it
                        }
                    },
                    singleLine = true,
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.inverseOnSurface, shape = MaterialTheme.shapes.small)
                        .padding(12.dp),
                    decorationBox = { innerTextField ->
                        if (searchText.isEmpty()) {
                            Text("Rechercher...", color = Color.Gray)
                        }
                        innerTextField()
                    }
                )

                Spacer(Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(musics, key = { it.uri }) { music ->
                        val isSelected = selected.contains(music.uri)
                        val borderColor = if (isSelected)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else
                            MaterialTheme.colorScheme.surface

                        ListItem(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .border(2.dp, borderColor, RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable(enabled = !isSaving) { toggleSelection(music.uri) }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            headlineContent = {
                                Text(music.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                            supportingContent = {
                                val artist = music.artist ?: "Artiste inconnu"
                                val album = music.album ?: "Album inconnu"
                                Text("$artist • $album", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                            trailingContent = {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = {
                                        if (!isSaving) {
                                            toggleSelection(music.uri)
                                        }
                                    }
                                )
                            }
                        )
                    }

                    if (musics.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Aucune musique à attribuer")
                            }
                        }
                    }
                }
            }

            if (isSaving) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CustomLoader(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(120.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Modification en cours...",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Veuillez patienter",
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
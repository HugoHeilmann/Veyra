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
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.example.veyra.R
import com.example.veyra.components.ArtistSelectorInput
import com.example.veyra.components.PlaylistSelector
import com.example.veyra.components.SelectorInput
import com.example.veyra.model.Music
import com.example.veyra.model.data.MusicHolder
import com.example.veyra.model.metadata.MetadataManager
import com.example.veyra.model.metadata.PlaylistManager
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
    var coverVersion by remember { mutableIntStateOf(0) }
    val defaultCover = R.drawable.default_album_cover

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                val copiedPath = FileUtils.copyImageToInternalStorage(
                    context = context,
                    uri = it,
                    musicId = music.uri
                )

                if (copiedPath != null) {
                    coverPath = copiedPath
                    coverVersion++
                }
            }
        }
    )

    var title by remember { mutableStateOf(music.name) }
    var album by remember { mutableStateOf(music.album ?: "Unknown") }

    val playlistName = PlaylistManager.getAllNames(context)
    val selectedPlaylists = remember { mutableStateListOf<String>() }

    LaunchedEffect(music.uri) {
        selectedPlaylists.clear()
        selectedPlaylists.addAll(
            PlaylistManager.getPlaylistsContaining(context, music.uri)
        )
    }

    // ============================
    // ✅ Parse artiste principal + feats depuis music.artist
    // ============================
    fun parseArtistAndFeats(raw: String?): Pair<String, List<String>> {
        val s = raw?.trim().orEmpty()
        if (s.isBlank()) return "" to emptyList()

        // split autour des tokens de feat
        val featRegex = Regex("""\s*(?:ft\.?|feat\.?|featuring)\s*""", RegexOption.IGNORE_CASE)
        val parts = featRegex.split(s, limit = 2)

        val main = parts.getOrNull(0)?.trim().orEmpty()

        if (parts.size < 2) return main to emptyList()

        val featPart = parts[1]

        // séparateurs possibles entre feats
        val splitRegex = Regex("""\s*(?:&|,| and )\s*""", RegexOption.IGNORE_CASE)

        val feats = featPart
            .split(splitRegex)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }

        return main to feats
    }

    // init states depuis music.artist PARSÉ
    val (initialMainArtist, initialFeats) = remember(music.artist) {
        parseArtistAndFeats(music.artist)
    }

    var mainArtist by remember { mutableStateOf(if (initialMainArtist.isBlank()) (music.artist ?: "Unknown") else initialMainArtist) }
    val feats = remember { mutableStateListOf<String>().apply { addAll(initialFeats) } }

    // ============================
    // ✅ Recompose la string finale artiste
    // ============================
    fun buildArtistString(main: String, featList: List<String>): String {
        val m = main.trim()
        val f = featList.map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }

        if (m.isBlank()) return ""
        if (f.isEmpty()) return m
        return "$m ft. ${f.joinToString(" & ")}"
    }

    // (optionnel) string finale toujours à jour
    val artistFinal by remember(mainArtist, feats) {
        derivedStateOf { buildArtistString(mainArtist, feats) }
    }

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
                    // Pochette
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(
                                coverPath ?: music.image
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
                                coverPath = null
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

                    // Artiste + feats
                    ArtistSelectorInput(
                        artists = MusicHolder.getArtistList(),
                        enabled = true,
                        initialArtist = mainArtist,
                        initialFeats = feats.toList(),
                        onArtistChange = { mainArtist = it },
                        onFeatsChange = { newFeats ->
                            feats.clear()
                            feats.addAll(newFeats)
                        }
                    )

                    // Album
                    SelectorInput(
                        list = MusicHolder.getAlbumList(),
                        placeholder = music.album ?: "Album",
                        onValueChange = { album = it }
                    )

                    // Playlists
                    PlaylistSelector(
                        playlists = playlistName,
                        selectedPlaylists = selectedPlaylists,
                        enabled = !playlistName.isEmpty(),
                        onSelectionChange = { newList ->
                            selectedPlaylists.clear()
                            selectedPlaylists.addAll(newList)
                        }
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
                        album = album.trim()

                        val artistToSave = artistFinal.trim()

                        MusicHolder.updateMusic(
                            filePath = music.uri,
                            title = title,
                            artist = artistToSave,
                            album = album,
                            coverPath = coverPath
                        )

                        MusicHolder.refreshMapsForMusic(music)

                        MetadataManager.updateMetadata(
                            context = context,
                            filePath = music.uri,
                            title = title,
                            artist = artistToSave,
                            album = album,
                            coverPath = coverPath
                        )

                        selectedPlaylists.forEach { playlistName ->
                            PlaylistManager.addMusicToPlaylist(
                                context = context,
                                playlistName = playlistName,
                                filePathOrUri = music.uri
                            )
                        }

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
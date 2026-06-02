package com.example.veyra.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import android.graphics.Color as AndroidColor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
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
import com.example.veyra.model.metadata.*
import com.example.veyra.utils.FileUtils
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private data class PendingTagEdit(
    val oldFilePath: String,
    val contentUri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val coverPath: String?,
    val renameFileName: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMusicScreen(
    music: Music,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isSaving by remember { mutableStateOf(false) }
    var pendingTagEdit by remember { mutableStateOf<PendingTagEdit?>(null) }

    var coverPath by remember { mutableStateOf(music.coverPath) }
    var coverVersion by remember { mutableIntStateOf(0) }
    val defaultCover = R.drawable.default_album_cover

    val playlistName = PlaylistManager.getAllNames(context)
    val selectedPlaylists = remember { mutableStateListOf<String>() }

    var title by remember { mutableStateOf(music.name) }
    var album by remember { mutableStateOf(music.album ?: "Unknown") }

    fun applyLocalChanges(
        originalPath: String,
        finalTitle: String,
        finalArtist: String,
        finalAlbum: String,
        finalCoverPath: String?,
        newPath: String
    ) {
        MusicHolder.updateMusic(
            filePath = originalPath,
            title = finalTitle,
            artist = finalArtist,
            album = finalAlbum,
            coverPath = finalCoverPath
        )

        playlistName.forEach { currentPlaylistName ->
            PlaylistManager.removeMusicFromPlaylist(
                context = context,
                playlistName = currentPlaylistName,
                filePathOrUri = originalPath
            )
        }

        selectedPlaylists.forEach { currentPlaylistName ->
            PlaylistManager.addMusicToPlaylist(
                context = context,
                playlistName = currentPlaylistName,
                filePathOrUri = newPath
            )
        }

        if (newPath != originalPath) {
            MetadataManager.renameFilePath(
                context = context,
                oldPath = originalPath,
                newPath = newPath
            )
        }

        MetadataManager.updateMetadata(
            context = context,
            filePath = newPath,
            title = finalTitle,
            artist = finalArtist,
            album = finalAlbum,
            coverPath = finalCoverPath,
            lastModified = File(newPath).lastModified()
        )

        music.uri = newPath
        music.coverPath = finalCoverPath
        music.image = if (finalCoverPath == null) {
            R.drawable.default_album_cover
        } else {
            0
        }
        music.name = finalTitle
        music.artist = finalArtist
        music.album = finalAlbum

        MusicHolder.refreshMapsForMusic(music)
    }

    val writeRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        val request = pendingTagEdit

        if (activityResult.resultCode != Activity.RESULT_OK || request == null) {
            pendingTagEdit = null
            isSaving = false
            Toast.makeText(context, "Modification annulée", Toast.LENGTH_LONG).show()
            return@rememberLauncherForActivityResult
        }

        scope.launch {
            val result = withContext(Dispatchers.IO) {
                AudioTagWriter.saveTagsAfterPermission(
                    context = context,
                    contentUri = request.contentUri,
                    title = request.title,
                    artist = request.artist,
                    album = request.album,
                    coverPath = request.coverPath,
                    renameFileName = request.renameFileName
                )
            }

            pendingTagEdit = null

            when (result) {
                is AudioTagWriteResult.Success -> {
                    val newPath = result.updatedFilePath ?: request.oldFilePath

                    applyLocalChanges(
                        originalPath = request.oldFilePath,
                        finalTitle = request.title,
                        finalArtist = request.artist,
                        finalAlbum = request.album,
                        finalCoverPath = request.coverPath,
                        newPath = newPath
                    )

                    isSaving = false
                    onSave()
                }

                is AudioTagWriteResult.Failure -> {
                    isSaving = false
                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                }

                is AudioTagWriteResult.NeedsUserPermission -> {
                    isSaving = false
                    Toast.makeText(
                        context,
                        "Autorisation d'écriture toujours requise",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    fun handleWriteResult(
        originalPath: String,
        result: AudioTagWriteResult,
        finalTitle: String,
        finalArtist: String,
        finalAlbum: String,
        finalCoverPath: String?
    ) {
        when (result) {
            is AudioTagWriteResult.Success -> {
                val newPath = result.updatedFilePath ?: originalPath

                applyLocalChanges(
                    originalPath = originalPath,
                    finalTitle = finalTitle,
                    finalArtist = finalArtist,
                    finalAlbum = finalAlbum,
                    finalCoverPath = finalCoverPath,
                    newPath = newPath
                )

                isSaving = false
                onSave()
            }

            is AudioTagWriteResult.NeedsUserPermission -> {
                val intentSender = AudioTagWriter.createWriteRequestIntentSender(
                    context = context,
                    uris = listOf(result.contentUri)
                )

                if (intentSender == null) {
                    isSaving = false
                    Toast.makeText(
                        context,
                        "Autorisation d'écriture non supportée",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }

                pendingTagEdit = PendingTagEdit(
                    oldFilePath = originalPath,
                    contentUri = result.contentUri,
                    title = finalTitle,
                    artist = finalArtist,
                    album = finalAlbum,
                    coverPath = finalCoverPath,
                    renameFileName = finalTitle != music.name.trim()
                )

                writeRequestLauncher.launch(
                    IntentSenderRequest.Builder(intentSender).build()
                )
            }

            is AudioTagWriteResult.Failure -> {
                isSaving = false
                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            return@rememberLauncherForActivityResult
        }

        val croppedUri = UCrop.getOutput(result.data ?: return@rememberLauncherForActivityResult)

        if (croppedUri != null) {
            val copiedPath = FileUtils.copyImageToInternalStorage(
                context = context,
                uri = croppedUri,
                musicId = music.uri
            )

            if (copiedPath != null) {
                coverPath = copiedPath
                coverVersion++
            } else {
                Toast.makeText(
                    context,
                    "Impossible de copier l'image",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { sourceUri ->
            context.contentResolver.takePersistableUriPermission(
                sourceUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            val destinationUri = Uri.fromFile(
                File(context.cacheDir, "cropped_cover_${System.currentTimeMillis()}.jpg")
            )

            val cropOptions = UCrop.Options().apply {
                setToolbarTitle("Recadrer l'image")

                setToolbarColor(AndroidColor.BLACK)
                setStatusBarColor(AndroidColor.BLACK)
                setToolbarWidgetColor(AndroidColor.WHITE)
                setActiveControlsWidgetColor(AndroidColor.WHITE)

                setHideBottomControls(false)

                setMaxScaleMultiplier(5f)
                setImageToCropBoundsAnimDuration(200)

                setMaxBitmapSize(1000)
            }

            val cropIntent = UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(1f, 1f)
                .withMaxResultSize(1000, 1000)
                .withOptions(cropOptions)
                .getIntent(context)

            cropLauncher.launch(cropIntent)
        }
    }

    LaunchedEffect(music.uri) {
        selectedPlaylists.clear()
        selectedPlaylists.addAll(
            PlaylistManager.getPlaylistsContaining(context, music.uri)
        )
    }

    fun parseArtistAndFeats(raw: String?): Pair<String, List<String>> {
        val s = raw?.trim().orEmpty()
        if (s.isBlank()) return "" to emptyList()

        val featRegex = Regex("""\s*(?:ft\.?|feat\.?|featuring)\s*""", RegexOption.IGNORE_CASE)
        val parts = featRegex.split(s, limit = 2)

        val main = parts.getOrNull(0)?.trim().orEmpty()

        if (parts.size < 2) return main to emptyList()

        val featPart = parts[1]
        val splitRegex = Regex("""\s*(?:&|,| and )\s*""", RegexOption.IGNORE_CASE)

        val feats = featPart
            .split(splitRegex)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }

        return main to emptyList<String>() + feats
    }

    val (initialMainArtist, initialFeats) = remember(music.artist) {
        parseArtistAndFeats(music.artist)
    }

    var mainArtist by remember {
        mutableStateOf(
            initialMainArtist.ifBlank {
                music.artist ?: "Unknown"
            }
        )
    }

    val feats = remember {
        mutableStateListOf<String>().apply {
            addAll(initialFeats)
        }
    }

    fun buildArtistString(main: String, featList: List<String>): String {
        val m = main.trim()
        val f = featList.map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }

        if (m.isBlank()) return ""
        if (f.isEmpty()) return m
        return "$m ft. ${f.joinToString(" & ")}"
    }

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
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(coverPath ?: music.image)
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
                            .clickable(enabled = !isSaving) {
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
                            modifier = Modifier.weight(1f),
                            enabled = !isSaving
                        ) {
                            Text("Changer l'image")
                        }

                        OutlinedButton(
                            onClick = {
                                coverPath = null
                                coverVersion++
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isSaving
                        ) {
                            Text("Supprimer l'image")
                        }
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Titre") },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words
                        ),
                        singleLine = true,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving
                    )

                    ArtistSelectorInput(
                        artists = MusicHolder.getArtistList(),
                        enabled = !isSaving,
                        initialArtist = mainArtist,
                        initialFeats = feats.toList(),
                        onArtistChange = { mainArtist = it },
                        onFeatsChange = { newFeats ->
                            feats.clear()
                            feats.addAll(newFeats)
                        }
                    )

                    SelectorInput(
                        list = MusicHolder.getAlbumList(),
                        placeholder = music.album ?: "Album",
                        onValueChange = { album = it }
                    )

                    PlaylistSelector(
                        playlists = playlistName,
                        selectedPlaylists = selectedPlaylists,
                        enabled = playlistName.isNotEmpty() && !isSaving,
                        onSelectionChange = { newList ->
                            selectedPlaylists.clear()
                            selectedPlaylists.addAll(newList)
                        }
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = { onCancel() },
                    modifier = Modifier.weight(1f),
                    enabled = !isSaving
                ) {
                    Text("Annuler")
                }

                Button(
                    onClick = {
                        if (isSaving) return@Button

                        scope.launch {
                            isSaving = true

                            val originalPath = music.uri
                            val finalTitle = title.trim()
                            val finalAlbum = album.trim()
                            val finalArtist = artistFinal.trim()

                            val shouldRenameFile = finalTitle != music.name.trim()

                            Log.d("EditMusicScreen", "SAVE -> title=$finalTitle / artist=$finalArtist / album=$finalAlbum")
                            val result = withContext(Dispatchers.IO) {
                                AudioTagWriter.saveTags(
                                    context = context,
                                    filePath = originalPath,
                                    title = finalTitle,
                                    artist = finalArtist,
                                    album = finalAlbum,
                                    coverPath = coverPath,
                                    renameFileName = shouldRenameFile
                                )
                            }

                            handleWriteResult(
                                originalPath = originalPath,
                                result = result,
                                finalTitle = finalTitle,
                                finalArtist = finalArtist,
                                finalAlbum = finalAlbum,
                                finalCoverPath = coverPath
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isSaving
                ) {
                    Text(if (isSaving) "Sauvegarde..." else "Sauvegarder")
                }
            }
        }
    }
}
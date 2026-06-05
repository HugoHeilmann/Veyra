package com.example.veyra.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.example.veyra.R
import com.example.veyra.components.form.ArtistSelectorInput
import com.example.veyra.components.MusicRow
import com.example.veyra.components.PlaylistSelector
import com.example.veyra.components.form.SelectorInput
import com.example.veyra.components.animations.AnimatedDownloadIcon
import com.example.veyra.model.Music
import com.example.veyra.model.convert.DownloadHolder
import com.example.veyra.model.data.MusicHolder
import com.example.veyra.model.metadata.PlaylistManager
import com.example.veyra.service.DownloadService
import com.example.veyra.utils.FileUtils
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(
    context: Context = LocalContext.current,
    onDownloadedMusicClick: (Music) -> Unit = {},
    onDownloadedMusicEditClick: (Music) -> Unit = {}
) {
    var url by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }
    var artist by rememberSaveable { mutableStateOf("") }
    val feats = remember { mutableStateListOf<String>() }
    var album by rememberSaveable { mutableStateOf("") }

    val status by DownloadHolder.status
    val state by DownloadHolder.state
    val progress by DownloadHolder.progress
    val isLoading by DownloadHolder.isLoading

    var restoreArtistSelector by remember { mutableStateOf<(() -> Unit)?>(null) }
    var restoreAlbumSelector by remember { mutableStateOf<(() -> Unit)?>(null) }

    val playlistName = remember { PlaylistManager.getAllNames(context) }
    val selectedPlaylists = remember { mutableStateListOf<String>() }

    var showCancelDialog by remember { mutableStateOf(false) }
    var showVerificationDialog by remember { mutableStateOf(false) }

    var lastDownloadedMusic by remember { mutableStateOf<Music?>(null) }
    var pendingDownloadedTitle by remember { mutableStateOf("") }
    var pendingDownloadedArtist by remember { mutableStateOf("") }
    var lastHandledTerminalStatus by remember { mutableStateOf("") }

    val defaultCover = R.drawable.default_album_cover

    var coverPath by rememberSaveable { mutableStateOf<String?>(null) }
    var coverVersion by rememberSaveable { mutableIntStateOf(0) }
    var userSelectedCover by rememberSaveable { mutableStateOf(false) }
    var lastAutoCoverVideoId by rememberSaveable { mutableStateOf<String?>(null) }

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
                musicId = title.ifBlank { url }.ifBlank { "download_${System.currentTimeMillis()}" }
            )

            if (copiedPath != null) {
                userSelectedCover = true
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

    fun extractYoutubeVideoId(rawUrl: String): String? {
        val value = rawUrl.trim()

        return when {
            value.contains("youtu.be/") ->
                value.substringAfter("youtu.be/")
                    .substringBefore("?")
                    .substringBefore("&")
                    .substringBefore("/")

            value.contains("watch?v=") ->
                value.substringAfter("watch?v=")
                    .substringBefore("&")

            value.contains("music.youtube.com/watch?v=") ->
                value.substringAfter("watch?v=")
                    .substringBefore("&")

            value.contains("youtube.com/shorts/") ->
                value.substringAfter("youtube.com/shorts/")
                    .substringBefore("?")
                    .substringBefore("&")
                    .substringBefore("/")

            else -> null
        }?.takeIf { it.length >= 8 }
    }

    suspend fun downloadYoutubeThumbnailToInternalStorage(
        context: Context,
        videoId: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val thumbnailUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
            val file = File(context.filesDir, "covers/youtube_$videoId.jpg")

            file.parentFile?.mkdirs()

            URL(thumbnailUrl).openStream().use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            file.absolutePath
        } catch (_: Exception) {
            null
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

    fun findDownloadedMusic(titleToFind: String, artistToFind: String): Music? {
        return MusicHolder.getMusicList()
            .asReversed()
            .firstOrNull { music ->
                music.name.equals(titleToFind.trim(), ignoreCase = true) &&
                        (
                                artistToFind.isBlank() ||
                                        extractMainArtist(music.artist)?.equals(
                                            artistToFind.trim(),
                                            ignoreCase = true
                                        ) == true
                                )
            }
    }

    fun vibrateBrieflyIfAllowed() {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        if (powerManager?.isPowerSaveMode == true) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            val vibrator = vibratorManager?.defaultVibrator ?: return

            if (!vibrator.hasVibrator()) return

            vibrator.vibrate(
                VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return

            @Suppress("DEPRECATION")
            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(60)
            }
        }
    }

    fun clearForm() {
        coverPath = null
        coverVersion++
        userSelectedCover = false
        lastAutoCoverVideoId = null
        url = ""
        title = ""
        artist = ""
        album = ""
        feats.clear()
        restoreArtistSelector?.invoke()
        restoreAlbumSelector?.invoke()
        selectedPlaylists.clear()
    }

    LaunchedEffect(url) {
        delay(700)

        val videoId = extractYoutubeVideoId(url) ?: return@LaunchedEffect

        if (videoId == lastAutoCoverVideoId) return@LaunchedEffect
        if (userSelectedCover) return@LaunchedEffect

        lastAutoCoverVideoId = videoId

        val downloadedCoverPath = downloadYoutubeThumbnailToInternalStorage(
            context = context,
            videoId = videoId
        )

        if (downloadedCoverPath != null) {
            coverPath = downloadedCoverPath
            coverVersion++
        }
    }

    LaunchedEffect(state) {
        val isTerminal = state != 0

        if (!isTerminal || status == lastHandledTerminalStatus) return@LaunchedEffect

        lastHandledTerminalStatus = status
        vibrateBrieflyIfAllowed()

        if (state > 0) {
            var foundMusic: Music? = null

            repeat(20) {
                foundMusic = findDownloadedMusic(
                    titleToFind = pendingDownloadedTitle,
                    artistToFind = pendingDownloadedArtist
                )

                if (foundMusic != null) return@repeat
                delay(250)
            }

            lastDownloadedMusic = foundMusic

            clearForm()
        }
    }

    val finalArtistForService by remember(artist, feats) {
        derivedStateOf {
            val main = artist.trim()
            val cleanedFeats = feats
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinctBy { it.lowercase() }

            if (main.isBlank()) {
                ""
            } else if (cleanedFeats.isEmpty()) {
                main
            } else {
                "$main ft. ${cleanedFeats.joinToString(" & ")}"
            }
        }
    }

    fun startDownload() {
        lastDownloadedMusic = null
        pendingDownloadedTitle = title.trim()
        pendingDownloadedArtist = artist.trim()

        val finalAlbum = album.trim().ifBlank { "Unknown Album" }

        DownloadHolder.status.value = "Extraction…"
        DownloadHolder.state.intValue = 0
        DownloadHolder.isLoading.value = true

        val intent = Intent(context, DownloadService::class.java).apply {
            putExtra("url", url)
            putExtra("coverPath", coverPath)
            putExtra("title", title)
            putExtra("artist", finalArtistForService)
            putExtra("album", finalAlbum)
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
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Informations du morceau",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = url,
                            onValueChange = { url = it },
                            enabled = !isLoading,
                            label = { Text("URL YouTube") },
                            placeholder = { Text("https://youtu.be/...") },
                            singleLine = true,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )

                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(coverPath ?: defaultCover)
                                .crossfade(true)
                                .error(defaultCover)
                                .fallback(defaultCover)
                                .memoryCacheKey("${coverPath ?: defaultCover}-$coverVersion")
                                .diskCacheKey("${coverPath ?: defaultCover}-$coverVersion")
                                .build(),
                            contentDescription = "Pochette du morceau",
                            modifier = Modifier
                                .size(60.dp)
                                .padding(top = 6.dp)
                                .clickable(enabled = !isLoading) {
                                    imagePicker.launch(arrayOf("image/*"))
                                }
                        )
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        enabled = !isLoading,
                        label = { Text("Titre") },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words
                        ),
                        singleLine = true,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth()
                    )

                    ArtistSelectorInput(
                        artists = MusicHolder.getArtistsAndFeats(),
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

                    SelectorInput(
                        list = MusicHolder.getAlbumList(),
                        placeholder = "Album",
                        enabled = !isLoading,
                        onValueChange = { album = it },
                        onRefCreated = { restoreAlbumSelector = it }
                    )

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
                                        && extractMainArtist(music.artist)?.equals(
                                    artist,
                                    ignoreCase = true
                                ) == true
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

                if (status.isNotBlank()) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = when {
                            state > 0 ->
                                MaterialTheme.colorScheme.primary

                            state < 0 ->
                                MaterialTheme.colorScheme.errorContainer

                            else ->
                                MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = status,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
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

            lastDownloadedMusic?.let { downloadedMusic ->
                Spacer(modifier = Modifier.height(4.dp))

                MusicRow(
                    music = downloadedMusic,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onDownloadedMusicClick(downloadedMusic)
                        MusicHolder.setContextName("Musique téléchargée")
                    },
                    onEditClick = {
                        onDownloadedMusicEditClick(it)
                    }
                )
            }

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

            if (showCancelDialog && isLoading) {
                AlertDialog(
                    onDismissRequest = { showCancelDialog = false },
                    title = { Text("Arrêter le téléchargement ?") },
                    text = { Text("Es-tu sûr de vouloir annuler ce téléchargement en cours ?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showCancelDialog = false

                                DownloadHolder.status.value = "Téléchargement annulé"
                                DownloadHolder.state.intValue = -1
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

package com.example.veyra.screens

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight.Companion.SemiBold
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.veyra.AppUIViewModel
import com.example.veyra.components.AlphabeticalListWithFastScroller
import com.example.veyra.components.BlandMusicRow
import com.example.veyra.components.FullColorPickerDialog
import com.example.veyra.components.MusicRow
import com.example.veyra.components.NewArtistOrAlbum
import com.example.veyra.components.PlayerButton
import com.example.veyra.components.animations.CustomLoader
import com.example.veyra.components.animations.WaveBars
import com.example.veyra.model.Music
import com.example.veyra.model.Section
import com.example.veyra.model.data.MusicHolder
import com.example.veyra.model.metadata.MetadataManager
import com.example.veyra.model.metadata.toMusic
import com.example.veyra.utils.loadMusicFromDevice
import com.example.veyra.utils.rememberBulkDeleteHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.Normalizer
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicListScreen(
    navController: NavHostController,
    onColorSelected: (Color) -> Unit,
    defaultTab: String = "Chansons"
) {
    val context = LocalContext.current
    val appUiVm: AppUIViewModel = viewModel(context as ComponentActivity)
    val scope = rememberCoroutineScope()

    var searchText by remember { mutableStateOf("") }
    var selectedTab by rememberSaveable { mutableStateOf(defaultTab) }
    var allMusic by remember { mutableStateOf<List<Music>>(emptyList()) }

    val metadataByPath by remember(allMusic) {
        mutableStateOf(
            MetadataManager.readAll(context).associateBy { it.filePath }
        )
    }

    var showColorDialog by remember { mutableStateOf(false) }
    var isBulkDeleting by remember { mutableStateOf(false) }

    val bulkDeleteHandler = rememberBulkDeleteHandler(
        context = context,
        scope = scope,
        getMusicsForArtistDeletion = MusicHolder::getMusicsForArtistDeletion,
        getMusicsForAlbumDeletion = MusicHolder::getMusicsForAlbumDeletion,
        onProcessingChanged = { processing ->
            isBulkDeleting = processing
            appUiVm.updateBottomBarEnabled(!processing)
        },
        onCompleted = {
            allMusic = MusicHolder.getMusicList().toList()
        }
    )

    LaunchedEffect(Unit) {
        if (MusicHolder.getMusicList().isEmpty()) {
            allMusic = emptyList()
            appUiVm.updateBottomBarEnabled(false)

            launch(Dispatchers.IO) {
                async { scanMusicFolder(context) }.await()
                val musics = async { loadMusicFromDevice(context) }.await()

                withContext(Dispatchers.Main) {
                    MusicHolder.setMusicList(musics)
                    allMusic = musics.toList()
                    appUiVm.updateBottomBarEnabled(true)
                }
            }
        } else {
            allMusic = MusicHolder.getMusicList().toList()
            appUiVm.updateBottomBarEnabled(true)
        }
    }

    val musicList by remember(allMusic, searchText) {
        derivedStateOf {
            allMusic.filter {
                it.name.contains(searchText, ignoreCase = true) ||
                        it.artist?.contains(searchText, ignoreCase = true) == true ||
                        it.album?.contains(searchText, ignoreCase = true) == true
            }
        }
    }

    val artistMap by remember(musicList) {
        derivedStateOf {
            musicList
                .filter { !it.artist.isNullOrBlank() }
                .groupBy {
                    it.artist!!
                        .replace(Regex("\\s+(ft\\.?|feat\\.?|featuring)\\s+.*", RegexOption.IGNORE_CASE), "")
                        .trim()
                }
        }
    }

    val albumMap by remember(musicList) {
        derivedStateOf {
            musicList
                .filter { !it.album.isNullOrBlank() }
                .groupBy { it.album!! }
        }
    }

    val songsListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val artistsListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val albumsListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    val tabs = listOf("Chansons", "Artistes", "Albums")

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            WaveBars(MaterialTheme.colorScheme.primary)

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "Veyra",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.clickable(enabled = !isBulkDeleting) {
                                    showColorDialog = true
                                }
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            WaveBars(MaterialTheme.colorScheme.primary)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            contentWindowInsets = WindowInsets(0)
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                BasicTextField(
                    value = searchText,
                    onValueChange = {
                        if (!isBulkDeleting) {
                            searchText = it
                        }
                    },
                    singleLine = true,
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(MaterialTheme.colorScheme.inverseOnSurface, shape = MaterialTheme.shapes.small)
                        .padding(12.dp),
                    decorationBox = { innerTextField ->
                        if (searchText.isEmpty()) {
                            Text("Rechercher...", color = Color.Gray)
                        }
                        innerTextField()
                    }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    tabs.forEach { tab ->
                        val isSelected = tab == selectedTab

                        val backgroundColor by animateColorAsState(
                            targetValue = if (isSelected)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else
                                Color.Transparent
                        )
                        val textColor by animateColorAsState(
                            targetValue = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onBackground
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable(enabled = !isBulkDeleting) { selectedTab = tab }
                                .background(backgroundColor)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = tab,
                                color = textColor,
                                fontWeight = SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                when (selectedTab) {
                    "Chansons" -> {
                        val groupedSongs = remember(musicList) {
                            groupByFirstLetter(musicList) { it.name }
                        }
                        val sections = remember(groupedSongs) {
                            buildSectionsFromGroupedMap(groupedSongs)
                        }

                        PlayerButton(navController)

                        AlphabeticalListWithFastScroller(
                            sections = sections,
                            headerContent = { letter ->
                                Text(
                                    text = letter,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 16.dp)
                                )
                            },
                            itemContent = { music ->
                                val musicReference = metadataByPath[music.uri]?.toMusic() ?: music

                                MusicRow(
                                    music = musicReference,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(vertical = 8.dp, horizontal = 16.dp),
                                    onClick = {
                                        if (!isBulkDeleting) {
                                            MusicHolder.isShuffled = true
                                            MusicHolder.setCurrentMusic(context, music, null)
                                            navController.navigate("player")
                                        }
                                    },
                                    onEditClick = { _ ->
                                        if (!isBulkDeleting) {
                                            val encodedUri = URLEncoder.encode(
                                                musicReference.uri,
                                                StandardCharsets.UTF_8.toString()
                                            )
                                            navController.navigate("editMusic/${encodedUri}")
                                        }
                                    }
                                )
                            },
                            listState = songsListState
                        )
                    }

                    "Artistes" -> {
                        val groupedArtists = remember(artistMap.keys) {
                            groupByFirstLetter(artistMap.keys.toList()) { it }
                        }
                        val sections = remember(groupedArtists) {
                            buildSectionsFromGroupedMap(groupedArtists)
                        }

                        NewArtistOrAlbum(navController, context, true)

                        AlphabeticalListWithFastScroller(
                            sections = sections,
                            headerContent = { letter ->
                                Text(
                                    text = letter,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 16.dp)
                                )
                            },
                            itemContent = { artist: String ->
                                val songs = artistMap[artist] ?: emptyList()
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = !isBulkDeleting) {
                                            navController.navigate("artist_detail/${Uri.encode(artist)}")
                                        }
                                        .padding(vertical = 8.dp)
                                ) {
                                    BlandMusicRow(
                                        text = artist,
                                        undertext = "${songs.size} chanson${if (songs.size == 1) "" else "s"}",
                                        type = "artist",
                                        onDeleteConfirmed = bulkDeleteHandler.handleBulkDelete
                                    )
                                }
                            },
                            listState = artistsListState
                        )
                    }

                    "Albums" -> {
                        val groupedAlbums = remember(albumMap.keys) {
                            groupByFirstLetter(albumMap.keys.toList()) { it }
                        }
                        val sections = remember(groupedAlbums) {
                            buildSectionsFromGroupedMap(groupedAlbums)
                        }

                        NewArtistOrAlbum(navController, context, false)

                        AlphabeticalListWithFastScroller(
                            sections = sections,
                            headerContent = { letter ->
                                Text(
                                    text = letter,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 16.dp)
                                )
                            },
                            itemContent = { album: String ->
                                val songs = albumMap[album] ?: emptyList()
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = !isBulkDeleting) {
                                            navController.navigate("album_detail/${Uri.encode(album)}")
                                        }
                                        .padding(vertical = 8.dp)
                                ) {
                                    BlandMusicRow(
                                        text = album,
                                        undertext = "${songs.size} chanson${if (songs.size == 1) "" else "s"}",
                                        type = "album",
                                        onDeleteConfirmed = bulkDeleteHandler.handleBulkDelete
                                    )
                                }
                            },
                            listState = albumsListState
                        )
                    }
                }

                FullColorPickerDialog(
                    show = showColorDialog && !isBulkDeleting,
                    onDismiss = { showColorDialog = false },
                    onConfirm = { color ->
                        onColorSelected(color)
                        showColorDialog = false
                    }
                )
            }
        }
    }
}

fun scanMusicFolder(context: Context) {
    val musicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath)

    if (musicDir.exists()) {
        musicDir.listFiles()?.forEach { file ->
            if (file.extension.equals("mp3", ignoreCase = true)) {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(file.absolutePath),
                    arrayOf("audio/mpeg")
                ) { path, uri ->
                    Log.d("Scan", "Fichier scanné : $path -> $uri")
                }
            }
        }
    } else {
        Log.d("Scan", "Dossier /Music/ introuvable")
    }
}

fun <T> groupByFirstLetter(list: List<T>, keySelector: (T) -> String?): Map<Char, List<T>> {
    return list
        .filter { keySelector(it).isNullOrBlank().not() }
        .sortedBy { keySelector(it)?.lowercase() }
        .groupBy { keySelector(it)?.firstOrNull()?.uppercaseChar() ?: '#' }
        .toSortedMap()
}

private fun normalizeHeader(ch: Char): Char {
    if (!ch.isLetter()) return '#'

    val base = Normalizer.normalize(ch.toString(), Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
        .uppercase(Locale.ROOT)

    val c = base.firstOrNull() ?: return '#'
    return if (c.isLetter()) c else '#'
}

private fun <T> buildSectionsFromGroupedMap(grouped: Map<Char, List<T>>): List<Section<T>> {
    val normalizedGrouped: Map<Char, List<T>> =
        grouped.entries
            .groupBy({ normalizeHeader(it.key) }, { it.value })
            .mapValues { (_, lists) -> lists.flatten() }

    val keys = normalizedGrouped.keys
        .sortedWith(compareBy(
            { ch -> if (ch == '#') 0 else 1 },
            { ch -> ch }
        ))

    return keys.map { ch ->
        Section(ch.toString(), normalizedGrouped[ch].orEmpty())
    }
}
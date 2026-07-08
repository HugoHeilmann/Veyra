package com.example.veyra.screens

import android.net.Uri
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
import com.example.veyra.components.form.SearchField
import com.example.veyra.model.Section
import com.example.veyra.model.data.MusicHolder
import com.example.veyra.model.metadata.MetadataManager
import com.example.veyra.model.metadata.toMusic
import com.example.veyra.utils.rememberBulkDeleteHandler
import com.example.veyra.utils.filterResults
import com.example.veyra.utils.updateAlbumMap
import com.example.veyra.utils.updateArtistMap
import kotlinx.coroutines.delay
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

    var isSyncingLibrary = appUiVm.isSyncingLibrary
    var allMusic = appUiVm.displayedMusics

    var searchText by remember { mutableStateOf("") }
    var debouncedSearchText by remember { mutableStateOf("") }
    LaunchedEffect(searchText) {
        delay(500)
        debouncedSearchText = searchText
    }

    var selectedTab by rememberSaveable { mutableStateOf(defaultTab) }

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
            appUiVm.refreshDisplayedMusicsFromHolder()
        }
    )

    LaunchedEffect(Unit) {
        appUiVm.initializeLibrary(context)
    }

    val songsListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val artistsListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val albumsListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    val musicList = remember(allMusic, debouncedSearchText) {
        filterResults(allMusic, debouncedSearchText)
    }
    LaunchedEffect(musicList) {
        when (selectedTab) {
            "Chansons" -> songsListState.scrollToItem(0)
            "Artistes" -> artistsListState.scrollToItem(0)
            "Albums" -> albumsListState.scrollToItem(0)
        }
    }

    val tabs = listOf("Chansons", "Artistes", "Albums")

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isSyncingLibrary) {
                                Row(
                                    modifier = Modifier.align(Alignment.CenterStart),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CustomLoader(
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = "Sync...",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.align(Alignment.Center),
                                verticalAlignment = Alignment.CenterVertically
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
                Box(
                    modifier = Modifier.padding(12.dp)
                ) {
                    SearchField(
                        onValueChange = {
                            if (!isBulkDeleting) {
                                searchText = it
                            }
                        }
                    )
                }

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
                                MusicRow(
                                    music = music,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(vertical = 8.dp, horizontal = 16.dp),
                                    onClick = {
                                        if (!isBulkDeleting) {
                                            MusicHolder.isShuffled = true
                                            MusicHolder.setCurrentMusic(context, music, "Toutes les musiques", null)
                                            navController.navigate("player")
                                        }
                                    },
                                    onEditClick = { _ ->
                                        if (!isBulkDeleting) {
                                            val encodedUri = URLEncoder.encode(
                                                music.uri,
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
                        val artistMap = remember(musicList) {
                            updateArtistMap(musicList)
                        }
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
                                        onDeleteConfirmed = bulkDeleteHandler.handleBulkDelete,
                                        canBeDeleted = (artist != "Unknown Artist")
                                    )
                                }
                            },
                            listState = artistsListState
                        )
                    }

                    "Albums" -> {
                        val albumMap = remember(musicList) {
                            updateAlbumMap(musicList)
                        }
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
                                        onDeleteConfirmed = bulkDeleteHandler.handleBulkDelete,
                                        canBeDeleted = (album != "Unknown Album")
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
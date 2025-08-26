package com.example.veyra.screens

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.veyra.components.MusicRow
import com.example.veyra.model.Music
import com.example.veyra.model.MusicHolder
import com.example.veyra.model.loadMusicFromDevice
import com.example.veyra.model.toMusic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicListScreen(navController: NavHostController, defaultTab: String = "Chansons") {
    val context = LocalContext.current
    var searchText by remember { mutableStateOf("") }
    var selectedTab by rememberSaveable { mutableStateOf(defaultTab) }
    var allMusic by remember { mutableStateOf<List<Music>>(emptyList()) }

    // Charger les musiques au lancement
    LaunchedEffect(Unit) {
        if (MusicHolder.getMusicList().isEmpty()) {
            allMusic = emptyList()

            launch(Dispatchers.IO) {
                scanMusicFolder(context)
                loadMusicFromDevice(context)

                val metadataList = MetadataManager.readAll(context)
                val musics = metadataList.map { it.toMusic() }

                withContext(Dispatchers.Main) {
                    MusicHolder.setMusicList(musics)
                    allMusic = musics
                }
            }
        } else {
            allMusic = MusicHolder.getMusicList()
        }
    }

    // all musics
    val musicList by remember(allMusic, searchText) {
        derivedStateOf {
            allMusic.filter {
                it.name.contains(searchText, ignoreCase = true) ||
                it.artist?.contains(searchText, ignoreCase = true) == true ||
                it.album?.contains(searchText, ignoreCase = true) == true
            }
        }
    }

    // music map according to artist
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

    // music map according to album
    val albumMap by remember(musicList) {
        derivedStateOf {
            musicList
                .filter { !it.album.isNullOrBlank() }
                .groupBy { it.album!! }
        }
    }

    val tabs = listOf("Chansons", "Artistes", "Albums")

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Icon",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Veyra",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 🔍 Barre de recherche
            BasicTextField(
                value = searchText,
                onValueChange = { searchText = it },
                singleLine = true,
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.small)
                    .padding(12.dp),
                decorationBox = { innerTextField ->
                    if (searchText.isEmpty()) {
                        Text("Rechercher...", color = Color.Gray)
                    }
                    innerTextField()
                }
            )

            // 🧭 Onglets : Chansons / Artistes / Albums
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                tabs.forEach { tab ->
                    val isSelected = tab == selectedTab
                    Text(
                        text = tab,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .clickable { selectedTab = tab }
                            .padding(vertical = 8.dp)
                    )
                }
            }

            // 📄 Liste scrollable avec gestion des tabs
            when (selectedTab) {
                "Chansons" -> {
                    val groupedSongs = remember(musicList) {
                        groupByFirstLetter(musicList) { it.name }
                    }
                    val sections = remember(groupedSongs) {
                        buildSectionsFromGroupedMap(groupedSongs)
                    }

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
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 16.dp),
                                onClick = {
                                    MusicHolder.setCurrentMusic(context, music, null)
                                    navController.navigate("player")
                                },
                                onEditClick = { selectedMusic ->
                                    val encodedUri = URLEncoder.encode(selectedMusic.uri, StandardCharsets.UTF_8.toString())
                                    navController.navigate("editMusic/${encodedUri}")
                                }
                            )
                        }
                    )
                }
                "Artistes" -> {
                    val groupedArtists = remember(artistMap.keys) {
                        groupByFirstLetter(artistMap.keys.toList()) { it }
                    }
                    val sections = remember(groupedArtists) {
                        buildSectionsFromGroupedMap(groupedArtists)
                    }

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
                                    .clickable {
                                        navController.navigate("artist_detail/${Uri.encode(artist)}")
                                    }
                                    .padding(vertical = 12.dp, horizontal = 16.dp)
                            ) {
                                Text(
                                    text = artist,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "${songs.size} chanson${if (songs.size == 1) "" else "s"}",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    )
                }
                "Albums" -> {
                    val groupedAlbums = remember(albumMap.keys) {
                        groupByFirstLetter(albumMap.keys.toList()) { it }
                    }
                    val sections = remember(groupedAlbums) {
                        buildSectionsFromGroupedMap(groupedAlbums)
                    }

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
                                    .clickable {
                                        navController.navigate("album_detail/${Uri.encode(album)}")
                                    }
                                    .padding(vertical = 12.dp, horizontal = 16.dp)
                            ) {
                                Text(
                                    text = album,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "${songs.size} chanson${if (songs.size == 1) "" else "s"}",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

// ---------- Utilitaires & FastScroller ----------

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

// Modèle section
private data class Section<T>(
    val label: String,
    val items: List<T>
)

/**
 * Liste + index alphabétique à DROITE (côte à côte, pas d’overlay).
 * Si l’écran est trop petit, on sous-échantillonne l’index (A, C, E, …, Z)
 * et le mapping des clics/drag suit exactement les lettres AFFICHÉES.
 */
@Composable
private fun <T> AlphabeticalListWithFastScroller(
    sections: List<Section<T>>,
    itemContent: @Composable (T) -> Unit,
    headerContent: @Composable (String) -> Unit
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Positions des headers dans la LazyColumn
    val headerPositions = remember(sections) {
        val positions = mutableListOf<Int>()
        var runningIndex = 0
        sections.forEach { s ->
            positions += runningIndex
            runningIndex += 1 + s.items.size
        }
        positions
    }

    val allLabels = remember(sections) { sections.map { it.label } }

    var scrollerHeightPx by remember { mutableStateOf(0) }
    var isDragging by remember { mutableStateOf(false) }
    var previewLabel by remember { mutableStateOf<String?>(null) }

    val density = LocalDensity.current
    val minSlotPx = with(density) { 16.dp.toPx() } // hauteur mini par label pour éviter le clipping

    // Calcul dynamique des labels affichés (sous-échantillonnage si nécessaire)
    val displayLabels by remember(allLabels, scrollerHeightPx) {
        mutableStateOf(run {
            if (scrollerHeightPx <= 0) return@run allLabels // avant mesure, on affiche tout
            val maxVisible = max(1, floor(scrollerHeightPx / minSlotPx).toInt())
            if (allLabels.size <= maxVisible) {
                allLabels
            } else {
                val step = ceil(allLabels.size / maxVisible.toFloat()).toInt().coerceAtLeast(1)
                val disp = mutableListOf<String>()
                var i = 0
                while (i < allLabels.size) {
                    disp += allLabels[i]
                    i += step
                }
                // Assure que le dernier affiché est le dernier réel
                if (disp.last() != allLabels.last()) {
                    if (disp.isNotEmpty()) disp[disp.lastIndex] = allLabels.last() else disp += allLabels.last()
                }
                disp
            }
        })
    }

    // Mapping label affiché -> index réel (proche) dans allLabels
    val displayToRealIndex by remember(allLabels, displayLabels) {
        mutableStateOf(run {
            if (displayLabels.size == allLabels.size) {
                allLabels.indices.toList()
            } else {
                val maxVisible = displayLabels.size
                val step = allLabels.lastIndex.toFloat() / (maxVisible - 1).coerceAtLeast(1)
                List(maxVisible) { i ->
                    val real = (i * step).toInt().coerceIn(0, allLabels.lastIndex)
                    real
                }
            }
        })
    }

    // Conteneur global pour permettre la bulle d’aperçu en overlay, tout en gardant la barre à droite "à côté"
    Box(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize()) {
            // --- Liste à gauche ---
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                sections.forEach { section ->
                    item(key = "header_${section.label}") {
                        headerContent(section.label)
                    }
                    items(section.items.size, key = { idx -> "item_${section.label}_$idx" }) { i ->
                        itemContent(section.items[i])
                    }
                }
            }

            // --- Index alphabétique à droite (côte à côte) ---
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .fillMaxHeight()
                    .padding(end = 6.dp) // petit espace du bord droit
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = MaterialTheme.shapes.small
                    )
                    .onGloballyPositioned { scrollerHeightPx = it.size.height }
                    .pointerInput(displayLabels) {
                        detectTapGestures { offset ->
                            if (scrollerHeightPx > 0 && displayLabels.isNotEmpty()) {
                                val idx = ((offset.y / scrollerHeightPx) * displayLabels.size)
                                    .toInt().coerceIn(0, displayLabels.lastIndex)
                                val realIdx = displayToRealIndex[idx]
                                previewLabel = allLabels[realIdx]
                                isDragging = true
                                scope.launch {
                                    listState.scrollToItem(headerPositions[realIdx])
                                }
                                isDragging = false
                            }
                        }
                    }
                    .pointerInput(displayLabels) {
                        detectDragGestures(
                            onDragStart = { isDragging = true },
                            onDragEnd = { isDragging = false; previewLabel = null },
                            onDragCancel = { isDragging = false; previewLabel = null }
                        ) { change, _ ->
                            if (scrollerHeightPx > 0 && displayLabels.isNotEmpty()) {
                                val y = change.position.y.coerceIn(0f, scrollerHeightPx.toFloat())
                                val idx = ((y / scrollerHeightPx) * displayLabels.size)
                                    .toInt().coerceIn(0, displayLabels.lastIndex)
                                val realIdx = displayToRealIndex[idx]
                                previewLabel = allLabels[realIdx]
                                scope.launch {
                                    listState.scrollToItem(headerPositions[realIdx])
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = 4.dp)
                ) {
                    displayLabels.forEach { lbl ->
                        Text(
                            text = lbl,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // --- Bulle d’aperçu (overlay, au centre) ---
        if (isDragging && previewLabel != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(64.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                        shape = MaterialTheme.shapes.large
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = previewLabel!!,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

private fun <T> buildSectionsFromGroupedMap(grouped: Map<Char, List<T>>): List<Section<T>> {
    val keys = grouped.keys.toList().sortedWith(compareBy(
        { ch ->
            when {
                ch in '0'..'9' -> 0
                ch.isLetter() -> 1
                ch == '#' -> 3
                else -> 2
            }
        },
        { ch -> ch.uppercaseChar() }
    ))

    return keys.map { ch ->
        Section(ch.toString(), grouped[ch].orEmpty())
    }
}

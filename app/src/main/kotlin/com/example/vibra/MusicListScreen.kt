package com.example.vibra

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.vibra.components.MusicRow

import com.example.vibra.model.Music
import com.example.vibra.model.MusicHolder
import com.example.vibra.model.loadMusicFromDevice
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicListScreen(navController: NavHostController, defaultTab: String = "Chansons") {
    val context = LocalContext.current
    var searchText by remember { mutableStateOf("") }
    var selectedTab by rememberSaveable { mutableStateOf(defaultTab) }
    var allMusic by remember { mutableStateOf<List<Music>>(emptyList()) }

    // Charger les musiques au lancement
    LaunchedEffect(Unit) {
        Log.d("Vibra", "Chargement de la musique...")

        scanMusicFolder(context)
        val loaded = loadMusicFromDevice(context)
        Log.d("Vibra", "Musique chargée: ${loaded.size} éléments")
        MusicHolder.setMusicList(loaded)
        allMusic = MusicHolder.getMusicList()

        // allMusic = loadMusicFromDevice(context)
    }

    // Toutes les musiques
    val musicList = allMusic.filter {
        val match = it.name.contains(searchText, ignoreCase = true) ||
                it.artist?.contains(searchText, ignoreCase = true) == true ||
                it.album?.contains(searchText, ignoreCase = true) == true
        if (match) {
            Log.d("Vibra", "Match: ${it.name}")
        }
        match
    }

    // Map des musiques selon l'artiste
    val artistMap = remember(musicList) {
        musicList
            .filter { !it.artist.isNullOrBlank() }
            .groupBy {
                val rawArtist = it.artist ?: "Unknown"

                // Nettoie tout ce qui est après "ft", "feat" ou "featuring" (insensible à la casse)
                rawArtist
                    .replace(Regex("\\s+(ft\\.?|feat\\.?|featuring)\\s+.*", RegexOption.IGNORE_CASE), "")
                    .trim()
            }
    }

    // Map des musiques selon l'album
    val albumMap = remember(musicList) {
        musicList
            .filter { !it.album.isNullOrBlank() }
            .groupBy { it.album ?: "Unfinished" }
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
                            text = "Vibra",
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
        bottomBar = {
            val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.LibraryMusic, contentDescription = "Ma musique") },
                    label = { Text("Ma musique") },
                    selected = currentRoute == "music_list",
                    onClick = {
                        if (currentRoute != "music_list") {
                            navController.navigate("music_list") {
                                popUpTo("music_list") { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Paramètres") },
                    label = { Text("Paramètres") },
                    selected = currentRoute == "settings",
                    onClick = {
                        if (currentRoute != "settings") {
                            navController.navigate("settings") {
                                popUpTo("music_list") // ou "settings", à adapter selon ton graph
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
        }

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

            // 📄 Liste scrollable
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (selectedTab == "Chansons") {
                    val groupedSongs = groupByFirstLetter(musicList) { it.name }

                    groupedSongs.forEach { (letter, songs) ->
                        item {
                            Text(
                                text = letter.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 16.dp)
                            )
                        }

                        items(songs) { music ->
                            MusicRow(
                                music = music,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 16.dp)
                            ) {
                                MusicHolder.setCurrentMusic(music, musicList)
                                navController.navigate("player")
                            }
                        }
                    }
                } else if (selectedTab == "Artistes") {
                    val groupedArtists = groupByFirstLetter(artistMap.keys.toList()) { it }

                    groupedArtists.forEach { (letter, artistNames) ->
                        item {
                            Text(
                                text = letter.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 16.dp)
                            )
                        }

                        items(artistNames) { artist ->
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
                    }
                } else if (selectedTab == "Albums") {
                    val groupedAlbums = groupByFirstLetter(albumMap.keys.toList()) { it }

                    groupedAlbums.forEach { (letter, albumNames) ->
                        item {
                            Text(
                                text = letter.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 16.dp)
                            )
                        }

                        items(albumNames) { album ->
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
                    }
                }
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
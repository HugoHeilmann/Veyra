package com.example.vibra

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.navigation.compose.currentBackStackEntryAsState

import com.example.vibra.model.Music
import com.example.vibra.model.MusicHolder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicListScreen(navController: NavHostController) {
    var searchText by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf("Chansons") }

    val tabs = listOf("Chansons", "Artistes", "Albums")
    val musicList = List(100) { Music(name = "$selectedTab $it") }

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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                items(musicList) { music ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                MusicHolder.currentMusic = music
                                navController.navigate("player")
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Text(
                                text = music.name,
                                color = MaterialTheme.colorScheme.onBackground,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${music.artist ?: "Unknown"} • ${music.album ?: "Unfinished"}",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Image(
                            painter = painterResource(id = music.image),
                            contentDescription = "Music cover",
                            modifier = Modifier
                                .size(64.dp)
                                .aspectRatio(1f)
                        )
                    }
                }
            }
        }
    }
}

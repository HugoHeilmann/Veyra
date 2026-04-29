package com.example.veyra.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavigationBar(navController: NavHostController, isEnabled: Boolean = true) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    )

    Column {
        Spacer(modifier = Modifier.height(8.dp))

        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            tonalElevation = NavigationBarDefaults.Elevation,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .height(120.dp)
                .clip(RoundedCornerShape(24.dp))
        ) {
            // --- Helper: bottom-nav navigation that preserves state ---
            fun navigateBottom(route: String) {
                navController.navigate(route) {
                    launchSingleTop = true
                    restoreState = true
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                }
            }

            // Ma musique
            val isMusicSelected = currentRoute?.startsWith("music_list") ?: false
            val musicScale by animateFloatAsState(
                targetValue = if (isMusicSelected) 1.15f else 1f,
                animationSpec = tween(durationMillis = 200),
                label = "musicScale"
            )

            NavigationBarItem(
                icon = {
                    Icon(
                        Icons.Filled.MusicNote,
                        contentDescription = "Ma musique",
                        modifier = Modifier.graphicsLayer(
                            scaleX = musicScale,
                            scaleY = musicScale
                        )
                    )
                },
                label = { Text("Ma musique", style = MaterialTheme.typography.labelSmall) },
                selected = isMusicSelected,
                enabled = isEnabled,
                colors = itemColors,
                onClick = {
                    if (!isMusicSelected && isEnabled) {
                        // Use the same route format as your NavHost definition
                        navigateBottom("music_list?selectedTab=Chansons")
                    }
                }
            )

            // Queue
            val isQueueSelected = currentRoute?.startsWith("queue") ?: false
            val queueScale by animateFloatAsState(
                targetValue = if (isQueueSelected) 1.15f else 1f,
                animationSpec = tween(durationMillis = 200),
                label = "queueScale"
            )

            NavigationBarItem(
                icon = {
                    Icon(
                        Icons.Filled.QueueMusic,
                        contentDescription = "Queue",
                        modifier = Modifier.graphicsLayer(
                            scaleX = queueScale,
                            scaleY = queueScale
                        )
                    )
                },
                label = { Text("File", style = MaterialTheme.typography.labelSmall) },
                selected = isQueueSelected,
                enabled = isEnabled,
                colors = itemColors,
                onClick = {
                    if (!isQueueSelected && isEnabled) {
                        navigateBottom("queue")
                    }
                }
            )

            // Playlists
            val isPlaylistsSelected = currentRoute?.startsWith("playlists") ?: false
            val playlistsScale by animateFloatAsState(
                targetValue = if (isPlaylistsSelected) 1.15f else 1f,
                animationSpec = tween(durationMillis = 200),
                label = "playlistsScale"
            )

            NavigationBarItem(
                icon = {
                    Icon(
                        Icons.Filled.LibraryMusic,
                        contentDescription = "Playlists",
                        modifier = Modifier.graphicsLayer(
                            scaleX = playlistsScale,
                            scaleY = playlistsScale
                        )
                    )
                },
                label = { Text("Playlists", style = MaterialTheme.typography.labelSmall) },
                selected = isPlaylistsSelected,
                enabled = isEnabled,
                colors = itemColors,
                onClick = {
                    if (!isPlaylistsSelected && isEnabled) {
                        navigateBottom("playlists")
                    }
                }
            )

            // Télécharger
            val isDownloadSelected = currentRoute?.startsWith("download") ?: false
            val downloadScale by animateFloatAsState(
                targetValue = if (isDownloadSelected) 1.15f else 1f,
                animationSpec = tween(durationMillis = 200),
                label = "downloadScale"
            )

            NavigationBarItem(
                icon = {
                    Icon(
                        Icons.Filled.Download,
                        contentDescription = "Télécharger",
                        modifier = Modifier.graphicsLayer(
                            scaleX = downloadScale,
                            scaleY = downloadScale
                        )
                    )
                },
                label = { Text("Télécharger", style = MaterialTheme.typography.labelSmall) },
                selected = isDownloadSelected,
                enabled = isEnabled,
                colors = itemColors,
                onClick = {
                    if (!isDownloadSelected && isEnabled) {
                        navigateBottom("download")
                    }
                }
            )
        }
    }
}

package com.example.veyra.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
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
            label = { Text("Playlists") },
            selected = currentRoute == "playlists",
            onClick = {
                if (currentRoute != "playlists") {
                    navController.navigate("playlists") {
                        popUpTo("music_list")
                        launchSingleTop = true
                    }
                }
            }
        )
    }
}

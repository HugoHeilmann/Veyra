package com.example.vibra

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.vibra.model.MusicHolder
import com.example.vibra.ui.theme.VibraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VibraTheme {
                VibraApp()
            }
        }
    }
}

@Composable
fun VibraApp() {
    val navController = rememberNavController()

    // Gestion de la permission pour lire les fichiers audio
    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { /* On continue même si la permission est refusée */ }
    )

    LaunchedEffect(Unit) {
        permissionLauncher.launch(audioPermission)
    }

    Scaffold { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "music_list",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("music_list") { MusicListScreen(navController) }
            composable("artist_detail/{artistName}") { backStackEntry ->
                val artistName = backStackEntry.arguments?.getString("artistName")
                artistName?.let {
                    ArtistDetailScreen(artistName = it, navController = navController)
                }
            }
            composable("player") { PlayerScreen(navController) }
            composable("settings") { SettingsScreen(navController) }
        }
    }
}

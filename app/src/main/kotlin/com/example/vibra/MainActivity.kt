package com.example.vibra

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.navigation.compose.*
import com.example.vibra.components.BottomNavigationBar
import com.example.vibra.components.MiniPlayerBar
import com.example.vibra.model.MediaSessionManager
import com.example.vibra.model.MusicHolder
import com.example.vibra.model.MusicPlayerManager
import com.example.vibra.screens.*
import com.example.vibra.service.NotificationService
import com.example.vibra.ui.theme.VibraTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Permission notif
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 123)
            }
        }

        MediaSessionManager.init(this)

        setContent {
            VibraTheme {
                VibraApp()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(this, NotificationService::class.java))
        MusicHolder.reset()
        MusicPlayerManager.stopMusic()
    }
}

@Composable
fun VibraApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Permission audio
    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { /* Ignorer si refusée */ }
    )

    LaunchedEffect(Unit) {
        permissionLauncher.launch(audioPermission)
    }

    Scaffold(
        bottomBar = {
            if (currentRoute != "player") {
                Column {
                    MiniPlayerBar(navController)
                    BottomNavigationBar(navController)
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "music_list",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("music_list?selectedTab={selectedTab}") { backStackEntry ->
                val selectedTab = backStackEntry.arguments?.getString("selectedTab") ?: "Chansons"
                MusicListScreen(navController, selectedTab)
            }
            composable("artist_detail/{artistName}") { backStackEntry ->
                val artistName = backStackEntry.arguments?.getString("artistName")
                artistName?.let {
                    ArtistDetailScreen(artistName = it, navController = navController)
                }
            }
            composable("album_detail/{albumName}") { backStackEntry ->
                val albumName = backStackEntry.arguments?.getString("albumName")
                albumName?.let {
                    AlbumDetailScreen(albumName = it, navController = navController)
                }
            }
            composable("player") { PlayerScreen(navController) }
            composable("settings") { SettingsScreen(navController) }
        }
    }
}

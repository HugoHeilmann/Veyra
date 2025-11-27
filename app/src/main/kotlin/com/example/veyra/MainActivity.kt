package com.example.veyra

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.veyra.components.BottomNavigationBar
import com.example.veyra.components.animations.CustomLoader
import com.example.veyra.components.MiniPlayerBar
import com.example.veyra.model.convert.DownloadHolder
import com.example.veyra.model.data.MediaSessionManager
import com.example.veyra.model.data.MusicHolder
import com.example.veyra.model.data.MusicPlayerManager
import com.example.veyra.model.convert.OkHttpDownloader
import com.example.veyra.model.metadata.MetadataManager
import com.example.veyra.model.metadata.PlaylistManager
import com.example.veyra.screens.*
import com.example.veyra.service.DownloadService
import com.example.veyra.service.NotificationService
import com.example.veyra.ui.theme.VeyraTheme
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.Localization
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Permission notif
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 123)
            }
        }

        // Init metadata if needed
        MetadataManager.initializeIfNeeded(this)
        MetadataManager.cleanup(this)

        PlaylistManager.initializeIfNeeded(this)

        //
        MusicPlayerManager.init(this)

        // Init MediaSessionManager
        MediaSessionManager.init(this)

        // Init NewPipeExtractor
        val localization = Localization("en", "US")
        NewPipe.init(OkHttpDownloader(), localization)

        MusicPlayerManager.setOnCompletionListener {
            val nextMusic = MusicHolder.getNext()

            if (nextMusic != null) {
                MusicHolder.setPlayedMusic(this, nextMusic)
            }
        }

        setContent {
            VeyraTheme {
                VeyraApp()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(this, NotificationService::class.java))
        stopService(Intent(this, DownloadService::class.java))
        MusicHolder.reset()
        MusicPlayerManager.stopMusic()
        MusicPlayerManager.release()
        DownloadHolder.reset()
        MetadataManager.cleanup(this)
    }
}

@Composable
fun VeyraApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val appUiVm: AppUIViewModel = viewModel()

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
            if (currentRoute != "player" && currentRoute?.startsWith("editMusic") == false) {
                Column {
                    MiniPlayerBar(navController)
                    BottomNavigationBar(
                        navController,
                        isEnabled = appUiVm.isBottomBarEnabled
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = "music_list",
                modifier = Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
            ) {
                composable("music_list?selectedTab={selectedTab}") { backStackEntry ->
                    val selectedTab =
                        backStackEntry.arguments?.getString("selectedTab") ?: "Chansons"
                    MusicListScreen(navController, selectedTab)
                }
                composable("editMusic/{uri}") { backStackEntry ->
                    val encodedUri = backStackEntry.arguments?.getString("uri") ?: ""
                    val decodedUri =
                        URLDecoder.decode(encodedUri, StandardCharsets.UTF_8.toString())
                    val music = MusicHolder.getMusicList().find { it.uri == decodedUri }

                    music?.let {
                        EditMusicScreen(
                            music = it,
                            onSave = { navController.navigate("music_list?selectedTab=Chansons") },
                            onCancel = { navController.navigate("music_list?selectedTab=Chansons") }
                        )
                    }
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
                composable("queue") { QueueScreen(navController) }
                composable("player") { PlayerScreen(navController) }
                composable("playlists") { PlaylistsScreen(navController) }
                composable(
                    "edit_artist_or_album/{name}/{isArtistCreated}",
                    arguments = listOf(
                        navArgument("name") {
                            type = NavType.StringType
                        },
                        navArgument("isArtistCreated") {
                            type = NavType.BoolType
                        }
                    )
                ) { backStackEntry ->
                    val name = backStackEntry.arguments?.getString("name")
                    val isArtistCreated = backStackEntry.arguments?.getBoolean("isArtistCreated")

                    if (name != null && isArtistCreated != null) {
                        EditArtistOrAlbumScreen(
                            name = name,
                            isArtistCreated = isArtistCreated,
                            navController = navController
                        )
                    }
                }
                composable("edit_playlist/{playlistName}") { backStackEntry ->
                    val playlistName = backStackEntry.arguments?.getString("playlistName")

                    if (playlistName != null) {
                        EditPlaylistScreen(
                            playlistName = playlistName,
                            navController = navController
                        )
                    }
                }
                composable("playlist_preview/{playlistName}") { backStackEntry ->
                    val playlistName = backStackEntry.arguments?.getString("playlistName")

                    if (playlistName != null) {
                        PlaylistPreviewScreen(
                            playlistName = playlistName,
                            navController = navController
                        )
                    }
                }
                composable("download") {
                    DownloadScreen()
                }
            }

            if (!appUiVm.isBottomBarEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.30f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CustomLoader(
                            color = Color(0xFF51FE70),
                            modifier = Modifier.size(128.dp)
                        )
                    }
                }
            }
        }
    }
}

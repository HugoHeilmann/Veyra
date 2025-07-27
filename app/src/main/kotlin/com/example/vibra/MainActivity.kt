package com.example.vibra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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
    Scaffold { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "music_list",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("music_list") { MusicListScreen(navController) }
            composable("player") { PlayerScreen(navController) }
            composable("settings") { SettingsScreen(navController) }
        }
    }
}

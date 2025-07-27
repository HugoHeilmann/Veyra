package com.example.vibra

import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.ui.Alignment
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.vibra.model.MusicHolder
import com.example.vibra.model.MusicPlayerManager
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(navController: NavController) {
    val context = LocalContext.current
    val music = MusicHolder.currentMusic

    if (music == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Aucune musique sélectionnée.")
        }
        return
    }

    var isPlaying by remember { mutableStateOf(MusicPlayerManager.isPlaying()) }

    var currentTime by remember { mutableStateOf(0f) }
    var duration by remember { mutableStateOf(0f) }

    var sliderPosition by remember { mutableStateOf(0f) }
    var isUserSeeking by remember { mutableStateOf(false) }

    LaunchedEffect(music) {
        val isSameMusic = MusicPlayerManager.getCurrentMusic()?.uri == music.uri

        if (!isSameMusic) {
            MusicPlayerManager.playMusic(context, music) { durationMs ->
                duration = durationMs / 1000f
                isPlaying = true
            }
        } else {
            duration = MusicPlayerManager.getDuration() / 1000f
            isPlaying = MusicPlayerManager.isPlaying()
        }
    }

    // Met a jour la position du slider pendant la lecture
    LaunchedEffect(true) {
        while (true) {
            if (MusicPlayerManager.isPlaying()) {
                val pos = MusicPlayerManager.getCurrentPosition() / 1000f
                val dur = MusicPlayerManager.getDuration() / 1000f
                duration = dur.coerceAtLeast(1f) // évite les problèmes avec duration = 0
                currentTime = pos.coerceAtMost(duration)

                if (!isUserSeeking) {
                    sliderPosition = currentTime
                }

                // Si la musique est terminee, on relance au debut
                if (currentTime >= duration - 0.5f) {
                    MusicPlayerManager.seekTo(0)
                    currentTime = 0f
                    sliderPosition = 0f
                    MusicPlayerManager.playMusic(context, music)
                }
            }
            delay(500)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 🔙 Bouton retour
        Row(modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
            }
        }

        // 🎵 Image
        Image(
            painter = painterResource(id = music.image),
            contentDescription = "Image album",
            modifier = Modifier
                .size(280.dp)
                .padding(bottom = 24.dp)
        )

        Text(text = music.name, style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = music.artist ?: "Unknown", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Text(text = music.album ?: "Unfinished", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

        Spacer(modifier = Modifier.height(32.dp))

        // 🎚️ Slider
        Slider(
            value = sliderPosition,
            onValueChange = {
                isUserSeeking = true
                sliderPosition = it
            },
            onValueChangeFinished = {
                MusicPlayerManager.seekTo((sliderPosition * 1000).toInt())
                currentTime = sliderPosition
                isUserSeeking = false
            },
            valueRange = 0f..duration,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatTime(currentTime.toInt()), style = MaterialTheme.typography.labelSmall)
            Text(formatTime(duration.toInt()), style = MaterialTheme.typography.labelSmall)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 🔘 Contrôles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* Previous */ }) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Précédent")
            }

            IconButton(onClick = {
                isPlaying = if (isPlaying) {
                    MusicPlayerManager.pauseMusic(); false
                } else {
                    MusicPlayerManager.playMusic(context, music); true
                }
            }) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    modifier = Modifier.size(48.dp)
                )
            }

            IconButton(onClick = { /* Next */ }) {
                Icon(Icons.Default.SkipNext, contentDescription = "Suivant")
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(minutes, secs)
}

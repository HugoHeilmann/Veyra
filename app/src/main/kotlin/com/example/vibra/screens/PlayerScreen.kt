package com.example.vibra.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.vibra.model.MusicHolder
import com.example.vibra.model.MusicPlayerManager
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(navController: NavController) {
    val context = LocalContext.current
    val currentMusic by rememberUpdatedState(MusicHolder.getCurrentMusic())
    val music = currentMusic

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

    var currentTime by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(0f) }

    var sliderPosition by remember { mutableFloatStateOf(0f) }
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
            currentTime = MusicPlayerManager.getCurrentPosition() / 1000f
            sliderPosition = currentTime
            isPlaying = MusicPlayerManager.isPlaying()
        }
    }

    LaunchedEffect(true) {
        while (true) {
            if (MusicPlayerManager.isPlaying()) {
                val pos = MusicPlayerManager.getCurrentPosition() / 1000f
                val dur = MusicPlayerManager.getDuration() / 1000f
                duration = dur.coerceAtLeast(1f)
                currentTime = pos.coerceAtMost(duration)

                if (!isUserSeeking) {
                    sliderPosition = currentTime
                }

                if (currentTime >= duration - 0.5f) {
                    val nextMusic = MusicHolder.getNext()
                    if (nextMusic != null) {
                        MusicHolder.setPlayedMusic(context, nextMusic)
                        MusicPlayerManager.playMusic(context, nextMusic) { durMs ->
                            duration = durMs / 1000f
                            currentTime = 0f
                            sliderPosition = 0f
                            isPlaying = true
                        }
                    } else {
                        currentTime = 0f
                        sliderPosition = 0f
                        isPlaying = true
                    }
                }
            }
            delay(500)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 🔙 Retour
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
            }
        }

        // 🎵 Image + infos
        Crossfade(targetState = music, label = "music transition") { animatedMusic ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Image(
                    painter = painterResource(id = animatedMusic.image),
                    contentDescription = "Image album",
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .aspectRatio(1f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = animatedMusic.name,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = animatedMusic.artist ?: "Unknown",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Text(
                    text = animatedMusic.album ?: "Unfinished",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }

        // 🎚️ Slider + temps
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatTime(currentTime.toInt()), style = MaterialTheme.typography.labelSmall)
                Text(formatTime(duration.toInt()), style = MaterialTheme.typography.labelSmall)
            }
        }

        // 🔘 Contrôles
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isShuffled = MusicHolder.isShuffled

            // Shuffle toggle
            IconButton(onClick = {
                MusicHolder.enableShuffle(!isShuffled)
            }) {
                Icon(
                    imageVector = if (isShuffled) Icons.Default.Shuffle else Icons.Default.Loop,
                    contentDescription = if (isShuffled) "Désactiver le mode aléatoire" else "Activer le mode aléatoire"
                )
            }

            // Previous
            IconButton(onClick = {
                val previousMusic = MusicHolder.getPrevious()
                if (previousMusic != null) {
                    MusicHolder.setPlayedMusic(context, previousMusic)
                }
            }) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Précédent")
            }

            // Play/Pause
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
                    modifier = Modifier.size(64.dp)
                )
            }

            // Next
            IconButton(onClick = {
                val nextMusic = MusicHolder.getNext()
                if (nextMusic != null) {
                    MusicHolder.setPlayedMusic(context, nextMusic)
                }
            }) {
                Icon(Icons.Default.SkipNext, contentDescription = "Suivant")
            }

            // Spacer pour equilibrer avec le bouton shuffle
            Spacer(modifier = Modifier.size(48.dp))
        }
    }
}

private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(minutes, secs)
}

package com.example.veyra.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.veyra.model.data.MusicHolder
import com.example.veyra.model.data.MusicPlayerManager
import com.example.veyra.service.NotificationService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(navController: NavController) {
    val context = LocalContext.current
    val currentMusic by rememberUpdatedState(MusicHolder.getCurrentMusic())
    val music = currentMusic

    if (music == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
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

    val scope = rememberCoroutineScope()

    var prevBounce by remember { mutableStateOf(0.dp) }
    var nextBounce by remember { mutableStateOf(0.dp) }

    val animatedPrevBounce by animateDpAsState(
        targetValue = prevBounce,
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = 800f
        ),
        label = "prevBounce"
    )

    val animatedNextBounce by animateDpAsState(
        targetValue = nextBounce,
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = 800f
        ),
        label = "nextBounce"
    )

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
            isPlaying = MusicPlayerManager.isPlaying()

            if (isPlaying) {
                val pos = MusicPlayerManager.getCurrentPosition() / 1000f
                val dur = MusicPlayerManager.getDuration() / 1000f
                duration = dur.coerceAtLeast(1f)
                currentTime = pos.coerceAtMost(duration)

                if (!isUserSeeking) {
                    sliderPosition = currentTime
                }
            }
            delay(500)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // 🔙 Barre de navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour"
                    )
                }
                Spacer(modifier = Modifier.width(62.dp))
                Text(
                    text = "Lecture en cours",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 🎵 Image + infos
            Crossfade(targetState = music, label = "music transition") { animatedMusic ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .aspectRatio(1f),
                        shape = RoundedCornerShape(24.dp),
                        tonalElevation = 8.dp,
                        shadowElevation = 8.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Image(
                            painter = if (animatedMusic.coverPath != null) {
                                rememberAsyncImagePainter(animatedMusic.coverPath)
                            } else {
                                painterResource(id = animatedMusic.image)
                            },
                            contentDescription = "Image album",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = animatedMusic.name,
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = animatedMusic.artist ?: "Artiste inconnu",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = animatedMusic.album ?: "Album inconnu",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 🎚️ Slider + temps + shuffle à gauche
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                val shuffleButtonSize = 40.dp

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 🔀 Shuffle à gauche du slider
                    val isShuffled = MusicHolder.isShuffled

                    IconButton(
                        onClick = { MusicHolder.enableShuffle(!isShuffled) },
                        modifier = Modifier.size(shuffleButtonSize)
                    ) {
                        Icon(
                            imageVector = if (isShuffled) {
                                Icons.Default.Shuffle
                            } else {
                                Icons.Default.Loop
                            },
                            contentDescription = "Shuffle",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Slider
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
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp)
                    )
                }

                // Temps en dessous, alignés sous le slider
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = shuffleButtonSize + 4.dp), // même décalage que le début du slider
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        formatTime(currentTime.toInt()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        formatTime(duration.toInt()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ▶️ Ligne unique : -10 / précédent / Play / suivant / +10
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reculer 10s
                IconButton(onClick = {
                    val newPos = MusicPlayerManager.rewind10Seconds()
                    sliderPosition = newPos
                    currentTime = newPos
                }) {
                    Icon(
                        Icons.Default.Replay10,
                        contentDescription = "Reculer 10 secondes",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Piste précédente
                IconButton(
                    onClick = {
                        scope.launch {
                            prevBounce = (-10).dp    // petit coup vers la gauche
                            val previousMusic = MusicHolder.getPrevious()
                            if (previousMusic != null) {
                                MusicHolder.setPlayedMusic(context, previousMusic)
                            }
                            delay(120)
                            prevBounce = 0.dp
                        }
                    },
                    modifier = Modifier.offset(x = animatedPrevBounce)
                ) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Précédent",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Gros bouton Play/Pause
                IconButton(
                    onClick = {
                        if (isPlaying) {
                            MusicPlayerManager.pauseMusic(context)
                        } else {
                            MusicPlayerManager.playMusic(context, music)
                        }

                        try {
                            NotificationService.startOrUpdate(context)
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                // Piste suivante
                IconButton(
                    onClick = {
                        scope.launch {
                            nextBounce = 10.dp      // petit coup vers la droite
                            val nextMusic = MusicHolder.getNext()
                            if (nextMusic != null) {
                                MusicHolder.setPlayedMusic(context, nextMusic)
                            }
                            delay(120)
                            nextBounce = 0.dp
                        }
                    },
                    modifier = Modifier.offset(x = animatedNextBounce)
                ) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Suivant",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Avancer 10s
                IconButton(onClick = {
                    val newPos = MusicPlayerManager.forward10Seconds()
                    sliderPosition = newPos
                    currentTime = newPos
                }) {
                    Icon(
                        Icons.Default.Forward10,
                        contentDescription = "Avancer 10 secondes",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(minutes, secs)
}

package com.example.veyra.components

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.veyra.components.animations.WaveBars
import com.example.veyra.model.data.MusicHolder
import com.example.veyra.model.data.MusicPlayerManager

@Composable
fun MiniPlayerBar(navController: NavHostController) {
    val context = LocalContext.current
    val currentMusic = MusicHolder.currentMusic
    val isPlaying = MusicPlayerManager.isPlaying()
    val currentPosition = MusicPlayerManager.playbackPosition
    val duration = MusicPlayerManager.playbackDuration
    val targetProgress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress.coerceIn(0f, 1f),
        animationSpec = tween(
            durationMillis = 500,
            easing = LinearEasing
        ),
        label = "mini_player_progress"
    )

    currentMusic?.let { music ->
        Surface(
            tonalElevation = 4.dp,
            color = MaterialTheme.colorScheme.inverseOnSurface,
            shadowElevation = 4.dp,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { navController.navigate("player") },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(10.dp))
                        WaveBars(MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = music.name,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = music.artist ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    val isShuffled = MusicHolder.isShuffled

                    IconButton(onClick = {
                        MusicHolder.enableShuffle(!isShuffled)
                    }) {
                        Icon(
                            imageVector = if (isShuffled) Icons.Default.Shuffle else Icons.Default.Loop,
                            contentDescription = if (isShuffled) "Désactiver le mode aléatoire" else "Activer le mode aléatoire",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = {
                        MusicHolder.playPrevious(context)
                    }) {
                        Icon(
                            Icons.Default.SkipPrevious,
                            contentDescription = "Précédent",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = {
                        if (isPlaying) {
                            MusicPlayerManager.pauseMusic(context)
                        } else {
                            MusicPlayerManager.playMusic(context, music)
                        }
                    }) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Lire",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = {
                        MusicHolder.playNext(context)
                    }) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = "Suivant",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}
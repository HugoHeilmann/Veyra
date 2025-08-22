package com.example.vibra.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.vibra.model.MusicHolder
import com.example.vibra.model.MusicPlayerManager

@Composable
fun MiniPlayerBar(navController: NavHostController) {
    val context = LocalContext.current
    val currentMusic by rememberUpdatedState(MusicHolder.getCurrentMusic())
    val isPlaying = MusicPlayerManager.isPlaying()

    currentMusic?.let { music ->
        Surface(
            tonalElevation = 4.dp,
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 4.dp,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 🎵 Partie cliquable pour ouvrir PlayerScreen
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { navController.navigate("player") },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = music.name,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = music.artist ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                // 🎛️ Contrôles
                val isShuffled = MusicHolder.isShuffled

                // Shuffle toggle
                IconButton(onClick = {
                    MusicHolder.enableShuffle(!isShuffled)
                }) {
                    Icon(
                        imageVector = if (isShuffled) Icons.Default.Shuffle else Icons.Default.Loop,
                        contentDescription = if (isShuffled) "Désactiver le mode aléatoire" else "Activer le mode aléatoire",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Previous
                IconButton(onClick = {
                    val previousMusic = MusicHolder.getPrevious()
                    if (previousMusic != null) {
                        MusicHolder.setPlayedMusic(context, previousMusic)
                    }
                }) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Précédent",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Play/Pause
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

                // Next
                IconButton(onClick = {
                    val nextMusic = MusicHolder.getNext()
                    if (nextMusic != null) {
                        MusicHolder.setPlayedMusic(context, nextMusic)
                    }
                }) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Suivant",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

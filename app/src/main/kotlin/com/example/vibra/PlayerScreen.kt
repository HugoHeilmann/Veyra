package com.example.vibra

import androidx.compose.ui.Alignment
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.vibra.model.MusicHolder

@Composable
fun PlayerScreen() {
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

    var isPlaying by remember { mutableStateOf(false) }
    var currentTime by remember { mutableStateOf(0f) }
    val duration = 180f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Image de l'album
        Image(
            painter = painterResource(id = music.image),
            contentDescription = "Image album",
            modifier = Modifier
                .size(280.dp)
                .padding(bottom = 24.dp)
        )

        // Nom, artiste, album
        Text(text = music.name, style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = music.artist ?: "Unknown", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Text(text = music.album ?: "Unfinished", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

        Spacer(modifier = Modifier.height(32.dp))

        // Slider de temps
        Slider(
            value = currentTime,
            onValueChange = { currentTime = it },
            valueRange = 0f..duration,
            modifier = Modifier.fillMaxWidth()
        )

        // Duree affichee
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

        // Controles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* Previous */ }) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Précédent")
            }

            IconButton(onClick = { isPlaying = !isPlaying }) {
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

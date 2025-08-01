package com.example.vibra.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.vibra.model.Music

@Composable
fun MusicRow(music: Music,
             modifier: Modifier = Modifier,
             onClick: () -> Unit
) {
    Row(
        modifier = modifier.clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Text(
                text = music.name,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "${music.artist ?: "Unknown"} • ${music.album ?: "Unfinished"}",
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Image(
            painter = painterResource(id = music.image),
            contentDescription = "Music cover",
            modifier = Modifier
                .size(64.dp)
                .aspectRatio(1f)
        )
    }
}

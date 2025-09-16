package com.example.veyra.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.veyra.R

data class Playlist(
    val name: String,
    val musicCount: Int
)

@Composable
fun PlaylistItem(
    playlist: Playlist,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clickable { onClick() }
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(id = R.drawable.default_album_cover),
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .background(Color.Gray),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = playlist.name, style = MaterialTheme.typography.titleMedium)
            Text(text = "${playlist.musicCount} musiques", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }

        // Play button
        IconButton(onClick = onPlayClick) {
            Icon(
                painter = painterResource(id = R.drawable.ic_play),
                contentDescription = "Play Playlist",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }

        // Delete button
        IconButton(onClick = onDeleteClick) {
            Icon(
                painter = painterResource(id = R.drawable.ic_delete),
                contentDescription = "Delete Playlist",
                tint = Color.Red,
                modifier = Modifier.size(24.dp)
            )
        }

        // Edit button
        IconButton(onClick = onEditClick) {
            Icon(
                painter = painterResource(id = R.drawable.ic_edit),
                contentDescription = "Edit playlist",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
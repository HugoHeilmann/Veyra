package com.example.veyra.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.example.veyra.model.Music
import com.example.veyra.R
import com.example.veyra.model.metadata.MetadataManager
import com.example.veyra.model.metadata.toMusic

@Composable
fun MusicRow(
    music: Music,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onEditClick: (Music) -> Unit
) {
    val context = LocalContext.current

    val usable = MetadataManager.getByPath(context, music.uri)
    val musicToUse = usable?.toMusic() ?: music

    Row(
        modifier = modifier
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Text(
                text = musicToUse.name,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = musicToUse.artist ?: "Unknown",
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = musicToUse.album ?: "Unknown album",
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Icon(
           painter = painterResource(id = R.drawable.ic_edit),
            contentDescription = "Edit metadata",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(36.dp)
                .padding(end = 12.dp)
                .clickable {
                    onEditClick(music)
                }
        )

        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(musicToUse.coverPath ?: musicToUse.image)
                .size(Size.ORIGINAL)
                .crossfade(true)
                .error(musicToUse.image)
                .fallback(musicToUse.image)
                .build(),
            contentDescription = "Music cover",
            modifier = Modifier
                .size(64.dp)
                .aspectRatio(1f)
        )
    }
}

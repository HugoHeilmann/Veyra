package com.example.veyra.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import com.example.veyra.model.Music

@Composable
fun QueueItemRow(
    music: Music,
    isCurrent: Boolean,
    isDragging: Boolean,
    dragOffset: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {           // applique la translation + alpha sur TOUTE la row
                if (isDragging) {
                    translationY = dragOffset
                    alpha = 0.7f
                } else {
                    translationY = 0f
                    alpha = 1f
                }
            }
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick) // clic = lecture
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxHeight()
            ) {
                Text(
                    text = music.name,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = music.artist ?: "Artiste inconnu",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = music.album ?: "Album inconnu",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 🔹 SEULEMENT le burger est "draggable"
            IconButton(
                onClick = { /* rien, drag seulement */ },
                modifier = Modifier.pointerInput(music.uri) {
                    detectDragGestures(
                        onDragStart = {
                            onDragStart()
                        },
                        onDrag = { _, dragAmount ->
                            onDrag(dragAmount)    // remonte juste le delta
                        },
                        onDragEnd = {
                            onDragEnd()
                        },
                        onDragCancel = {
                            onDragEnd()
                        }
                    )
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Réordonner",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

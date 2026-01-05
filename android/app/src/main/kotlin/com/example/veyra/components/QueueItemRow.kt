package com.example.veyra.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.veyra.R
import com.example.veyra.model.Music
import com.example.veyra.model.data.QueueManager

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
    val targetBg = if (isCurrent) Color(0xFF343434) else Color(0xFF2B2B2B)
    val bgColor by animateColorAsState(
        targetValue = targetBg,
        animationSpec = tween(durationMillis = 160),
        label = "queue_item_bg"
    )

    val targetScale = if (isCurrent) 1.01f else 1f
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = 160),
        label = "queue_item_scale"
    )

    val removeEnabled = !isCurrent
    val removeTint = if (removeEnabled) Color.Red else Color(0xFF7A7A7A)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY = if (isDragging) dragOffset else 0f
                alpha = if (isDragging) 0.7f else 1f
                scaleX = scale
                scaleY = scale
            }
            .background(
                color = bgColor,
                shape = MaterialTheme.shapes.medium
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = music.album ?: "Album inconnu",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Remove music
        IconButton(
            onClick = {
                QueueManager.remove(music)
            },
            enabled = removeEnabled,
            modifier = Modifier
                .size(36.dp)
                .padding(end = 12.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_remove_from_queue),
                contentDescription = if (removeEnabled) "Retirer de la file" else "Impossible de retirer le morceau en cours",
                tint = removeTint
            )
        }

        // Handle de drag (burger)
        IconButton(
            onClick = {},
            modifier = Modifier.pointerInput(music.uri) {
                detectDragGestures(
                    onDragStart = { onDragStart() },
                    onDrag = { _, dragAmount -> onDrag(dragAmount) },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() }
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

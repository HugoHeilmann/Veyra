package com.example.veyra.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.veyra.R
import com.example.veyra.model.Music
import com.example.veyra.model.data.MusicHolder

@Composable
fun QueueItemRow(
    music: Music,
    isCurrent: Boolean,
    dragOffset: Float,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "queueWaveTransition")
    val waveProgress by infiniteTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "queueWaveProgress"
    )

    val baseColor = MaterialTheme.colorScheme.primary
    val accent = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY = dragOffset
            }
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(MaterialTheme.shapes.medium)
            .drawBehind {
                if (isCurrent) {
                    val p = waveProgress

                    val startStop = (p - 0.18f).coerceIn(0f, 1f)
                    val midStop = p.coerceIn(0f, 1f)
                    val endStop = (p + 0.18f).coerceIn(0f, 1f)

                    val colorStops = sortedMapOf<Float, Color>().apply {
                        this[0f] = baseColor
                        this[startStop] = baseColor
                        this[midStop] = accent
                        this[endStop] = baseColor
                        this[1f] = baseColor
                    }

                    val brush = Brush.linearGradient(
                        colorStops = colorStops.toSortedMap()
                            .map { it.key to it.value }
                            .toTypedArray(),
                        start = Offset(0f, size.height),
                        end = Offset(size.width, 0f)
                    )

                    drawRect(brush = brush)
                }
            },
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.background
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 4.dp
        ),
        onClick = { onClick?.invoke() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = music.name,
                    color = MaterialTheme.colorScheme.onSurface,
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

            IconButton(
                enabled = enabled,
                onClick = {
                    if (enabled) {
                        MusicHolder.removeFromQueue(music)
                    }
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_delete),
                    contentDescription = "Supprimer",
                    tint = if (enabled) Color.Red else Color.Gray
                )
            }
        }
    }
}

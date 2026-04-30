package com.example.veyra.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
    isDragging: Boolean,
    dragOffset: Float,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    // ===== Wave animation (comme MusicRow) =====
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

    val baseColor = Color(0xFF2B2B2B)
    val accent = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)

    val removeEnabled = !isCurrent
    val removeTint = if (removeEnabled) Color.Red else Color(0xFF7A7A7A)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .graphicsLayer {
                translationY = if (isDragging) dragOffset else 0f
                alpha = if (isDragging) 0.7f else 1f
            }
            .drawBehind {
                // Fond de base
                drawRect(color = baseColor)

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
            }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clickable(enabled = onClick != null) {
                onClick?.invoke()
            }
    ) {
        Column(
            modifier = Modifier.weight(1f)
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

        // Delete button
        IconButton(
            enabled = enabled,
            onClick = {
                if (enabled) {
                    MusicHolder.removeFromQueue(music)
                }
            },
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_delete),
                contentDescription = "Réordonner",
                tint = if (enabled) {
                    Color.Red
                } else {
                    Color.Gray
                }
            )
        }
    }
}

package com.example.veyra.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight.Companion.SemiBold
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.example.veyra.R
import com.example.veyra.model.Music
import com.example.veyra.model.data.MusicHolder
import com.example.veyra.model.data.QueueManager
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
    val inQueue = MusicHolder.isInQueue(music)

    val isCurrent = MusicHolder.getCurrent()?.uri == musicToUse.uri

    val infiniteTransition = rememberInfiniteTransition(label = "waveTransition")
    val waveProgress by infiniteTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveProgress"
    )

    val baseColor = Color(0xFF1A1A1A)
    val accent = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)

    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .shadow(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .drawBehind {
                    // Fond de base
                    drawRect(color = baseColor)

                    if (isCurrent) {
                        // On crée un dégradé linéaire sur la diagonale
                        // et on fait avancer une bande verte le long de cette diagonale
                        val p = waveProgress

                        // On construit les stops du gradient :
                        // base -> base -> accent -> base -> base, centrés autour de p
                        val startStop = (p - 0.18f).coerceIn(0f, 1f)
                        val midStop = p.coerceIn(0f, 1f)
                        val endStop = (p + 0.18f).coerceIn(0f, 1f)

                        // On s'assure que les stops sont croissants
                        val colorStops = sortedMapOf<Float, Color>().apply {
                            this[0f] = baseColor
                            this[startStop] = baseColor
                            this[midStop] = accent
                            this[endStop] = baseColor
                            this[1f] = baseColor
                        }

                        val brush = Brush.linearGradient(
                            colorStops = colorStops.toSortedMap().map { it.key to it.value }.toTypedArray(),
                            start = androidx.compose.ui.geometry.Offset(0f, size.height),
                            end = androidx.compose.ui.geometry.Offset(size.width, 0f)
                        )

                        drawRect(brush = brush)
                    }
                }
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 8.dp),
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
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = musicToUse.artist ?: "Unknown",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = musicToUse.album ?: "Unknown album",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                painter = if (!inQueue) {
                    painterResource(id = R.drawable.ic_add_to_queue)
                } else {
                    painterResource(id = R.drawable.ic_remove_from_queue)
                },
                contentDescription = "Add to queue",
                tint = if (MusicHolder.currentMusic == null) {
                    Color.Gray
                } else if (!inQueue) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Red
                },
                modifier = Modifier
                    .size(36.dp)
                    .padding(end = 12.dp)
                    .clickable(
                        enabled = MusicHolder.currentMusic != null,
                        onClick = {
                            if (!MusicHolder.isInQueue(music)) {
                                MusicHolder.addInQueue(music)
                            } else {
                                MusicHolder.removeFromQueue(music)
                            }
                        }
                    )
            )

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
                    .clip(RoundedCornerShape(12.dp))
                    .size(64.dp)
                    .aspectRatio(1f)
            )
        }
    }
}

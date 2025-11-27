package com.example.veyra.components.animations

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun AnimatedPlaylistIcon(
    modifier: Modifier = Modifier,
    durationMillis: Int = 1200,
    direction: Float = 1f
) {
    val transition = rememberInfiniteTransition()

    val scale by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis),
            repeatMode = RepeatMode.Reverse
        )
    )

    val rotation by transition.animateFloat(
        initialValue = -6f * direction,
        targetValue = 6f * direction,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis),
            repeatMode = RepeatMode.Reverse
        )
    )

    Icon(
        imageVector = Icons.Filled.LibraryMusic,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = modifier.graphicsLayer(
            scaleX = scale,
            scaleY = scale,
            rotationZ = rotation
        )
    )
}

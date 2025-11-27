package com.example.veyra.components.animations

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedDownloadIcon(
    modifier: Modifier = Modifier,
    durationMillis: Int = 1000,
    iconSize: Dp = 24.dp,
    barWidth: Dp = 16.dp,
    barHeight: Dp = 2.dp
) {
    val transition = rememberInfiniteTransition(label = "downloadAnim")

    val offsetY by transition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY"
    )

    val barScale by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis),
            repeatMode = RepeatMode.Reverse
        ),
        label = "barScale"
    )

    Box(
        modifier = modifier.size(iconSize),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.ArrowDownward,
            contentDescription = "Téléchargement",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.offset(y = offsetY.dp)
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .height(barHeight)
                .width(barWidth * barScale)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(barHeight / 2)
                )
        )
    }
}
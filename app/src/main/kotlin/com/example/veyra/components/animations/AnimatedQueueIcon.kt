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
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.zIndex
import kotlin.math.abs
import kotlin.math.floor
import kotlin.random.Random

@Composable
fun AnimatedQueueIcon(
    modifier: Modifier = Modifier,
    durationMillis: Int = 1600,
    iconSize: Dp = 24.dp
) {
    val transition = rememberInfiniteTransition()

    val baseProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = modifier.size(iconSize * 1.6f),
        contentAlignment = Alignment.CenterEnd
    ) {
        val barColor = MaterialTheme.colorScheme.primary
        val barHeight = 3.dp
        val maxBarWidth = iconSize * 0.9f

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(iconSize * 0.75f)
                .background(MaterialTheme.colorScheme.surface)
                .zIndex(Float.MAX_VALUE),
            contentAlignment = Alignment.CenterStart
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }


        listOf(0, 1, 2).forEach { index ->
            val phaseBase = when (index) {
                0 -> 0f
                1 -> 0.2f
                else -> 0.4f
            }

            val phaseJitter = remember(index) { Random.nextFloat() * 0.3f }
            val speedFactor = remember(index) { 0.8f + Random.nextFloat() * 0.6f }

            val v = baseProgress * speedFactor + phaseBase + phaseJitter
            val p = (v - floor(v)).coerceIn(0f, 1f)

            val offsetX = lerp(iconSize, (-iconSize * 0.3f), p)

            val alpha = (1f - abs(p - 0.5f) * 2f).coerceIn(0f, 1f)
            val widthFactor = 0.6f + 0.4f * alpha
            val barWidth = maxBarWidth * widthFactor

            val baseVerticalOffset = when (index) {
                0 -> (-4).dp
                1 -> 0.dp
                else -> 4.dp
            }
            val verticalJitter = remember(index) {
                ((Random.nextFloat() - 0.5f) * 4f).dp
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = offsetX, y = baseVerticalOffset + verticalJitter)
                    .width(barWidth)
                    .height(barHeight)
                    .graphicsLayer(alpha = alpha)
                    .background(
                        color = barColor,
                        shape = RoundedCornerShape(barHeight / 2)
                    )
            )
        }
    }
}

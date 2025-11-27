package com.example.veyra.components.animations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.example.veyra.model.controllers.WaveBarsController

@Composable
fun WaveBars(color: Color) {
    val minHeight = 4f
    val bars = listOf(
        remember { Animatable(minHeight) },
        remember { Animatable(minHeight) },
        remember { Animatable(minHeight) }
    )

    val isPlaying = WaveBarsController.isPlaying

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                bars.forEach { bar ->
                    bar.animateTo((6..16).random().toFloat(), tween(360))
                }
            }
        } else {
            bars.forEach { bar ->
                bar.animateTo(minHeight, tween(250))
            }
        }
    }

    Row(verticalAlignment = Alignment.Bottom) {
        bars.forEachIndexed { index, bar ->
            Box(
                Modifier
                    .width(4.dp)
                    .height(bar.value.dp)
                    .background(color, RoundedCornerShape(2.dp))
            )
            if (index != bars.lastIndex) {
                Spacer(Modifier.width(3.dp))
            }
        }
    }
}

package com.example.veyra.components

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

@Composable
fun WaveBars(color: androidx.compose.ui.graphics.Color) {
    val bars = listOf(
        remember { Animatable(6f) },
        remember { Animatable(12f) },
        remember { Animatable(9f) }
    )

    bars.forEach { bar ->
        LaunchedEffect(Unit) {
            while (true) {
                bar.animateTo((4..16).random().toFloat(), tween(300))
                bar.animateTo((4..16).random().toFloat(), tween(300))
            }
        }
    }

    Row(verticalAlignment = Alignment.Bottom) {
        bars.forEach { bar ->
            Box(
                Modifier
                    .width(4.dp)
                    .height(bar.value.dp)
                    .background(color, RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.width(3.dp))
        }
    }
}

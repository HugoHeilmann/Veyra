package com.example.veyra.components.animations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WaveBars(color: Color) {
    val minHeight = 4f
    val maxHeight = 18f

    val bars = listOf(
        remember { Animatable(minHeight) },
        remember { Animatable(minHeight) },
        remember { Animatable(minHeight) }
    )

    val isPlaying = WaveBarsController.isPlaying

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            bars.forEachIndexed { index, bar ->
                launch {
                    delay(index * 70L)

                    while (WaveBarsController.isPlaying) {
                        val target = (minHeight.toInt()..maxHeight.toInt()).random().toFloat()
                        val duration = (180..260).random()

                        bar.animateTo(
                            target,
                            animationSpec = tween(
                                durationMillis = duration,
                                easing = LinearOutSlowInEasing
                            )
                        )
                    }
                }
            }
        } else {
            bars.forEach { bar ->
                bar.animateTo(
                    minHeight,
                    animationSpec = tween(
                        durationMillis = 200,
                        easing = LinearOutSlowInEasing
                    )
                )
            }
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
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

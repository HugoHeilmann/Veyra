package com.example.veyra.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CustomLoader(
    color: Color = Color(0xFF51FE70),
    modifier: Modifier = Modifier.size(128.dp),
    rotationSpeed: Float = 60f // degrés par seconde
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (360 / rotationSpeed * 1000).toInt(),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationAnim"
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val outerRadius = size.minDimension / 2.6f

        val bigEllipseWidth = size.minDimension / 14f
        val bigEllipseHeight = size.minDimension / 2.8f
        val smallEllipseWidth = size.minDimension / 18f
        val smallEllipseHeight = size.minDimension / 3.6f

        // --- 🟢 Cercle sous-jacent ---
        drawCircle(
            color = color.copy(alpha = 0.4f),
            radius = outerRadius,
            center = center,
            style = Stroke(width = size.minDimension / 30f)
        )

        // --- 🔁 Rotation du groupe des ellipses ---
        rotate(rotation, center) {
            for (i in 0 until 8) {
                val angleDeg = i * 45f
                val angleRad = Math.toRadians(angleDeg.toDouble())

                val isBig = i % 2 == 0
                val ellipseW = if (isBig) bigEllipseWidth else smallEllipseWidth
                val ellipseH = if (isBig) bigEllipseHeight else smallEllipseHeight

                val x = center.x + outerRadius * cos(angleRad).toFloat()
                val y = center.y + outerRadius * sin(angleRad).toFloat()

                // Orientation vers le centre
                withTransform({
                    rotate(degrees = angleDeg + 90f, pivot = Offset(x, y))
                }) {
                    drawOval(
                        color = color,
                        topLeft = Offset(x - ellipseW / 2, y - ellipseH / 2),
                        size = Size(ellipseW, ellipseH)
                    )
                }
            }
        }
    }
}

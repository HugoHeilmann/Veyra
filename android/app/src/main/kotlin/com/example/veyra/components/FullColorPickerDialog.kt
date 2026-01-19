package com.example.veyra.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullColorPickerDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    initialColor: Color = MaterialTheme.colorScheme.primary,
    onConfirm: (Color) -> Unit
) {
    if (!show) return

    var pickerSize by remember { mutableStateOf(IntSize(1, 1)) }
    var cursor by remember { mutableStateOf(Offset.Zero) }
    var didInitCursor by remember(show) { mutableStateOf(false) }

    // Couleur actuelle depuis la position du curseur
    val currentColor by remember(cursor, pickerSize) {
        derivedStateOf {
            val w = pickerSize.width.toFloat().coerceAtLeast(1f)
            val h = pickerSize.height.toFloat().coerceAtLeast(1f)

            val x = (cursor.x / w).coerceIn(0f, 1f)
            val y = (cursor.y / h).coerceIn(0f, 1f)

            val hue = x * 360f
            val sat = 1f
            val value = 1f - y // haut = lumineux, bas = noir

            Color.hsv(hue, sat, value)
        }
    }

    // Init curseur DYNAMIQUE (en fonction de initialColor) dès qu'on connaît la taille
    LaunchedEffect(show, pickerSize, initialColor) {
        if (!show) return@LaunchedEffect
        if (pickerSize.width <= 1 || pickerSize.height <= 1) return@LaunchedEffect
        if (didInitCursor) return@LaunchedEffect

        val hsv = initialColor.toHsv()
        val xNorm = (hsv[0] / 360f).coerceIn(0f, 1f)
        val yNorm = (1f - hsv[2]).coerceIn(0f, 1f)

        val x = xNorm * pickerSize.width.toFloat()
        val y = yNorm * pickerSize.height.toFloat()

        cursor = clampOffset(Offset(x, y), pickerSize)
        didInitCursor = true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choisir une couleur") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // Carré de sélection : TAP + DRAG
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .onSizeChanged { pickerSize = it }
                        .pointerInput(pickerSize) {
                            detectTapGestures { offset ->
                                cursor = clampOffset(offset, pickerSize)
                            }
                        }
                        .pointerInput(pickerSize) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    cursor = clampOffset(offset, pickerSize)
                                },
                                onDrag = { change, _ ->
                                    cursor = clampOffset(change.position, pickerSize)
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // 1) Teinte horizontale
                        val hueBrush = Brush.horizontalGradient(
                            listOf(
                                Color.Red,
                                Color.Yellow,
                                Color.Green,
                                Color.Cyan,
                                Color.Blue,
                                Color.Magenta,
                                Color.Red
                            )
                        )
                        drawRect(brush = hueBrush)

                        // 2) Luminosité verticale vers noir
                        val valueBrush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black)
                        )
                        drawRect(brush = valueBrush)

                        // Curseur
                        val w = size.width.coerceAtLeast(1f)
                        val h = size.height.coerceAtLeast(1f)
                        val cx = cursor.x.coerceIn(0f, w)
                        val cy = cursor.y.coerceIn(0f, h)

                        drawCircle(
                            color = Color.White,
                            radius = 10.dp.toPx(),
                            center = Offset(cx, cy),
                            style = Stroke(width = 3.dp.toPx())
                        )
                        drawCircle(
                            color = Color.Black,
                            radius = 10.dp.toPx(),
                            center = Offset(cx, cy),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }

                    // Bord léger
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                RoundedCornerShape(12.dp)
                            )
                    )
                }

                // Preview + HEX
                val hex = remember(currentColor) {
                    val argb = currentColor.toArgb()
                    String.format("#%08X", argb)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(currentColor, RoundedCornerShape(8.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    )
                    Text(text = hex, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(currentColor) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        },
        containerColor = Color(0xFF1A1A1A)
    )
}

/** Convertit une Color Compose en HSV Android (h:0..360, s:0..1, v:0..1) */
private fun Color.toHsv(): FloatArray {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(this.toArgb(), hsv)
    return hsv
}

/** Clamp une position (px) dans la taille du picker */
private fun clampOffset(o: Offset, size: IntSize): Offset {
    val w = size.width.toFloat().coerceAtLeast(1f)
    val h = size.height.toFloat().coerceAtLeast(1f)
    return Offset(
        x = o.x.coerceIn(0f, w),
        y = o.y.coerceIn(0f, h)
    )
}

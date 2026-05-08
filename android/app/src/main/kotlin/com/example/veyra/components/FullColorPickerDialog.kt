package com.example.veyra.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.veyra.ui.theme.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullColorPickerDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    initialColor: Color = MaterialTheme.colorScheme.primary,
    onConfirm: (Color, Boolean) -> Unit
) {
    if (!show) return

    val themeVm: ThemeViewModel = viewModel()

    var isDarkTheme by remember { mutableStateOf(themeVm.isDarkTheme.value) }

    var pickerSize by remember { mutableStateOf(IntSize(1, 1)) }
    var hueBarSize by remember { mutableStateOf(IntSize(1, 1)) }

    var cursor by remember { mutableStateOf(Offset.Zero) }
    var hueCursorX by remember { mutableFloatStateOf(0f) }

    var hue by remember { mutableFloatStateOf(0f) }
    var didInitCursor by remember(show) { mutableStateOf(false) }

    val currentColor by remember(cursor, pickerSize, hue) {
        derivedStateOf {
            val w = pickerSize.width.toFloat().coerceAtLeast(1f)
            val h = pickerSize.height.toFloat().coerceAtLeast(1f)

            val saturation = (cursor.x / w).coerceIn(0f, 1f)
            val value = (1f - cursor.y / h).coerceIn(0f, 1f)

            Color.hsv(hue, saturation, value)
        }
    }

    LaunchedEffect(show, pickerSize, hueBarSize, initialColor) {
        if (!show) return@LaunchedEffect
        if (pickerSize.width <= 1 || pickerSize.height <= 1) return@LaunchedEffect
        if (hueBarSize.width <= 1) return@LaunchedEffect
        if (didInitCursor) return@LaunchedEffect

        val hsv = initialColor.toHsv()

        hue = hsv[0]

        val saturation = hsv[1].coerceIn(0f, 1f)
        val value = hsv[2].coerceIn(0f, 1f)

        cursor = clampOffset(
            Offset(
                x = saturation * pickerSize.width.toFloat(),
                y = (1f - value) * pickerSize.height.toFloat()
            ),
            pickerSize
        )

        hueCursorX = (hue / 360f).coerceIn(0f, 1f) * hueBarSize.width.toFloat()

        didInitCursor = true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choisir une couleur") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
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
                        drawRect(Color.hsv(hue, 1f, 1f))

                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.White, Color.Transparent)
                            )
                        )

                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black)
                            )
                        )

                        val cx = cursor.x.coerceIn(0f, size.width)
                        val cy = cursor.y.coerceIn(0f, size.height)

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

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                RoundedCornerShape(12.dp)
                            )
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .clip(RoundedCornerShape(50))
                        .onSizeChanged { hueBarSize = it }
                        .pointerInput(hueBarSize) {
                            detectTapGestures { offset ->
                                val w = hueBarSize.width.toFloat().coerceAtLeast(1f)
                                hueCursorX = offset.x.coerceIn(0f, w)
                                hue = (hueCursorX / w) * 360f
                            }
                        }
                        .pointerInput(hueBarSize) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val w = hueBarSize.width.toFloat().coerceAtLeast(1f)
                                    hueCursorX = offset.x.coerceIn(0f, w)
                                    hue = (hueCursorX / w) * 360f
                                },
                                onDrag = { change, _ ->
                                    val w = hueBarSize.width.toFloat().coerceAtLeast(1f)
                                    hueCursorX = change.position.x.coerceIn(0f, w)
                                    hue = (hueCursorX / w) * 360f
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Red,
                                    Color.Yellow,
                                    Color.Green,
                                    Color.Cyan,
                                    Color.Blue,
                                    Color.Magenta,
                                    Color.Red
                                )
                            )
                        )

                        val cx = hueCursorX.coerceIn(0f, size.width)

                        drawCircle(
                            color = Color.White,
                            radius = 9.dp.toPx(),
                            center = Offset(cx, size.height / 2f),
                            style = Stroke(width = 3.dp.toPx())
                        )

                        drawCircle(
                            color = Color.Black,
                            radius = 9.dp.toPx(),
                            center = Offset(cx, size.height / 2f),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                }

                val hex = remember(currentColor) {
                    String.format("#%08X", currentColor.toArgb())
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(currentColor, RoundedCornerShape(8.dp))
                            .border(
                                1.dp,
                                Color.White.copy(alpha = 0.2f),
                                RoundedCornerShape(8.dp)
                            )
                    )

                    Text(
                        text = hex,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isDarkTheme) "Thème sombre" else "Thème clair",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { isDarkTheme = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(currentColor, isDarkTheme) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
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

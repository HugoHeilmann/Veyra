// Theme.kt
package com.example.vibra.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Couleur surbrillance : #0f0
val HighlightGreen = Color(0xFF00FF00)

private val DarkColorScheme = darkColorScheme(
    primary = HighlightGreen,
    onPrimary = Color.Black,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun VibraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(), // Tu peux ajouter une typo custom si besoin
        content = content
    )
}

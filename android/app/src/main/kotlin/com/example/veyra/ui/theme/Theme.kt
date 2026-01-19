package com.example.veyra.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun VeyraTheme(
    primaryColor: Color,
    content: @Composable () -> Unit
) {
    val darkColorScheme = darkColorScheme(
        primary = primaryColor,
        onPrimary = Color.Black,
        background = Color(0xFF121212),
        surface = Color(0xFF1E1E1E),
        onBackground = Color.White,
        onSurface = Color.White
    )

    MaterialTheme(
        colorScheme = darkColorScheme,
        typography = Typography(), // Tu peux ajouter une typo custom si besoin
        content = content
    )
}

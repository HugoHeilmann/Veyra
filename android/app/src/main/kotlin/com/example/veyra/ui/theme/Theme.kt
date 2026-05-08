package com.example.veyra.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun VeyraTheme(
    primaryColor: Color,
    isDarkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val lightColorScheme = lightColorScheme(
        primary = primaryColor,
        onPrimary = Color.White,

        background = Color(0xFFF5F5F5),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFEAEAEA),

        onBackground = Color(0xFF121212),
        onSurface = Color(0xFF1E1E1E),
        onSurfaceVariant = Color(0xFF5F5F5F),

        outline = Color(0xFFDDDDDD),
        outlineVariant = Color(0xFFE0E0E0)
    )

    val darkColorScheme = darkColorScheme(
        primary = primaryColor,
        onPrimary = Color.Black,

        background = Color(0xFF121212),
        surface = Color(0xFF1E1E1E),
        surfaceVariant = Color(0xFF2A2A2A),

        onBackground = Color.White,
        onSurface = Color.White,
        onSurfaceVariant = Color(0xFFB3B3B3),

        outline = Color(0xFF333333),
        outlineVariant = Color(0xFF2C2C2C)
    )

    MaterialTheme(
        colorScheme = if (isDarkTheme) {
            darkColorScheme
        } else {
            lightColorScheme
        },
        typography = Typography(),
        content = content
    )
}

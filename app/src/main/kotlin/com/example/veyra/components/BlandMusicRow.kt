package com.example.veyra.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun BlandMusicRow(
    text: String,
    undertext: String
) {
    Text(
        text = text,
        color = Color.White
    )
    Text(
        text = undertext,
        color = Color.Gray,
        modifier = Modifier.padding(top = 4.dp)
    )
}
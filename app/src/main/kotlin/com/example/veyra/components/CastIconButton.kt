package com.example.veyra.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory

@Composable
fun CastIconButton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(48.dp),
        contentAlignment = Alignment.Center
    ) {
        // 🧱 Bouton Chromecast "invisible" (gère clic et menu)
        AndroidView(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Transparent),
            factory = { context ->
                MediaRouteButton(context).apply {
                    alpha = 0f // invisible mais actif
                    CastButtonFactory.setUpMediaRouteButton(context, this)
                }
            }
        )

        // 🎨 Icône visible stylisée selon ton thème
        Icon(
            imageVector = Icons.Default.Cast,
            contentDescription = "Chromecast",
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

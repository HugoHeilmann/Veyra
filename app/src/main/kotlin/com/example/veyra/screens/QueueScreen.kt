package com.example.veyra.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.veyra.components.RandomPlay
import com.example.veyra.model.data.QueueManager
import com.example.veyra.model.data.MusicPlayerManager
import com.example.veyra.model.Music

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    navController: NavHostController,
    onBack: () -> Unit = { navController.popBackStack() }
) {
    val context = LocalContext.current

    // QueueManager.queue est une SnapshotStateList -> Compose se mettra à jour automatiquement
    val queue = QueueManager.queue
    val currentIndex = QueueManager.currentIndex
    val current = QueueManager.getCurrent()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("File de lecture") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (queue.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text("Aucun morceau dans la file de lecture")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                RandomPlay(navController, list = QueueManager.queue)
                // Section "En lecture maintenant"
                if (current != null && currentIndex in queue.indices) {
                    Text(
                        text = "En lecture maintenant",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    QueueItemRow(
                        music = current,
                        isCurrent = true,
                        onClick = {
                            // On rejoue simplement le morceau courant (au cas où il était en pause)
                            MusicPlayerManager.playFromQueueIndex(context, currentIndex)
                        }
                    )

                    Divider(modifier = Modifier.padding(top = 8.dp))
                }

                // Section "À suivre"
                Text(
                    text = "À suivre",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // On affiche toute la queue, en marquant l'élément courant si tu veux
                    itemsIndexed(queue) { index, music ->
                        QueueItemRow(
                            music = music,
                            isCurrent = index == currentIndex,
                            onClick = {
                                MusicPlayerManager.playFromQueueIndex(context, index)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueItemRow(
    music: Music,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    val title = remember(music.name) { music.name }
    val artist = remember(music.artist) { music.artist ?: "Artiste inconnu" }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = if (isCurrent) "▶ $title" else title,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = artist,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

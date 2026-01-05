package com.example.veyra.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.veyra.components.PlayerButton
import com.example.veyra.components.QueueItemRow
import com.example.veyra.model.data.QueueManager
import com.example.veyra.R
import com.example.veyra.components.animations.AnimatedQueueIcon
import com.example.veyra.model.data.MusicHolder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    navController: NavHostController,
) {
    val context = LocalContext.current
    val queue = QueueManager.queue
    val currentIndex = QueueManager.currentIndex

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var draggedMusicUri by remember { mutableStateOf<String?>(null) }
    var draggedOffsetY by remember { mutableFloatStateOf(0f) }

    val dragThresholdPx = with(LocalDensity.current) {
        32.dp.toPx()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedQueueIcon()

                        Spacer(Modifier.width(8.dp))

                        Text(
                            text = "File de lecture",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleLarge
                        )

                        Spacer(Modifier.width(8.dp))

                        AnimatedQueueIcon()
                    }
                }
            )
        }
    ) { paddingValues ->

        if (queue.isEmpty()) {
            // ÉTAT VIDE
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add_to_queue),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Aucun morceau dans la file de lecture",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Ajoutez des morceaux à partir de votre bibliothèque.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Bouton de lecture du morceau courant (mini player)
                PlayerButton(
                    navController,
                    list = QueueManager.queue,
                    random = false,
                    onClick = {
                        QueueManager.isLaunched = true
                    }
                )

                // Carte qui contient le titre "À suivre" + la liste
                Surface(
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 2.dp,
                    color = Color(0xFF1A1A1A),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "À suivre",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Divider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize(),
                            contentPadding = PaddingValues(
                                horizontal = 8.dp,
                                vertical = 8.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            itemsIndexed(
                                queue,
                                key = { _, music -> music.uri }
                            ) { index, music ->
                                val isDragging = draggedMusicUri == music.uri
                                val currentUri = MusicHolder.getCurrent()?.uri

                                QueueItemRow(
                                    music = music,
                                    isCurrent = QueueManager.isLaunched && currentUri == music.uri,
                                    isDragging = isDragging,
                                    dragOffset = if (isDragging) draggedOffsetY else 0f,
                                    onDragStart = {
                                        draggedMusicUri = music.uri
                                        draggedOffsetY = 0f
                                    },
                                    onDrag = { dragAmount ->
                                        val draggedUri = draggedMusicUri ?: return@QueueItemRow
                                        draggedOffsetY += dragAmount.y

                                        val current = QueueManager.queue.indexOfFirst { it.uri == draggedUri }
                                        if (current == -1) return@QueueItemRow

                                        if (draggedOffsetY > dragThresholdPx &&
                                            current < QueueManager.queue.lastIndex
                                        ) {
                                            val newIndex = current + 1
                                            QueueManager.move(current, newIndex)
                                            draggedOffsetY = -dragThresholdPx
                                        } else if (draggedOffsetY < -dragThresholdPx &&
                                            current > 0
                                        ) {
                                            val newIndex = current - 1
                                            QueueManager.move(current, newIndex)
                                            draggedOffsetY = dragThresholdPx
                                        }
                                    },
                                    onDragEnd = {
                                        draggedMusicUri = null
                                        draggedOffsetY = 0f
                                    }
                                )

                                Spacer(Modifier.height(4.dp))
                            }
                        }
                    }
                }

                // Bouton pour vider la file
                OutlinedButton(
                    onClick = {
                        QueueManager.clearQueue()
                    },
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Vider la file")
                }
            }
        }
    }
}

package com.example.veyra.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.veyra.components.PlayerButton
import com.example.veyra.components.QueueItemRow
import com.example.veyra.model.data.QueueManager
import com.example.veyra.model.data.MusicPlayerManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    navController: NavHostController,
) {
    val context = LocalContext.current
    val queue = QueueManager.queue
    val currentIndex = QueueManager.currentIndex

    val listState = rememberLazyListState()

    var draggedMusicUri by remember { mutableStateOf<String?>(null) }
    var draggedOffsetY by remember { mutableStateOf(0f) }

    val dragThresholdPx = with(LocalDensity.current) {
        32.dp.toPx()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("File de lecture") }
            )
        }
    ) { paddingValues ->
        if (queue.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Aucun morceau dans la file de lecture")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                PlayerButton(
                    navController,
                    list = QueueManager.queue,
                    random = false,
                    onClick = {
                        QueueManager.isLaunched = true
                    }
                )

                Text(
                    text = "À suivre",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    itemsIndexed(
                        queue,
                        key = { _, music -> music.uri }
                    ) { index, music ->
                        val isDragging = draggedMusicUri == music.uri

                        QueueItemRow(
                            music = music,
                            isCurrent = index == currentIndex,
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .pointerInput(music.uri) {
                                    detectDragGestures(
                                        onDragStart = {
                                            draggedMusicUri = music.uri
                                            draggedOffsetY = 0f
                                        },
                                        onDrag = { _, dragAmount ->
                                            val draggedUri = draggedMusicUri ?: return@detectDragGestures
                                            draggedOffsetY += dragAmount.y

                                            val current = QueueManager.queue.indexOfFirst { it.uri == draggedUri }
                                            if (current == -1) return@detectDragGestures

                                            if (draggedOffsetY > dragThresholdPx && current < QueueManager.queue.lastIndex) {
                                                val newIndex = current + 1
                                                QueueManager.move(current, newIndex)
                                                draggedOffsetY = -dragThresholdPx
                                            } else if (draggedOffsetY < -dragThresholdPx && current > 0) {
                                                val newIndex = current - 1
                                                QueueManager.move(current, newIndex)
                                                draggedOffsetY = dragThresholdPx
                                            }
                                        },
                                        onDragEnd = {
                                            draggedMusicUri = null
                                            draggedOffsetY = 0f
                                        },
                                        onDragCancel = {
                                            draggedMusicUri = null
                                            draggedOffsetY = 0f
                                        }
                                    )
                                }
                                .graphicsLayer {
                                    if (isDragging) {
                                        translationY = draggedOffsetY
                                        alpha = 0.7f
                                    } else {
                                        translationY = 0f
                                        alpha = 1f
                                    }
                                },
                            onClick = {
                                MusicPlayerManager.playFromQueueIndex(context, index)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        QueueManager.clearQueue()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text("Vider la file")
                }
            }
        }
    }
}
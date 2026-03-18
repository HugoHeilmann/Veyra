package com.example.veyra.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.veyra.R
import com.example.veyra.components.QueueItemRow
import com.example.veyra.components.animations.AnimatedQueueIcon
import com.example.veyra.model.data.MusicHolder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    navController: NavHostController,
) {
    val currentMusic = MusicHolder.currentMusic
    val activeList = MusicHolder.getActiveList()
    val currentIndex = MusicHolder.currentIndex

    val displayedList = buildList {
        addAll(MusicHolder.getQueue())

        if (size < 5 && currentIndex in activeList.indices) {
            val nextMusics = activeList.drop(currentIndex + 1)

            for (music in nextMusics) {
                if (size >= 5) break
                if (!contains(music)) add(music)
            }
        }
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
        if (currentMusic == null && displayedList.isEmpty()) {
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
                if (currentMusic != null) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        tonalElevation = 2.dp,
                        color = Color(0xFF1A1A1A),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "En cours de lecture",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            QueueItemRow(
                                music = currentMusic,
                                isCurrent = true,
                                isDragging = false,
                                dragOffset = 0f,
                                onDragStart = {},
                                onDrag = {},
                                onDragEnd = {}
                            )
                        }
                    }
                }

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
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Liste de lecture",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        val listState = rememberLazyListState()

                        LaunchedEffect(displayedList) {
                            listState.scrollToItem(0)
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                horizontal = 8.dp,
                                vertical = 8.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(
                                displayedList,
                                key = { music -> music.uri }
                            ) { music ->
                                QueueItemRow(
                                    music = music,
                                    isCurrent = false,
                                    isDragging = false,
                                    dragOffset = 0f,
                                    onDragStart = {},
                                    onDrag = {},
                                    onDragEnd = {}
                                )

                                Spacer(Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
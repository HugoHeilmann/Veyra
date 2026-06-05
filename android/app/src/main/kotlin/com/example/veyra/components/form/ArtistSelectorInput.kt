package com.example.veyra.components.form

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistSelectorInput(
    artists: List<String>,
    artistPlaceholder: String = "Artiste",
    featPlaceholder: String = "Ajouter un feat",
    enabled: Boolean = true,

    initialArtist: String = "",
    initialFeats: List<String> = emptyList(),

    onArtistChange: (String) -> Unit = {},
    onFeatsChange: (List<String>) -> Unit = {},

    onRefCreated: ((() -> Unit) -> Unit)? = null
) {
    val disabledColor = Color(0x88FFFFFF)
    val dialogBg = Color(0xFF2C2C2C)

    var artistText by remember { mutableStateOf(initialArtist) }
    var artistExpanded by remember { mutableStateOf(false) }
    var debouncedArtistText by remember { mutableStateOf("") }

    var feats by remember { mutableStateOf(initialFeats.distinct().filter { it.isNotBlank() }) }
    var featsDialogOpen by remember { mutableStateOf(false) }

    val filteredArtistsForMain = remember(debouncedArtistText, artists) {
        if (debouncedArtistText.length < 3) {
            emptyList()
        } else {
            artists.filter {
                it.contains(debouncedArtistText, ignoreCase = true)
            }
        }
    }

    LaunchedEffect(artistText, enabled) {
        if (!enabled) return@LaunchedEffect

        val query = artistText.trim()

        if (query.length < 3) {
            artistExpanded = false
            debouncedArtistText = ""
            return@LaunchedEffect
        }

        artistExpanded = true

        delay(250)

        debouncedArtistText = query
    }

    LaunchedEffect(Unit) {
        onRefCreated?.invoke {
            artistText = ""
            artistExpanded = false
            debouncedArtistText = ""
            feats = emptyList()
            featsDialogOpen = false
            onArtistChange("")
            onFeatsChange(emptyList())
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Box {
                OutlinedTextField(
                    value = artistText,
                    onValueChange = {
                        artistText = it
                        onArtistChange(it)
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words
                    ),
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = {
                        if (artistText.isEmpty() && artistPlaceholder.isNotEmpty()) {
                            Text(artistPlaceholder)
                        }
                    },
                    trailingIcon = {
                        IconButton(
                            enabled = enabled,
                            onClick = {
                                artistExpanded = !artistExpanded
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Ouvrir la liste"
                            )
                        }
                    }
                )

                DropdownMenu(
                    expanded = artistExpanded,
                    onDismissRequest = { artistExpanded = false },
                    properties = PopupProperties(focusable = false),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    if (filteredArtistsForMain.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Aucun résultat") },
                            onClick = {}
                        )
                    } else {
                        filteredArtistsForMain.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    artistText = item
                                    onArtistChange(item)
                                    artistExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(48.dp)
        ) {
            IconButton(
                enabled = enabled,
                onClick = { featsDialogOpen = !featsDialogOpen }
            ) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = if (featsDialogOpen) "Fermer les feats" else "Gérer les feats",
                    tint = if (enabled) MaterialTheme.colorScheme.primary else disabledColor
                )
            }

            if (feats.isNotEmpty()) {
                val badgeText = if (feats.size > 9) "9+" else feats.size.toString()

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-2).dp, y = (-2).dp)
                        .size(18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = badgeText,
                                color = Color.Black,
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }

    if (featsDialogOpen) {
        AlertDialog(
            onDismissRequest = { featsDialogOpen = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Feats",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )

                    IconButton(onClick = { featsDialogOpen = false }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = Color.White
                        )
                    }
                }
            },
            text = {
                Column {
                    if (feats.isEmpty()) {
                        Text(
                            "Aucun feat",
                            color = Color.White,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 180.dp)
                        ) {
                            items(feats) { feat ->
                                FeatRow(
                                    feat = feat,
                                    onRemove = {
                                        feats = feats.filterNot {
                                            it.equals(feat, ignoreCase = true)
                                        }
                                        onFeatsChange(feats)
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Divider(color = Color(0x33FFFFFF))
                    Spacer(Modifier.height(12.dp))

                    Text(
                        "Ajouter un feat",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge
                    )

                    Spacer(Modifier.height(6.dp))

                    FeatAddSelectorWithButton(
                        artists = artists,
                        placeholder = featPlaceholder,
                        enabled = enabled,
                        dialogBg = dialogBg,
                        currentMainArtist = artistText,
                        currentFeats = feats,
                        onAddFeat = { featToAdd ->
                            val cleaned = featToAdd.trim()
                            if (cleaned.isNotEmpty()) {
                                feats = (feats + cleaned)
                                    .distinctBy { it.lowercase() }
                                onFeatsChange(feats)
                            }
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {},
            containerColor = dialogBg
        )
    }
}

@Composable
private fun FeatRow(
    feat: String,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = feat, color = Color.White)
        Text(
            text = "Supprimer",
            color = Color(0xFFCCCCCC),
            modifier = Modifier.clickable { onRemove() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeatAddSelectorWithButton(
    artists: List<String>,
    placeholder: String,
    enabled: Boolean,
    dialogBg: Color,
    currentMainArtist: String,
    currentFeats: List<String>,
    onAddFeat: (String) -> Unit
) {
    val disabledColor = Color(0x88FFFFFF)

    var text by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var debouncedText by remember { mutableStateOf("") }

    val filtered = remember(debouncedText, artists) {
        if (debouncedText.length < 3) {
            emptyList()
        } else {
            artists.filter {
                it.contains(debouncedText, ignoreCase = true)
            }
        }
    }

    LaunchedEffect(text, enabled) {
        if (!enabled) return@LaunchedEffect

        val query = text.trim()

        if (query.length < 3) {
            expanded = false
            debouncedText = ""
            return@LaunchedEffect
        }

        expanded = true

        delay(250)

        debouncedText = query
    }

    fun tryAddAndClear(value: String) {
        val cleaned = value.trim()
        if (cleaned.isBlank()) return
        if (cleaned.equals(currentMainArtist.trim(), ignoreCase = true)) return
        if (currentFeats.any { it.equals(cleaned, ignoreCase = true) }) return

        onAddFeat(cleaned)
        text = ""
        expanded = false
        debouncedText = ""
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Box {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = {
                        if (text.isEmpty() && placeholder.isNotEmpty()) {
                            Text(placeholder)
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words
                    ),
                    trailingIcon = {
                        IconButton(
                            enabled = enabled,
                            onClick = { expanded = !expanded }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Ouvrir la liste"
                            )
                        }
                    }
                )

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    properties = PopupProperties(focusable = false),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                ) {
                    if (filtered.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Aucun résultat") },
                            onClick = {}
                        )
                    } else {
                        filtered.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    text = item
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        IconButton(
            enabled = enabled && text.trim().isNotEmpty(),
            onClick = { tryAddAndClear(text) }
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Ajouter le feat",
                tint = if (enabled && text.trim().isNotEmpty()) {
                    MaterialTheme.colorScheme.primary
                } else {
                    disabledColor
                }
            )
        }
    }
}

package com.example.veyra.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun PlaylistSelector(
    playlists: List<String>,
    selectedPlaylists: List<String>,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onSelectionChange: (List<String>) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = !expanded },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (playlists.isEmpty()) {
                    "Aucune playlist existante"
                } else if (selectedPlaylists.isEmpty()) {
                    "Ajouter à une ou plusieurs playlists"
                } else {
                    "Playlists : ${selectedPlaylists.joinToString(", ")}"
                }
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 250.dp)
                .background(Color(0xFF2C2C2C))
        ) {
            playlists.forEach { name ->
                val isChecked = selectedPlaylists.contains(name)

                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(name)
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    val newSelection = if (checked) {
                                        (selectedPlaylists + name).distinct()
                                    } else {
                                        selectedPlaylists - name
                                    }
                                    onSelectionChange(newSelection)
                                }
                            )
                        }
                    },
                    onClick = {
                        val newSelection = if (isChecked) {
                            selectedPlaylists - name
                        } else {
                            (selectedPlaylists + name).distinct()
                        }
                        onSelectionChange(newSelection)
                    }
                )
            }
        }
    }
}
package com.example.veyra.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import com.example.veyra.model.data.MusicHolder

@Composable
fun NewArtistOrAlbum(
    navController: NavHostController,
    context: Context,
    isArtistCreated: Boolean
) {
    val artists = MusicHolder.getArtistList()
    val albums = MusicHolder.getAlbumList()

    val identifier = if (isArtistCreated) "artiste" else "album"
    var showDialog by remember { mutableStateOf(false) }
    var createdName by remember { mutableStateOf("") }

    val nameFocusRequester = remember { FocusRequester() }
    LaunchedEffect(showDialog) {
        if (showDialog) {
            // Donne le focus quand la popup apparaît
            nameFocusRequester.requestFocus()
        }
    }

    fun tryCreateObject(): Boolean {
        val name = createdName.trim()
        if (name.isEmpty()) {
            Toast.makeText(context, "Le nom ne peut pas être vide.", Toast.LENGTH_SHORT).show()
            return false
        }

        val alreadyExists = if (isArtistCreated) {
            artists.any { it.equals(name, ignoreCase = true) }
        } else {
            albums.any { it.equals(name, ignoreCase = true) }
        }

        if (alreadyExists) {
            Toast.makeText(context, "Un $identifier avec ce nom existe déjà.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (isArtistCreated) MusicHolder.addArtist(name)
        else MusicHolder.addAlbum(name)

        return true
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Créer un nouvel $identifier",
            style = MaterialTheme.typography.titleMedium
        )

        Button(
            onClick = {
                showDialog = true
            },
            enabled = artists.isNotEmpty() && albums.isNotEmpty()
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "Créer")
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = {
                    showDialog = false
                    createdName = ""
                },
                title = { Text(text = "Nouvel $identifier") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = createdName,
                            onValueChange = { createdName = it },
                            label = { Text("Nom de l'$identifier") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(nameFocusRequester)
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (tryCreateObject()) {
                            showDialog = false
                            val encoded = createdName.toUri()
                            navController.navigate("edit_artist_or_album/$encoded/$isArtistCreated")
                            createdName = ""
                        }
                    }) {
                        Text("Enregistrer")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDialog = false
                            createdName = ""
                        }
                    ) {
                        Text(
                            "Annuler",
                            color = Color.Red
                        )
                    }
                },
                containerColor = Color(0xFF2C2C2C)
            )
        }
    }
}
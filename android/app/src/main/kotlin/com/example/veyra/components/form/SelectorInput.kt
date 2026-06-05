package com.example.veyra.components.form

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorInput(
    list: List<String>,
    placeholder: String,
    enabled: Boolean = true,
    onValueChange: (String) -> Unit = {},
    onRefCreated: ((() -> Unit) -> Unit)? = null
) {
    var text by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var debouncedText by remember { mutableStateOf("") }

    val filteredList = remember(debouncedText, list) {
        if (debouncedText.length < 3) {
            emptyList()
        } else {
            list.filter {
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

    LaunchedEffect(Unit) {
        onRefCreated?.invoke {
            text = ""
            expanded = false
            debouncedText = ""
        }
    }

    Box {
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                onValueChange(it)
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words
            ),
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = {
                if (text.isEmpty() && placeholder.isNotEmpty()) {
                    Text(placeholder)
                }
            },
            trailingIcon = {
                IconButton(
                    enabled = enabled,
                    onClick = {
                        expanded = !expanded
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
            expanded = expanded,
            onDismissRequest = { expanded = false },
            properties = PopupProperties(focusable = false),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp)
        ) {
            if (filteredList.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("Aucun résultat") },
                    onClick = {}
                )
            } else {
                filteredList.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item) },
                        onClick = {
                            text = item
                            onValueChange(item)
                            expanded = false
                            debouncedText = item
                        }
                    )
                }
            }
        }
    }
}

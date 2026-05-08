package com.example.veyra.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

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

    val filteredList = remember(text, list) {
        list.filter { it.contains(text, ignoreCase = true) }
    }

    LaunchedEffect(Unit) {
        onRefCreated?.invoke { text = "" }
    }

    Column {
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                onValueChange(it)
            },
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
                    onClick = { expanded = !expanded }
                ) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Ouvrir la liste")
                }
            }
        )

        if (expanded) {
            Popup(
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 6.dp,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp),
                ) {
                    if (filteredList.isEmpty()) {
                        Text(
                            "Aucun résultat",
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(12.dp)
                        )
                    } else {
                        LazyColumn {
                            items(filteredList) { item ->
                                Text(
                                    text = item,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                        .clickable {
                                            text = item
                                            onValueChange(item)
                                            expanded = false
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

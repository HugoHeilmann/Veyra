package com.example.veyra.components.form

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Composable
fun SearchField(
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit = {},
) {
    var text by remember { mutableStateOf("")}

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.inverseOnSurface,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = text,
            onValueChange = {
                text = it
                onValueChange(it)
            },
            cursorBrush = SolidColor(Color.White),
            singleLine = true,
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground),
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                Box {
                    if (text.isEmpty()) {
                        Text("Rechercher...", color = Color.Gray)
                    }
                    innerTextField()
                }
            }
        )

        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Clear",
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.clickable {
                if (text != "") {
                    text = ""
                    onValueChange("")
                }
            }
        )
    }
}
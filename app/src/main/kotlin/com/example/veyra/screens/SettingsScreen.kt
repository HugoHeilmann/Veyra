package com.example.veyra.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres") }
                // Pas de navigationIcon → pas de flèche de retour
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Text("Paramètres de l'application ici.")
            Button(onClick = { /* changer thème plus tard */ }) {
                Text("Changer thème (exemple)")
            }
        }
    }
}

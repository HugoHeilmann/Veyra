package com.example.veyra.screens

import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Environment
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arthenica.ffmpegkit.FFmpegKit
import com.example.veyra.components.BottomNavigationBar
import com.example.veyra.model.Music
import com.example.veyra.model.MusicHolder
import com.example.veyra.model.MusicMetadata
import com.example.veyra.model.convert.YoutubeApi
import com.example.veyra.model.metadata.MetadataManager
import com.example.veyra.service.DownloadService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(context: Context = androidx.compose.ui.platform.LocalContext.current) {
    var url by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var album by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Idle") }

    val scope = rememberCoroutineScope()

    val isLoading = remember(status) {
        status.startsWith("Extraction") ||
        status.startsWith("Téléchargement") ||
        status.startsWith("Conversion")
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Téléchargement",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp)
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("YouTube URL") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Nom de la musique") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = artist,
                onValueChange = { artist = it },
                label = { Text("Artiste") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = album,
                onValueChange = { album = it },
                label = { Text("Album") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val intent = Intent(context, DownloadService::class.java).apply {
                        putExtra("url", url)
                        putExtra("title", title)
                        putExtra("artist", artist)
                        putExtra("album", album)
                    }
                    context.startService(intent)
                    /*
                    scope.launch {
                        try {
                            status = "Extraction..."
                            val videoId = YoutubeApi.extractVideoId(url)
                            if (videoId == null) {
                                status = "URL invalide"
                                return@launch
                            }

                            val playerJson = withContext(Dispatchers.IO) {
                                YoutubeApi.getPlayerResponse(videoId)
                            } ?: run {
                                status = "Impossible d’obtenir les infos"
                                return@launch
                            }

                            val videoTitle = playerJson["videoDetails"]
                                ?.asJsonObject
                                ?.get("title")
                                ?.asString ?: videoId

                            val audioUrl = YoutubeApi.extractBestAudioUrl(playerJson)
                            if (audioUrl == null) {
                                status = "Pas de flux audio trouvé"
                                return@launch
                            }

                            status = "Téléchargement..."
                            val tempFile = withContext(Dispatchers.IO) {
                                val client = OkHttpClient()
                                val req = Request.Builder().url(audioUrl).build()
                                val resp = client.newCall(req).execute()
                                val file = File.createTempFile("yt_", ".webm")
                                resp.body?.byteStream()?.use { input ->
                                    FileOutputStream(file).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                file
                            }

                            status = "Conversion..."
                            val outputFile = File(
                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                                "${(title.ifBlank { videoTitle }).trim()}.mp3"
                            )

                            // --- Construction des métadonnées ID3 ---
                            val finalTitle = title.ifBlank { videoTitle }
                            val finalArtist = artist.ifBlank { null }
                            val finalAlbum  = album.ifBlank { null }

                            val metaArgs = buildString {
                                append(" -metadata title=\"${esc(finalTitle)}\"")
                                if (finalArtist != null) append(" -metadata artist=\"${esc(finalArtist)}\"")
                                if (finalAlbum  != null) append(" -metadata album=\"${esc(finalAlbum)}\"")
                                append(" -id3v2_version 3 -write_id3v1 1")
                            }

                            MetadataManager.addIfNotExists(
                                context,
                                MusicMetadata(
                                    videoTitle,
                                    finalTitle,
                                    finalArtist ?: "Unknown Artist",
                                    finalAlbum ?: "Unknown Album",
                                    outputFile.absolutePath,
                                )
                            )

                            val cmd = "-y -i \"${tempFile.absolutePath}\" -vn -ar 44100 -ac 2 -b:a 192k$metaArgs \"${outputFile.absolutePath}\""

                            FFmpegKit.executeAsync(cmd) { session ->
                                if (session.returnCode.isValueSuccess) {
                                    // -> Scanner le fichier pour rafraîchir MediaStore
                                    MediaScannerConnection.scanFile(
                                        context,
                                        arrayOf(outputFile.absolutePath),
                                        arrayOf("audio/mpeg")
                                    ) { _, _ ->
                                        // Tu peux soit relire via MediaStore ici, soit juste ajouter à la main
                                    }

                                    // Dans l’app, on ajoute sans valeurs par défaut pour artiste/album
                                    val newMusic = Music(
                                        uri = outputFile.absolutePath,
                                        name = finalTitle,
                                        artist = finalArtist,
                                        album  = finalAlbum
                                    )
                                    MusicHolder.addMusic(newMusic)
                                    status = "✅ Fini : ${outputFile.absolutePath}"
                                } else {
                                    status = "❌ Erreur conversion"
                                }
                            }
                        } catch (e: Exception) {
                            status = "Erreur : ${e.message}"
                            Log.e("VEYRA", "Erreur download", e)
                        }
                    }*/
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Télécharger MP3")
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("Status: $status")

            if (isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

fun esc(s: String) = s.replace("\"", "\\\"")
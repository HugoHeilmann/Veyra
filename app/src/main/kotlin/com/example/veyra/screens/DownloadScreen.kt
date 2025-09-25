package com.example.veyra.screens

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arthenica.ffmpegkit.FFmpegKit
import com.example.veyra.model.Music
import com.example.veyra.model.MusicHolder
import com.example.veyra.model.convert.YoutubeApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

@Composable
fun DownloadScreen(context: Context = androidx.compose.ui.platform.LocalContext.current) {
    var url by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Idle") }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("YouTube URL") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(onClick = {
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

                        val file = File.createTempFile("yt_", ".webm", context.cacheDir)
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
                        "$videoId.mp3"
                    )

                    val cmd = "-y -i \"${tempFile.absolutePath}\" -vn -ar 44100 -ac 2 -b:a 192k \"${outputFile.absolutePath}\""

                    FFmpegKit.executeAsync(cmd) { session ->
                        val state = session.state
                        val returnCode = session.returnCode

                        Log.d("VEYRA_FFMPEG", "State=$state, returnCode=$returnCode")
                        Log.d("VEYRA_FFMPEG", "Logs=${session.allLogsAsString}")

                        if (returnCode.isValueSuccess) {
                            status = "✅ Fini : ${outputFile.absolutePath}"
                            val newMusic = Music(
                                uri = outputFile.absolutePath,
                                name = outputFile.nameWithoutExtension,
                                artist = "YouTube",
                                album = "YouTube Downloads"
                            )
                            MusicHolder.addMusic(newMusic)
                        } else {
                            status = "❌ Erreur conversion"
                        }
                    }
                } catch (e: Exception) {
                    status = "Erreur : ${e.message}"
                    Log.e("VEYRA", "Erreur download", e)
                }
            }
        }) {
            Text("Télécharger MP3")
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Status: $status")
    }
}

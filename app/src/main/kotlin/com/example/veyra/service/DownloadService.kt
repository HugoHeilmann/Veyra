package com.example.veyra.service

import android.app.Service
import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Environment
import android.os.IBinder
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.example.veyra.model.Music
import com.example.veyra.model.MusicHolder
import com.example.veyra.model.metadata.MetadataManager
import com.example.veyra.model.MusicMetadata
import com.example.veyra.model.convert.DownloadBroadcast
import com.example.veyra.model.convert.DownloadHolder
import com.example.veyra.model.convert.YoutubeApi
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class DownloadService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra("url")
        val title = intent?.getStringExtra("title") ?: ""
        val artist = intent?.getStringExtra("artist") ?: ""
        val album = intent?.getStringExtra("album") ?: ""

        if (url != null) {
            scope.launch {
                try {
                    downloadAndConvert(url, title, artist, album)
                } catch (e: Exception) {
                    Log.e("DownloadService", "Erreur pendant le téléchargement", e)
                } finally {
                    stopSelf()
                }
            }
        } else {
            stopSelf()
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun sendStatus(message: String) {
        DownloadHolder.status.value = message

        val intent = Intent(DownloadBroadcast.ACTION_STATUS).apply {
            putExtra(DownloadBroadcast.EXTRA_STATUS, message)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    private suspend fun downloadAndConvert(url: String, title: String, artist: String, album: String) {
        sendStatus("Extraction...")

        val videoId = YoutubeApi.extractVideoId(url) ?: return

        val playerJson = YoutubeApi.getPlayerResponse(videoId) ?: return
        val videoTitle = playerJson["videoDetails"]
            ?.asJsonObject
            ?.get("title")
            ?.asString ?: videoId

        val audioUrl = YoutubeApi.extractBestAudioUrl(playerJson) ?: return

        sendStatus("Téléchargement…")

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

        sendStatus("Conversion…")

        val outputFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            "${(title.ifBlank { videoTitle }).trim()}.mp3"
        )

        val finalTitle = title.ifBlank { videoTitle }
        val finalArtist = artist.ifBlank { null }
        val finalAlbum = album.ifBlank { null }

        val metaArgs = buildString {
            append(" -metadata title=\"${esc(finalTitle)}\"")
            if (finalArtist != null) append(" -metadata artist=\"${esc(finalArtist)}\"")
            if (finalAlbum != null) append(" -metadata album=\"${esc(finalAlbum)}\"")
            append(" -id3v2_version 3 -write_id3v1 1")
        }

        MetadataManager.addIfNotExists(
            this,
            MusicMetadata(
                videoTitle,
                finalTitle,
                finalArtist ?: "Unknown",
                finalAlbum ?: "Unknown Album",
                outputFile.absolutePath,
            )
        )

        val cmd = "-y -i \"${tempFile.absolutePath}\" -vn -ar 44100 -ac 2 -b:a 192k$metaArgs \"${outputFile.absolutePath}\""

        FFmpegKit.executeAsync(cmd) { session ->
            if (session.returnCode.isValueSuccess) {
                MediaScannerConnection.scanFile(
                    this,
                    arrayOf(outputFile.absolutePath),
                    arrayOf("audio/mpeg"),
                    null
                )

                val newMusic = Music(
                    uri = outputFile.absolutePath,
                    name = finalTitle,
                    artist = finalArtist,
                    album = finalAlbum
                )
                MusicHolder.addMusic(newMusic)
                sendStatus("✅ Fini : ${outputFile.absolutePath}")
            } else {
                sendStatus("❌ Erreur conversion")
            }
        }
    }

    private fun esc(s: String) = s.replace("\"", "\\\"")
}

package com.example.veyra.service

import android.app.Service
import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Environment
import android.os.IBinder
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.example.veyra.components.Playlist
import com.example.veyra.model.Music
import com.example.veyra.model.data.MusicHolder
import com.example.veyra.model.metadata.MetadataManager
import com.example.veyra.model.metadata.MusicMetadata
import com.example.veyra.model.convert.DownloadBroadcast
import com.example.veyra.model.convert.DownloadHolder
import com.example.veyra.model.convert.YoutubeApi
import com.example.veyra.model.metadata.PlaylistManager
import kotlinx.coroutines.*
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class DownloadService : Service() {

    companion object {
        const val ACTION_CANCEL = "com.example.veyra.service.action.CANCEL_DOWNLOAD"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Volatile
    private var currentCall: Call? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            sendStatus("❌ Téléchargement annulé")
            currentCall?.cancel()
            scope.coroutineContext.cancelChildren()
            stopSelf()
            return START_NOT_STICKY
        }

        val url = intent?.getStringExtra("url")
        val title = intent?.getStringExtra("title") ?: ""
        val artist = intent?.getStringExtra("artist") ?: ""
        val album = intent?.getStringExtra("album") ?: ""
        val playlists = intent?.getStringArrayListExtra("playlists") ?: arrayListOf()

        if (url == null || !isUrlFormatted(url)) {
            sendStatus("❌ URL invalide")
            stopSelf()
        } else {
            scope.launch {
                try {
                    downloadAndConvert(url, title, artist, album, playlists)
                } catch (e: Exception) {
                    Log.e("DownloadService", "Erreur pendant le téléchargement", e)
                } finally {
                    stopSelf()
                }
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun isUrlFormatted(url: String): Boolean {
        return url.startsWith("https://youtu.be/")
                || url.startsWith("https://m.youtube.com/watch?v=")
                || url.startsWith("https://www.youtube.com/watch?v=")
    }

    private fun sendStatus(message: String) {
        DownloadHolder.status.value = message

        val regex = Regex("""(\d{1,3})%""")
        val match = regex.find(message)

        if (match != null) {
            val percent = match.groupValues[1].toInt().coerceIn(0, 100)
            DownloadHolder.progress.floatValue = percent / 100f
        } else if (message.startsWith("Extraction")){
            DownloadHolder.progress.floatValue = 0.05f
        } else if (message.startsWith("Téléchargement")) {
            // le download ajuste la barre via les %
        } else if (message.startsWith("Conversion")) {
            DownloadHolder.progress.floatValue = 0.95f
        } else if (message.startsWith("✅")) {
            DownloadHolder.progress.floatValue = 1f
        } else if (message.startsWith("❌")) {
            DownloadHolder.progress.floatValue = 0f
        }

        val intent = Intent(DownloadBroadcast.ACTION_STATUS).apply {
            putExtra(DownloadBroadcast.EXTRA_STATUS, message)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    private suspend fun downloadAndConvert(url: String, title: String, artist: String, album: String, playlists: ArrayList<String>) {
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

            val call = client.newCall(req)
            currentCall = call

            val resp = call.execute()

            try {
                val totalBytes = resp.body?.contentLength() ?: -1
                val input = resp.body?.byteStream() ?: return@withContext null
                val file = File.createTempFile("yt_", ".webm")

                FileOutputStream(file).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var bytesCopied = 0L
                    var lastProgress = 0

                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        bytesCopied += read

                        if (totalBytes > 0) {
                            val progress = ((bytesCopied * 100) / totalBytes).toInt()
                            if (progress > lastProgress) {
                                lastProgress = progress
                                sendStatus("Téléchargement... $progress%")
                            }
                        }
                    }
                }
                file
            } finally {
                resp.close()
                currentCall = null
            }
        } ?: run {
            if (!DownloadHolder.status.value.startsWith("❌ Téléchargement annulé")) {
                sendStatus("❌ Échec du téléchargement.")
            }
            return
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

        playlists.forEach { name ->
            PlaylistManager.addMusicToPlaylist(this@DownloadService, name, outputFile.absolutePath)
        }

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

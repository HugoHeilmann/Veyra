package com.example.veyra.service

import android.app.Service
import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Environment
import android.os.IBinder
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.example.veyra.R
import com.example.veyra.model.Music
import com.example.veyra.model.convert.DownloadBroadcast
import com.example.veyra.model.convert.DownloadHolder
import com.example.veyra.model.convert.NewPipeResolver
import com.example.veyra.model.data.MusicHolder
import com.example.veyra.model.metadata.MetadataManager
import com.example.veyra.model.metadata.MusicMetadata
import com.example.veyra.model.metadata.PlaylistManager
import kotlinx.coroutines.*
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class DownloadService : Service() {

    companion object {
        const val ACTION_CANCEL = "com.example.veyra.service.action.CANCEL_DOWNLOAD"
        private const val TAG = "DownloadService"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val httpClient by lazy { OkHttpClient() }

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
        val coverPath = intent?.getStringExtra("coverPath")
        val title = intent?.getStringExtra("title") ?: ""
        val artist = intent?.getStringExtra("artist") ?: ""
        val album = intent?.getStringExtra("album") ?: ""
        val playlists = intent?.getStringArrayListExtra("playlists") ?: arrayListOf()

        if (url == null || !isUrlFormatted(url)) {
            sendStatus("❌ URL invalide")
            stopSelf()
            return START_NOT_STICKY
        }

        scope.launch {
            try {
                downloadAndConvert(url, coverPath, title, artist, album, playlists)
            } catch (ce: CancellationException) {
                Log.i(TAG, "Cancelled", ce)
            } catch (e: Exception) {
                Log.e(TAG, "Erreur pendant le téléchargement", e)

                if (DownloadHolder.state.intValue != -1) {
                    val message = when (e) {
                        is UnknownHostException ->
                            "❌ Connexion impossible : vérifie ta connexion internet."

                        is SocketTimeoutException ->
                            "❌ Délai dépassé : le téléchargement a mis trop longtemps à répondre."

                        is ConnectException ->
                            "❌ Connexion refusée : le serveur distant est inaccessible."

                        is IOException ->
                            "❌ Erreur réseau pendant le téléchargement."

                        else ->
                            "❌ Le téléchargement a échoué. Vérifie l’URL ou réessaie plus tard."
                    }

                    sendStatus(message)
                }
            } finally {
                stopSelf()
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
        return url.startsWith("https://youtu.be/") ||
                url.startsWith("https://m.youtube.com/watch?v=") ||
                url.startsWith("https://www.youtube.com/watch?v=") ||
                url.startsWith("https://music.youtube.com/watch?v=") ||
                url.contains("youtube.com/shorts/")
    }

    private fun sendStatus(message: String) {
        DownloadHolder.status.value = message

        val regex = Regex("""(\d{1,3})%""")
        val match = regex.find(message)

        if (match != null) {
            val raw = match.groupValues[1].toInt().coerceIn(0, 100)
            val percent = 10 + (raw / 100f) * (90 - 10)
            DownloadHolder.progress.floatValue = percent / 100f
            DownloadHolder.state.intValue = 0
            DownloadHolder.isLoading.value = true
        } else if (message.startsWith("Extraction")) {
            DownloadHolder.progress.floatValue = 0.05f
            DownloadHolder.state.intValue = 0
            DownloadHolder.isLoading.value = true
        } else if (message.startsWith("Téléchargement")) {
            DownloadHolder.state.intValue = 0
            DownloadHolder.isLoading.value = true
        } else if (message.startsWith("Conversion")) {
            DownloadHolder.progress.floatValue = 0.95f
            DownloadHolder.state.intValue = 0
            DownloadHolder.isLoading.value = true
        } else if (message.startsWith("✅")) {
            DownloadHolder.progress.floatValue = 1f
            DownloadHolder.state.intValue = 1
            DownloadHolder.isLoading.value = false
        } else if (message.startsWith("❌")) {
            DownloadHolder.progress.floatValue = 0f
            DownloadHolder.state.intValue = -1
            DownloadHolder.isLoading.value = false
        }

        val intent = Intent(DownloadBroadcast.ACTION_STATUS).apply {
            putExtra(DownloadBroadcast.EXTRA_STATUS, message)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    private suspend fun downloadAndConvert(
        url: String,
        coverPath: String?,
        title: String,
        artist: String,
        album: String,
        playlists: ArrayList<String>
    ) {
        sendStatus("Extraction...")

        val resolved = NewPipeResolver.resolve(url)
        if (resolved == null) {
            sendStatus("❌ Extraction impossible (NewPipe)")
            return
        }

        val videoTitle = resolved.title
        val audioUrl = resolved.audioUrl

        sendStatus("Téléchargement…")

        val tempFile = downloadToTempFile(audioUrl) ?: run {
            if (!DownloadHolder.status.value.startsWith("❌ Téléchargement annulé")) {
                if (!DownloadHolder.status.value.startsWith("❌")) {
                    sendStatus("❌ Échec du téléchargement.")
                }
            }
            return
        }

        sendStatus("Conversion…")

        val finalTitle = title.ifBlank { videoTitle }
        val finalArtist = artist.ifBlank { null }
        val finalAlbum = album.ifBlank { null }

        val outputFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            "${safeFileName(finalTitle)}.mp3"
        )

        val metaArgs = buildString {
            append(" -metadata title=\"${esc(finalTitle)}\"")
            if (finalArtist != null) append(" -metadata artist=\"${esc(finalArtist)}\"")
            if (finalAlbum != null) append(" -metadata album=\"${esc(finalAlbum)}\"")
            append(" -id3v2_version 3 -write_id3v1 1")
        }

        val cmd =
            "-y -i \"${tempFile.absolutePath}\" -vn -ar 44100 -ac 2 -b:a 192k$metaArgs \"${outputFile.absolutePath}\""

        val session = try {
            withContext(Dispatchers.IO) {
                FFmpegKit.execute(cmd)
            }
        } catch (e: Exception) {
            Log.e(TAG, "FFmpeg execute exception", e)
            sendStatus("❌ Erreur conversion (exception)")
            safeDelete(tempFile)
            return
        }

        val rc = session.returnCode
        val logs = session.allLogsAsString

        if (rc != null && rc.isValueSuccess) {
            MediaScannerConnection.scanFile(
                this,
                arrayOf(outputFile.absolutePath),
                arrayOf("audio/mpeg"),
                null
            )

            try {
                MetadataManager.addIfNotExists(
                    this,
                    MusicMetadata(
                        fileName = videoTitle,
                        title = finalTitle,
                        artist = finalArtist ?: "Unknown",
                        album = finalAlbum ?: "Unknown Album",
                        filePath = outputFile.absolutePath,
                        coverPath = coverPath
                    )
                )

                playlists.forEach { name ->
                    PlaylistManager.addMusicToPlaylist(
                        this@DownloadService,
                        name,
                        outputFile.absolutePath
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Post-success metadata/playlist error", e)
            }

            val newMusic = Music(
                uri = outputFile.absolutePath,
                name = finalTitle,
                artist = finalArtist,
                album = finalAlbum,
                image = if (coverPath == null) R.drawable.default_album_cover else 0,
                coverPath = coverPath
            )
            MusicHolder.addMusic(newMusic)

            sendStatus("✅ Fini : ${outputFile.absolutePath}")
        } else {
            Log.e(TAG, "FFmpeg failed rc=$rc\n---logs---\n${logs.takeLast(4000)}")
            sendStatus("❌ Erreur conversion (ffmpeg)")
        }

        safeDelete(tempFile)
    }

    private suspend fun downloadToTempFile(audioUrl: String): File? = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url(audioUrl)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Accept", "*/*")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Referer", "https://www.youtube.com/")
            .build()

        val call = httpClient.newCall(req)
        currentCall = call

        val resp = try {
            call.execute()
        } catch (e: IOException) {
            if (call.isCanceled()) {
                sendStatus("❌ Téléchargement annulé")
                return@withContext null
            }
            Log.e(TAG, "HTTP execute error", e)
            sendStatus("❌ Erreur réseau")
            return@withContext null
        }

        try {
            val code = resp.code
            val ctype = resp.header("Content-Type") ?: "?"

            if (!resp.isSuccessful) {
                val peek = try { resp.peekBody(512).string() } catch (_: Exception) { "" }
                sendStatus("❌ Téléchargement HTTP $code")
                Log.e(TAG, "HTTP $code Content-Type=$ctype peek=${peek.take(200)}")
                return@withContext null
            }

            if (!ctype.startsWith("audio/") && !ctype.startsWith("video/") && !ctype.contains("octet-stream")) {
                val peek = try { resp.peekBody(512).string() } catch (_: Exception) { "" }
                sendStatus("❌ Réponse non-audio ($ctype)")
                Log.e(TAG, "Bad Content-Type=$ctype peek=${peek.take(200)}")
                return@withContext null
            }

            val body = resp.body ?: run {
                sendStatus("❌ Réponse vide")
                return@withContext null
            }

            val totalBytes = body.contentLength()
            val input = body.byteStream()

            val file = File.createTempFile("yt_", ".webm")

            FileOutputStream(file).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var bytesCopied = 0L
                var lastProgress = -1

                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    output.write(buffer, 0, read)
                    bytesCopied += read

                    if (totalBytes > 0) {
                        val progress = ((bytesCopied * 100) / totalBytes).toInt().coerceIn(0, 100)
                        if (progress != lastProgress) {
                            lastProgress = progress
                            sendStatus("Téléchargement... $progress%")
                        }
                    }
                }
            }

            if (file.length() < 32_000) {
                val head = try {
                    file.inputStream().buffered().readBytes().decodeToString()
                } catch (_: Exception) { "" }

                Log.e(TAG, "Downloaded file suspiciously small (${file.length()} bytes). Head=${head.take(200)}")
                sendStatus("❌ Flux invalide (trop petit) — YouTube a peut-être bloqué")
                safeDelete(file)
                return@withContext null
            }

            file
        } finally {
            resp.close()
            currentCall = null
        }
    }

    private fun esc(s: String) = s.replace("\"", "\\\"")

    private fun safeFileName(s: String): String {
        val cleaned = s.trim()
            .replace(Regex("""[\\/:*?"<>|]"""), "_")
            .replace(Regex("""\s+"""), " ")
            .take(120)
        return if (cleaned.isBlank()) "audio" else cleaned
    }

    private fun safeDelete(f: File?) {
        try {
            if (f != null && f.exists()) f.delete()
        } catch (_: Exception) { }
    }
}

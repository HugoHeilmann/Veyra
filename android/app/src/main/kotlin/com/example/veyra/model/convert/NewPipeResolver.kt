package com.example.veyra.model.convert

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.stream.StreamInfo

object NewPipeResolver {

    private const val TAG = "NewPipeResolver"

    @Volatile
    private var isInit = false

    data class Resolved(
        val audioUrl: String,
        val title: String
    )

    /**
     * À appeler une fois au démarrage idéalement (Application/MainActivity).
     * Ici on le garde pour init "de secours" si le Service démarre avant.
     */
    fun ensureInit() {
        if (isInit) return
        synchronized(this) {
            if (isInit) return
            try {
                NewPipe.init(OkHttpDownloader(), Localization("en", "US"))
                isInit = true
                Log.i(TAG, "NewPipe initialized (fallback)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to init NewPipe", e)
            }
        }
    }

    suspend fun resolve(url: String): Resolved? = withContext(Dispatchers.IO) {
        ensureInit()

        val normalized = normalizeYoutubeUrl(url)

        try {
            val service = NewPipe.getServiceByUrl(normalized)
            val info = StreamInfo.getInfo(service, normalized)

            Log.d(TAG, "Title=${info.name}")
            Log.d(TAG, "audioStreams=${info.audioStreams.size}")
            Log.d(TAG, "videoStreams=${info.videoStreams.size}")
            Log.d(TAG, "videoOnlyStreams=${info.videoOnlyStreams.size}")

            val bestAudio = info.audioStreams
                .filter { !it.url.isNullOrBlank() }
                .maxByOrNull { it.bitrate }

            val fallbackVideoWithAudio = info.videoStreams
                .filter { !it.url.isNullOrBlank()}
                .filter { !it.isVideoOnly }
                .maxByOrNull { it.bitrate }

            val streamUrl = bestAudio?.url ?: fallbackVideoWithAudio?.url

            if (streamUrl.isNullOrBlank()) {
                Log.e(
                    TAG,
                    "No usable stream found for url=$normalized " +
                            "audio=${info.audioStreams.size}, " +
                            "video=${info.videoStreams.size}, " +
                            "videoOnly=${info.videoOnlyStreams.size}"
                )

                return@withContext null
            }

            Resolved(
                audioUrl = streamUrl,
                title = info.name ?: "Unknown title"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Resolve failed for url=$normalized (original=$url)", e)
            null
        }
    }

    /**
     * Rend l’URL plus “stable” pour NewPipe.
     */
    private fun normalizeYoutubeUrl(url: String): String {
        val u = url.trim()

        // youtu.be/<id>?t=...
        if (u.startsWith("https://youtu.be/") || u.startsWith("http://youtu.be/")) {
            val idPart = u.substringAfter("youtu.be/").substringBefore("?").substringBefore("&")
            if (idPart.isNotBlank()) {
                return "https://www.youtube.com/watch?v=$idPart"
            }
        }

        // youtube.com/shorts/<id>?...
        if (u.contains("youtube.com/shorts/")) {
            val idPart = u.substringAfter("youtube.com/shorts/").substringBefore("?").substringBefore("&").substringBefore("/")
            if (idPart.isNotBlank()) {
                return "https://www.youtube.com/watch?v=$idPart"
            }
        }

        // music.youtube.com -> youtube.com (souvent plus compatible)
        if (u.startsWith("https://music.youtube.com/") || u.startsWith("http://music.youtube.com/")) {
            return u.replace("music.youtube.com", "www.youtube.com")
        }

        return u
    }
}

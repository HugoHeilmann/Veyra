package com.example.veyra.model.convert

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Response

class OkHttpDownloader : Downloader() {
    private val client = OkHttpClient()

    override fun execute(request: org.schabi.newpipe.extractor.downloader.Request): Response {
        val builder = Request.Builder().url(request.url())

        // Ajout des headers
        for ((key, values) in request.headers()) {
            for (value in values) {
                builder.addHeader(key, value)
            }
        }

        builder.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        builder.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
        builder.addHeader("Accept-Language", "en-US,en;q=0.9")

        val response = client.newCall(builder.build()).execute()
        val bodyString = response.body?.string() ?: ""

        Log.d("VEYRA_HTTP", "URL=${request.url()} | CODE=${response.code}")
        Log.d("VEYRA_HTTP", "BODY=${bodyString.take(500)}")

        return Response(
            response.code,
            response.message,
            response.headers.toMultimap(),
            bodyString,
            response.request.url.toString()
        )
    }
}

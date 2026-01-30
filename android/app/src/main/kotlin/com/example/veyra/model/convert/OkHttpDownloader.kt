package com.example.veyra.model.convert

import okhttp3.OkHttpClient
import okhttp3.RequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.nio.charset.Charset

class OkHttpDownloader : Downloader() {

    private val client = OkHttpClient()

    override fun execute(request: Request): Response {
        val url = request.url()
        val method = request.httpMethod()

        val builder = okhttp3.Request.Builder()
            .url(url)

        // 1️⃣ Headers demandés par NewPipe (Map<String, List<String>>)
        val headers = request.headers()
        for ((k, values) in headers) {
            builder.removeHeader(k)
            for (v in values) {
                builder.addHeader(k, v)
            }
        }

        // 2️⃣ Consent EU (sinon HTML au lieu de JSON)
        val hasCookie = headers.keys.any { it.equals("Cookie", ignoreCase = true) }
        if (!hasCookie && (url.contains("youtube.com") || url.contains("googleapis.com"))) {
            builder.addHeader("Cookie", "CONSENT=YES+1")
        }

        // 3️⃣ Body (POST Innertube)
        val data = request.dataToSend()
        val body: RequestBody? =
            if (data != null) {
                RequestBody.create(null, data)
            } else {
                if (method.equals("POST", true) ||
                    method.equals("PUT", true) ||
                    method.equals("PATCH", true)
                ) {
                    RequestBody.create(null, ByteArray(0))
                } else null
            }

        builder.method(method, body)

        val resp = client.newCall(builder.build()).execute()

        resp.use { r ->
            val responseBytes = r.body?.bytes() ?: ByteArray(0)

            val charset: Charset = try {
                r.body?.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
            } catch (_: Exception) {
                Charsets.UTF_8
            }

            val responseString = try {
                String(responseBytes, charset)
            } catch (_: Exception) {
                String(responseBytes, Charsets.UTF_8)
            }

            return Response(
                r.code,
                r.message,
                r.headers.toMultimap(),
                responseString,
                r.request.url.toString()
            )
        }
    }
}

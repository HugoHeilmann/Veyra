package com.example.veyra.model.convert

import android.util.Log
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object YoutubeApi {
    private val client = OkHttpClient()

    fun getPlayerResponse(videoId: String): JsonObject? {
        val jsonBody = """
        {
          "videoId": "$videoId",
          "context": {
            "client": {
              "clientName": "ANDROID",
              "clientVersion": "19.09.37"
            }
          }
        }
        """.trimIndent()

        val body = jsonBody.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("https://www.youtube.com/youtubei/v1/player?key=AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8")
            .post(body)
            .addHeader("User-Agent", "com.google.android.youtube/19.09.37 (Linux; U; Android 12)")
            .addHeader("Accept-Language", "en-US,en;q=0.9")
            .build()

        val response = client.newCall(request).execute()
        val bodyString = response.body?.string()

        if (bodyString != null) {
            val chunkSize = 5000
            for (i in bodyString.indices step chunkSize) {
                val end = (i + chunkSize).coerceAtMost(bodyString.length)
                Log.d("VEYRA_JSON", bodyString.substring(i, end))
            }
        }

        return if (bodyString != null) JsonParser.parseString(bodyString).asJsonObject else null
    }

    fun extractBestAudioUrl(playerJson: JsonObject): String? {
        val formats = playerJson["streamingData"]
            ?.asJsonObject
            ?.get("adaptiveFormats")
            ?.asJsonArray ?: return null

        var bestUrl: String? = null
        var bestBitrate = 0

        for (format in formats) {
            val obj = format.asJsonObject
            val mime = obj["mimeType"]?.asString ?: ""
            if (mime.startsWith("audio")) {
                val bitrate = obj["bitrate"]?.asInt ?: 0
                if (bitrate > bestBitrate) {
                    bestBitrate = bitrate
                    bestUrl = obj["url"]?.asString
                }
            }
        }

        return bestUrl
    }

    fun extractVideoId(url: String): String? {
        val regex = Regex("(?:v=|youtu\\.be/)([a-zA-Z0-9_-]{11})")
        return regex.find(url)?.groupValues?.get(1)
    }
}
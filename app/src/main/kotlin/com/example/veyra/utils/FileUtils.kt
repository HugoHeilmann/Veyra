package com.example.veyra.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object FileUtils {

    fun copyImageToInternalStorage(context: Context, uri: Uri, musicId: String): String? {
        return try {
            val coversDir = File(context.filesDir, "covers")
            if (!coversDir.exists()) coversDir.mkdirs()

            val safeName = "${musicId.hashCode()}_cover.jpg"
            val destFile = File(coversDir, safeName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: run {
                return null
            }

            destFile.absolutePath

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
import android.content.Context
import com.example.vibra.model.MusicMetadata
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

object MetadataManager {

    private const val FILE_NAME = "metadata.json"
    private val gson = Gson()

    private fun getFile(context: Context): File {
        return File(context.filesDir, FILE_NAME)
    }

    fun initializeIfNeeded(context: Context) {
        val file = getFile(context)
        if (!file.exists()) {
            file.writeText("[]")
        }
    }

    // Lire tout le JSON comme une liste d’objets MusicMetadata
    fun readAll(context: Context): MutableList<MusicMetadata> {
        val file = getFile(context)
        if (!file.exists()) initializeIfNeeded(context)
        val json = file.readText()
        val type = object : TypeToken<MutableList<MusicMetadata>>() {}.type
        return gson.fromJson(json, type) ?: mutableListOf()
    }

    // Sauvegarder la liste complète
    fun writeAll(context: Context, list: List<MusicMetadata>) {
        val file = getFile(context)
        file.writeText(gson.toJson(list))
    }

    // Add entry if does not exists
    fun addIfNotExists(context: Context, metadata: MusicMetadata) {
        val list = readAll(context)

        // verify by fileName
        val exists = list.any { it.fileName == metadata.fileName }

        if (!exists) {
            list.add(metadata)
            writeAll(context, list)
        }
    }

    fun updateMetadata(context: Context, filePath: String, title: String, artist: String, album: String) {
        val list = readAll(context)
        val index = list.indexOfFirst { it.filePath == filePath }

        if (index >= 0) {
            val existing = list[index]
            list[index] = existing.copy(
                title = title,
                artist = artist,
                album = album
            )
            writeAll(context, list)
        }
    }

    // Récupérer une entrée par chemin
    fun getByPath(context: Context, filePath: String): MusicMetadata? {
        return readAll(context).find { it.filePath == filePath }
    }
}

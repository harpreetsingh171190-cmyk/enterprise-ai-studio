package ai.mlc.mlcchat.enterprise

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/** App-private GGUF catalog; imported models are not automatically executable. */
data class GgufModel(val fileName: String, val bytes: Long, val sha256: String)
class GgufCatalog(private val context: Context) {
    private val root = File(context.filesDir, "gguf-models").apply { mkdirs() }
    private val index = File(root, "catalog.json")
    fun list(): List<GgufModel> = runCatching {
        val array = JSONArray(index.readText())
        (0 until array.length()).map { i ->
            val j = array.getJSONObject(i)
            GgufModel(j.getString("name"), j.getLong("bytes"), j.getString("sha256"))
        }.filter { File(root, it.fileName).isFile }
    }.getOrDefault(emptyList())

    fun import(uri: Uri): GgufModel {
        val displayName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val col = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (col >= 0 && cursor.moveToFirst()) cursor.getString(col) else null
        } ?: "imported.gguf"
        require(displayName.endsWith(".gguf", ignoreCase = true)) { "Select a GGUF file" }
        val safe = displayName.substringAfterLast('/').substringAfterLast('\\').replace(Regex("[^A-Za-z0-9._-]"), "_")
        require(safe != "." && safe != ".." && safe.length <= 128)
        val destination = File(root, safe)
        require(!destination.exists()) { "Model already exists" }
        val temp = File.createTempFile("model-", ".partial", root)
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            var total = 0L
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(temp).use { output ->
                    val buffer = ByteArray(1024 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= 16L * 1024 * 1024 * 1024) { "Model too large" }
                        digest.update(buffer, 0, count)
                        output.write(buffer, 0, count)
                    }
                }
            } ?: error("Cannot read selected file")
            require(total >= 8) { "Invalid GGUF file" }
            temp.inputStream().use { stream ->
                val header = ByteArray(4)
                require(stream.read(header) == 4 && String(header, Charsets.US_ASCII) == "GGUF") { "Not a GGUF file" }
            }
            require(temp.renameTo(destination)) { "Could not finish import" }
            val model = GgufModel(safe, total, digest.digest().joinToString("") { "%02x".format(it) })
            save(list() + model)
            return model
        } finally { temp.delete() }
    }
    fun delete(model: GgufModel) {
        val file = File(root, model.fileName)
        require(file.canonicalFile.parentFile == root.canonicalFile)
        require(!file.exists() || file.delete())
        save(list().filterNot { it.fileName == model.fileName })
    }
    private fun save(models: List<GgufModel>) {
        val array = JSONArray()
        models.forEach { array.put(JSONObject().put("name", it.fileName).put("bytes", it.bytes).put("sha256", it.sha256)) }
        val temp = File(root, "catalog.tmp")
        temp.writeText(array.toString())
        require(temp.renameTo(index)) { "Could not update catalog" }
    }
}

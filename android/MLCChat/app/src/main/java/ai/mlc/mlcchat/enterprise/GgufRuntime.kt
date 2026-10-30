package ai.mlc.mlcchat.enterprise

import android.app.ActivityManager
import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GgufRuntime(private val context: Context) {
    private var loaded: String? = null
    private val root = File(context.filesDir, "gguf-models")
    fun modelFile(model: GgufModel): File {
        val file = File(root, model.fileName).canonicalFile
        require(file.parentFile == root.canonicalFile && file.isFile)
        return file
    }
    fun memoryCheck(file: File, contextSize: Int): String? {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(info)
        if (info.lowMemory) return "Android reports low memory; close other apps."
        val reserve = 700L * 1024 * 1024
        val kvEstimate = contextSize.toLong() * 256 * 1024 // heuristic, not guaranteed
        val required = file.length() + kvEstimate + reserve
        if (info.availMem < required) return "Not enough free RAM (estimated need ${required / 1048576} MB)."
        return null
    }
    suspend fun load(model: GgufModel, contextSize: Int, threads: Int): String = withContext(Dispatchers.IO) {
        require(GgufNative.available) { "Native llama.cpp library not included in this APK." }
        require(contextSize in 256..8192 && threads in 1..16)
        val file = modelFile(model)
        memoryCheck(file, contextSize)?.let { error(it) }
        if (loaded != null) GgufNative.unload()
        loaded = null
        check(GgufNative.load(file.absolutePath, contextSize, threads)) { "Native model load failed." }
        loaded = model.fileName
        "Loaded ${model.fileName} (CPU)"
    }
    suspend fun generate(prompt: String, maxTokens: Int, temperature: Float, topP: Float, onToken: (String)->Unit): Int =
        withContext(Dispatchers.IO) {
            check(loaded != null) { "Load a model first." }
            require(maxTokens in 1..2048 && temperature in 0f..2f && topP > 0f && topP <= 1f)
            val decoder = Utf8TokenDecoder()
            val count = GgufNative.generate(prompt, maxTokens, temperature, topP, object : TokenReceiver {
                override fun onBytes(bytes: ByteArray) {
                    val text = decoder.append(bytes)
                    if (text.isNotEmpty()) onToken(text)
                }
            })
            val tail = decoder.finish()
            if (tail.isNotEmpty()) onToken(tail)
            count
        }
    fun stop() { if (GgufNative.available) GgufNative.cancel() }
    suspend fun unload() = withContext(Dispatchers.IO) {
        if (GgufNative.available) GgufNative.unload()
        loaded = null
    }
}

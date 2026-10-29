package ai.mlc.mlcchat.enterprise

interface TokenReceiver { fun onBytes(bytes: ByteArray) }

/** JNI is only available in APKs built with a compatible llama.cpp checkout. */
object GgufNative {
    val available: Boolean = runCatching { System.loadLibrary("enterprise_gguf") }.isSuccess
    external fun load(path: String, contextSize: Int, threads: Int): Boolean
    external fun generate(prompt: String, maxTokens: Int, temperature: Float, topP: Float, receiver: TokenReceiver): Int
    external fun formatChat(system: String, user: String): String?
    external fun cancel()
    external fun unload()
}

package ai.mlc.mlcchat.core.inference

class InferenceRouter(backends: List<InferenceBackend>) {
    private val available = backends.associateBy { it.id }
    init { require(available.size == backends.size) { "Duplicate backend ID" } }
    fun backendFor(format: String): InferenceBackend {
        val id = when (format.lowercase()) {
            "gguf" -> "llama.cpp"
            else -> throw IllegalArgumentException("Unsupported model format: $format")
        }
        return available[id] ?: error("Backend not installed: $id")
    }
}

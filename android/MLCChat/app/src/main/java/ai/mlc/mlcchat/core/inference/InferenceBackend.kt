package ai.mlc.mlcchat.core.inference

data class GenerationOptions(val temperature: Float = 0.7f, val topP: Float = 0.95f, val maxTokens: Int = 512) {
    init { require(temperature in 0f..2f && topP > 0f && topP <= 1f && maxTokens in 1..32768) }
}
data class BackendCapabilities(val text: Boolean = true, val vision: Boolean = false, val tools: Boolean = false)
interface InferenceBackend {
    val id: String
    val capabilities: BackendCapabilities
    suspend fun isAvailable(): Boolean
    suspend fun load(modelPath: String)
    suspend fun generate(prompt: String, options: GenerationOptions, onToken: (String) -> Unit)
    suspend fun unload()
}

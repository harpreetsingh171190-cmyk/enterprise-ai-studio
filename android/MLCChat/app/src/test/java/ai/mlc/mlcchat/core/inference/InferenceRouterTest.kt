package ai.mlc.mlcchat.core.inference
import org.junit.Assert.assertEquals
import org.junit.Test
class InferenceRouterTest {
    private class Fake(override val id: String): InferenceBackend {
        override val capabilities = BackendCapabilities()
        override suspend fun isAvailable() = true
        override suspend fun load(modelPath: String) {}
        override suspend fun generate(prompt: String, options: GenerationOptions, onToken: (String)->Unit) {}
        override suspend fun unload() {}
    }
    @Test fun routes() {
        val router = InferenceRouter(listOf(Fake("llama.cpp")))
        assertEquals("llama.cpp",router.backendFor("GGUF").id)
    }
    @Test(expected=IllegalArgumentException::class) fun rejectsMlcNow() { InferenceRouter(emptyList()).backendFor("tvm") }
    @Test(expected=IllegalArgumentException::class) fun rejectsUnknown() { InferenceRouter(emptyList()).backendFor("unknown") }
}

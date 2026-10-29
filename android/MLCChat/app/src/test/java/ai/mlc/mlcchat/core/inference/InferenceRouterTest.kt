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
        val router = InferenceRouter(listOf(Fake("mlc"),Fake("llama.cpp")))
        assertEquals("mlc",router.backendFor("TVM").id)
        assertEquals("llama.cpp",router.backendFor("GGUF").id)
    }
    @Test(expected=IllegalArgumentException::class) fun rejectsUnknown() { InferenceRouter(emptyList()).backendFor("unknown") }
}

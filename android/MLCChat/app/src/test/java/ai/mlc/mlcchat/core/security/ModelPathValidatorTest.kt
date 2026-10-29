package ai.mlc.mlcchat.core.security
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
class ModelPathValidatorTest {
    @Test fun acceptsName() { assertEquals("qwen-3b",ModelPathValidator.resolve(File(System.getProperty("java.io.tmpdir")),"qwen-3b").name) }
    @Test(expected=IllegalArgumentException::class) fun rejectsTraversal() { ModelPathValidator.resolve(File(System.getProperty("java.io.tmpdir")),"../escape") }
}

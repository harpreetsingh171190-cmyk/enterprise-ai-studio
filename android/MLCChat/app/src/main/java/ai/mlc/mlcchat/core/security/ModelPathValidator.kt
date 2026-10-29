package ai.mlc.mlcchat.core.security

import java.io.File
object ModelPathValidator {
    fun resolve(root: File, modelId: String): File {
        require(modelId.matches(Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,127}")))
        require(modelId != "." && modelId != "..")
        val base = root.canonicalFile
        val result = File(base, modelId).canonicalFile
        require(result.parentFile == base) { "Invalid model path" }
        return result
    }
}

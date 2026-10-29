package ai.mlc.mlcchat.core.model

data class ModelDescriptor(val id: String, val displayName: String, val format: String, val localPath: String, val estimatedRamBytes: Long? = null) {
    init {
        require(id.isNotBlank() && displayName.isNotBlank())
        require(format.lowercase() in setOf("mlc", "tvm", "gguf"))
        require(estimatedRamBytes == null || estimatedRamBytes >= 0)
    }
}

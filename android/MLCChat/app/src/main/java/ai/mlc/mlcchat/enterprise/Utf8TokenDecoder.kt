package ai.mlc.mlcchat.enterprise

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/** Incremental UTF-8 decoding across arbitrary native token byte boundaries. */
class Utf8TokenDecoder {
    private val decoder = StandardCharsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPLACE)
        .onUnmappableCharacter(CodingErrorAction.REPLACE)
    private var pending = ByteArray(0)

    @Synchronized fun append(bytes: ByteArray): String = decode(bytes, false)
    @Synchronized fun finish(): String = decode(byteArrayOf(), true)

    private fun decode(bytes: ByteArray, end: Boolean): String {
        val input = ByteBuffer.wrap(pending + bytes)
        val output = CharBuffer.allocate((input.remaining() + 4) * 2)
        val result = decoder.decode(input, output, end)
        if (result.isError) result.throwException()
        pending = ByteArray(input.remaining()).also { input.get(it) }
        if (end) {
            decoder.flush(output)
            pending = byteArrayOf()
            decoder.reset()
        }
        output.flip()
        return output.toString()
    }
}

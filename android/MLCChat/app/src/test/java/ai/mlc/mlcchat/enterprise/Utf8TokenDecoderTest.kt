package ai.mlc.mlcchat.enterprise

import org.junit.Assert.assertEquals
import org.junit.Test

class Utf8TokenDecoderTest {
    @Test fun splitPunjabiUnicode() {
        val bytes = "ਸਤਿ ਸ੍ਰੀ ਅਕਾਲ 🌟".toByteArray(Charsets.UTF_8)
        val decoder = Utf8TokenDecoder()
        val result = StringBuilder()
        for (byte in bytes) result.append(decoder.append(byteArrayOf(byte)))
        result.append(decoder.finish())
        assertEquals("ਸਤਿ ਸ੍ਰੀ ਅਕਾਲ 🌟", result.toString())
    }

    @Test fun asciiStreaming() {
        val decoder = Utf8TokenDecoder()
        assertEquals("Hello", decoder.append("Hello".toByteArray()))
        assertEquals("", decoder.finish())
    }
}

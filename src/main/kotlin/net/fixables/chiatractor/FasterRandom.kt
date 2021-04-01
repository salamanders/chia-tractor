package net.fixables.chiatractor

/**
 * A random number generator based on the simple and fast xor-shift pseudo
 * random number generator (RNG) specified in:
 * Marsaglia, George. (2003). Xorshift RNGs.
 * http://www.jstatsoft.org/v08/i14/xorshift.pdf
 * Translated from:
 * http://www.codeproject.com/Articles/9187/A-fast-equivalent-for-System-Random.
 */
object FasterRandom  {
    var x: Long = 0
    var y: Long = 0
    var z: Long = 0
    var w: Long = 0
     fun nextBytes(buffer: ByteArray) {
        // Fill up the bulk of the buffer in chunks of 4 bytes at a time.
        var x = x
        var y = y
        var z = z
        var w = w
        var i = 0
        var t: Long
        val bound = buffer.size - 3
        while (i < bound) {
            // Generate 4 bytes.
            // Increased performance is achieved by generating 4 random bytes per loop.
            // Also note that no mask needs to be applied to zero out the higher order bytes before
            // casting because the cast ignores those bytes. Thanks to Stefan Trosch for pointing this out.
            t = x xor (x shl 11)
            x = y
            y = z
            z = w
            w = w xor (w shr 19) xor (t xor (t shr 8))
            buffer[i++] = w.toByte()
            buffer[i++] = (w shr 8).toByte()
            buffer[i++] = (w shr 16).toByte()
            buffer[i++] = (w shr 24).toByte()
        }

        // Fill up any remaining bytes in the buffer.
        if (i < buffer.size) {
            // Generate 4 bytes.
            t = x xor (x shl 11)
            x = y
            y = z
            z = w
            w = w xor (w shr 19) xor (t xor (t shr 8))
            buffer[i++] = w.toByte()
            if (i < buffer.size) {
                buffer[i++] = (w shr 8).toByte()
                if (i < buffer.size) {
                    buffer[i++] = (w shr 16).toByte()
                    if (i < buffer.size) {
                        buffer[i] = (w shr 24).toByte()
                    }
                }
            }
        }
        this.x = x
        this.y = y
        this.z = z
        this.w = w
    }
}
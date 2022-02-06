import kotlin.random.Random

expect fun currentTimeMillis(): Long

fun generateRandomClientId(): String {
    val length = 30
    val buffer = StringBuilder(length)
    for (i in 0 until length) {
        buffer.append(Random.Default.nextInt(97, 122).toChar())
    }
    return buffer.toString()
}

fun UByteArray.validatePayloadFormat(indicator: UInt): Boolean {
    if (indicator == 1u) {
        return this.toByteArray().decodeToString().validateUTF8String()
    }
    return true
}

fun String.validateUTF8String(): Boolean {
    this.forEachIndexed { index, character ->
        if (character == '\u0000')
            return false
        if (character == '\uFFFD')
            return false
        if (character in '\uD800'..'\uDFFF') {
            this.getOrNull(index + 1)?.let {
                if (it !in '\uDC00'..'\uDFFF')
                    return false
            } ?: return false
        }
    }
    return true
}

fun UByteArray.toHexString() = joinToString("") { it.toString(16).padStart(2, '0') }

fun <K, V> MutableMap<K, V>.removeIf(predicate: (MutableMap.MutableEntry<K, V>) -> Boolean): Boolean {
    var removed = false
    val iterator = iterator()
    while (iterator.hasNext()) {
        val next = iterator.next()
        if (predicate(next)) {
            iterator.remove()
            removed = true
        }
    }
    return removed
}

private infix fun UInt.leftRotate(bits: Int): UInt = ((this shl bits) or (this shr (32 - bits)))

// TODO implement base64
private fun ByteArray.sha1(): ByteArray { // TODO fix
    val hash = UIntArray(5)
    hash[0] = 0x67452301u
    hash[1] = 0xEFCDAB89u
    hash[2] = 0x98BADCFEu
    hash[3] = 0x10325476u
    hash[4] = 0xC3D2E1F0u

    val ml = this.size * 8

    val chunks = UIntArray((((this.size + 8) shr 6) + 1) * 16)

    for (i in 0 until this.size) {
        chunks[i shr 2] = chunks[i shr 2] or (this[i].toUInt() shl (24 - (i % 4) * 8))
    }

    chunks[this.size shr 2] = chunks[this.size shr 2] or (0x80u shl (24 - (this.size % 4) * 8))
    chunks[chunks.size - 1] = ml.toUInt()

    for (j in chunks.indices step 16) {
        val w = UIntArray(80)
        for (i in 0 until 16) {
            w[i] = chunks[j + i]
        }
        for (i in 16 until 80) {
            w[i] = (w[i - 3] xor w[i - 8] xor w[i - 14] xor w[i - 16]) leftRotate 1
        }

        var a = hash[0]
        var b = hash[1]
        var c = hash[2]
        var d = hash[3]
        var e = hash[4]
        var f = 0u
        var k = 0u
        for (i in 0 until 80) {
            when (i) {
                in 0..19 -> {
                    f = (b and c) or (b.inv() and d)
                    k = 0x5A827999u
                }
                in 20..39 -> {
                    f = b xor c xor d
                    k = 0x6ED9EBA1u
                }
                in 40..59 -> {
                    f = (b and c) or (b and d) or (c and d)
                    k = 0x8F1BBCDCu
                }
                in 60..79 -> {
                    f = b xor c xor d
                    k = 0xCA62C1D6u
                }
            }

            val temp = (a leftRotate 5) + f + e + k + w[i]
            e = d
            d = c
            c = b leftRotate 30
            b = a
            a = temp
        }

        hash[0] += a
        hash[1] += b
        hash[2] += c
        hash[3] += d
        hash[4] += e
    }

    return hash.foldIndexed(ByteArray(hash.size)) { i, a, v -> a.apply { set(i, v.toByte()) } }
}

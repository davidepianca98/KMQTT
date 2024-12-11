package io.github.davidepianca98

import io.github.davidepianca98.socket.streams.ByteArrayOutputStream
import kotlin.random.Random

public expect fun currentTimeMillis(): Long

public fun generateRandomClientId(): String {
    val length = 30
    val buffer = StringBuilder(length)
    for (i in 0 until length) {
        buffer.append(Random.Default.nextInt(97, 122).toChar())
    }
    return buffer.toString()
}

public fun UByteArray.validatePayloadFormat(indicator: UInt): Boolean {
    if (indicator == 1u) {
        return this.toByteArray().decodeToString().validateUTF8String()
    }
    return true
}

public fun String.validateUTF8String(): Boolean {
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

public fun UByteArray.toHexString(): String = joinToString("") { it.toString(16).padStart(2, '0') }

public fun UIntArray.toHexString(): String = joinToString("") { it.toString(16).padStart(8, '0') }

public fun String.fromHexString(): ByteArray = chunked(2).map { it.toInt(16).toByte() }.toByteArray()

public fun <K, V> MutableMap<K, V>.removeIf(predicate: (MutableMap.MutableEntry<K, V>) -> Boolean): Boolean {
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

public fun ByteArray.sha1(): ByteArray {
    val hash = UIntArray(5)
    hash[0] = 0x67452301u
    hash[1] = 0xEFCDAB89u
    hash[2] = 0x98BADCFEu
    hash[3] = 0x10325476u
    hash[4] = 0xC3D2E1F0u

    val ml = (this.size * 8).toULong()

    // Prepare the data
    val outStream = ByteArrayOutputStream()

    outStream.write(this.toUByteArray())
    outStream.write(0x80u)

    while ((outStream.size() + 8) % 64 != 0) {
        outStream.write(0u)
    }
    outStream.writeULong(ml)

    val data = outStream.toByteArray()

    for (j in data.indices step 64) {
        val w = UIntArray(80)
        for (i in 0 until 16) {
            w[i] = (data[j + i * 4].toUInt() shl 24) or
                    (data[j + i * 4 + 1].toUInt() shl 16) or
                    (data[j + i * 4 + 2].toUInt() shl 8) or
                    data[j + i * 4 + 3].toUInt()
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

    val hexString = hash.toHexString()

    return hexString.fromHexString()
}

public fun ByteArray.toBase64(): String {
    val base64chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    var r = ""
    var p = ""
    var c = this.size % 3

    val outStream = ByteArrayOutputStream()
    outStream.write(this.toUByteArray())

    if (c > 0) {
        while (c < 3) {
            p += "="
            outStream.write(0u)
            c++
        }
    }

    val s = outStream.toByteArray()

    c = 0
    while (c < this.size) {
        if (c > 0 && (c / 3 * 4) % 76 == 0) {
            r += "\r\n"
        }

        val n = (s[c].toInt() shl 16) + (s[c + 1].toInt() shl 8) + s[c + 2].toInt()

        val n1 = n shr 18 and 63
        val n2 = n shr 12 and 63
        val n3 = n shr 6 and 63
        val n4 = n and 63

        r += ("" + base64chars[n1] + base64chars[n2] + base64chars[n3] + base64chars[n4])
        c += 3
    }

    return r.substring(0, r.length - p.length) + p
}

public fun String.isValidPem(): Boolean {
    return matches("(-----BEGIN PUBLIC KEY-----(\\n|\\r|\\r\\n)([0-9a-zA-Z+/=]{64}(\\n|\\r|\\r\\n))*([0-9a-zA-Z+/=]{1,63}(\\n|\\r|\\r\\n))?-----END PUBLIC KEY-----(\\n|\\r|\\r\\n)?)|(-----BEGIN PRIVATE KEY-----(\\n|\\r|\\r\\n)([0-9a-zA-Z+/=]{64}(\\n|\\r|\\r\\n))*([0-9a-zA-Z+/=]{1,63}(\\n|\\r|\\r\\n))?-----END PRIVATE KEY-----(\\n|\\r|\\r\\n)?)|(-----BEGIN CERTIFICATE-----(\\n|\\r|\\r\\n)([0-9a-zA-Z+/=]{64}(\\n|\\r|\\r\\n))*([0-9a-zA-Z+/=]{1,63}(\\n|\\r|\\r\\n))?-----END CERTIFICATE-----(\\n|\\r|\\r\\n)?)".toRegex())
}

package mqtt

import java.io.InputStream

fun Int.encodeVariableByteInteger(): ByteArray {
    val encoded = mutableListOf<Int>()
    var x = this
    do {
        var encodedByte = x.rem(128)
        x /= 128
        if (x > 0) {
            encodedByte = encodedByte or 128
        }
        encoded.add(encodedByte)
    } while (x > 0)
    return encoded.map { it.toByte() }.toByteArray()
}

fun InputStream.decodeVariableByteInteger(): Int {
    var multiplier = 1
    var value = 0
    do {
        val encodedByte = read()
        value += (encodedByte and 127) * multiplier
        if (multiplier > 128 * 128 * 128) {
            throw Exception("Malformed Variable Byte Integer")
        }
        multiplier *= 128
    } while ((encodedByte and 128) != 0)
    return value
}

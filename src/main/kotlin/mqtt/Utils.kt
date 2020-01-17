package mqtt

import java.io.InputStream
import java.io.OutputStream

fun OutputStream.encodeVariableByteInteger(value: Int) {
    var x = value
    do {
        var encodedByte = x.rem(128)
        x /= 128
        if (x > 0) {
            encodedByte = encodedByte or 128
        }
        write(encodedByte)
    } while (x > 0)
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

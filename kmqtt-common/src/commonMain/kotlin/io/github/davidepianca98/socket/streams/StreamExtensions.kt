package io.github.davidepianca98.socket.streams

public fun OutputStream.encodeVariableByteInteger(value: UInt): Int {
    var length = 0
    var x = value
    do {
        var encodedByte = x.rem(128u)
        x /= 128u
        if (x > 0u) {
            encodedByte = encodedByte or 128u
        }
        write(encodedByte.toUByte())
        length++
    } while (x > 0u)
    return length
}

public fun InputStream.decodeVariableByteInteger(): UInt {
    var multiplier = 1u
    var value = 0u
    do {
        val encodedByte = read().toUInt()
        value += (encodedByte and 127u) * multiplier
        if (multiplier > 128u * 128u * 128u) {
            throw Exception("Malformed Variable Byte Integer")
        }
        multiplier *= 128u
    } while ((encodedByte and 128u) != 0u)
    return value
}

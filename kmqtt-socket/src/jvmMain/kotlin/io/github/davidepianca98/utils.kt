package io.github.davidepianca98

import java.nio.ByteBuffer

public fun ByteBuffer.toUByteArray(): UByteArray {
    val length = remaining()
    val array = ByteArray(length)
    get(array, 0, length)
    clear()
    return array.toUByteArray()
}

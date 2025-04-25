package io.github.davidepianca98
import node.buffer.Buffer

public fun UByteArray.toBuffer(): Buffer {
    val result = Buffer.allocUnsafe(this.size)
    for (i in indices) {
        result.writeUint8(this[i].toInt(), i)
    }
    return result
}

public fun Buffer.toUByteArray(): UByteArray {
    val result = UByteArray(this.byteLength)
    for (i in 0 until this.byteLength) {
        result[i] = this[i].toUByte()
    }
    return result
}
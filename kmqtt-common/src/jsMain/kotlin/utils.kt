
import node.buffer.Buffer

public fun UByteArray.toBuffer(): Buffer {
    return Buffer(this)
}

public fun Buffer.toUByteArray(): UByteArray {
    val result = UByteArray(this.byteLength)
    for (i in 0 until this.byteLength) {
        result[i] = this[i].toUByte()
    }
    return result
}
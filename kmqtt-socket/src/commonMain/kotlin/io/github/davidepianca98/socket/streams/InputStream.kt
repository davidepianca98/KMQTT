package io.github.davidepianca98.socket.streams

public interface InputStream {

    public fun read(): UByte

    public fun readBytes(length: Int): UByteArray

    public fun readUShort(): UShort {
        return ((read().toUInt() shl 8) or read().toUInt()).toUShort()
    }

    public fun readUInt(): UInt {
        var number = 0u
        for (i in 0..3) {
            number = number or (read().toUInt() shl ((3 - i) * 8))
        }
        return number
    }

    public fun readULong(): ULong {
        var number: ULong = 0u
        for (i in 0..7) {
            number = number or (read().toULong() shl ((7 - i) * 8))
        }
        return number
    }
}

package io.github.davidepianca98.socket.streams

public interface OutputStream {

    public fun write(b: UByte)

    public fun write(b: UByteArray)

    public fun writeUShort(s: UShort) {
        write((s.toUInt() shr 8).toUByte())
        write((s and 0xFFu).toUByte())
    }

    public fun writeUInt(v: UInt) {
        for (i in 0..3) {
            write(((v shr ((3 - i) * 8)) and 0xFFu).toUByte())
        }
    }

    public fun writeULong(l: ULong) {
        for (i in 0..7) {
            write(((l shr ((7 - i) * 8)) and 0xFFu).toUByte())
        }
    }
}

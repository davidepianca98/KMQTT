package socket.streams

interface OutputStream {

    fun write(b: UByte)

    fun write(b: UByteArray)

    fun writeUShort(s: UShort) {
        write((s.toUInt() shr 8).toUByte())
        write((s and 0xFFu).toUByte())
    }

    fun writeUInt(v: UInt) {
        for (i in 0..3) {
            write(((v shr ((3 - i) * 8)) and 0xFFu).toUByte())
        }
    }

    fun writeULong(l: ULong) {
        for (i in 0..7) {
            write(((l shr ((7 - i) * 8)) and 0xFFu).toUByte())
        }
    }
}

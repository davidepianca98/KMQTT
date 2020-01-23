package socket.streams

interface OutputStream {

    fun write(b: UByte)

    fun write(b: UByteArray)
}

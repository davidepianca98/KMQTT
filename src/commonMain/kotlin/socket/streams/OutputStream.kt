package socket.streams

interface OutputStream {

    suspend fun write(b: UByte)

    suspend fun write(b: UByteArray)
}

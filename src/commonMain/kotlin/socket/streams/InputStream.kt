package socket.streams

interface InputStream {

    suspend fun read(): UByte

    suspend fun readBytes(length: Int): UByteArray
}

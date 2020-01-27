package socket.streams

interface InputStream {

    fun read(): UByte

    fun readBytes(length: Int): UByteArray
}

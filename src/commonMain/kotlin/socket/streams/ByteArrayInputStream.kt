package socket.streams

class ByteArrayInputStream(private val array: UByteArray) : InputStream {

    private var position = 0

    override suspend fun read(): UByte {
        return if (position < array.size)
            array[position++].toUByte()
        else
            throw EOFException()
    }

    override suspend fun readBytes(length: Int): UByteArray {
        val result = UByteArray(length)
        for (i in 0 until length)
            result[i] = read()
        return result
    }

    suspend fun readRemaining(): UByteArray {
        return readBytes(available())
    }

    fun available(): Int {
        return array.size - position
    }
}

package socket.streams

class ByteArrayInputStream(private val array: UByteArray) : InputStream {

    private var position = 0

    override fun read(): UByte {
        return if (position < array.size)
            array[position++]
        else
            throw EOFException()
    }

    override fun readBytes(length: Int): UByteArray {
        val result = UByteArray(length)
        for (i in 0 until length)
            result[i] = read()
        return result
    }

    fun readRemaining(): UByteArray {
        return readBytes(available())
    }

    fun available(): Int {
        return array.size - position
    }
}

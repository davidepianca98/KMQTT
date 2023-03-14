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
        try {
            val result = array.copyOfRange(position, position + length)
            position += length
            return result
        } catch (e: IndexOutOfBoundsException) {
            throw EOFException()
        }
    }

    fun readRemaining(): UByteArray {
        return readBytes(available())
    }

    fun available(): Int {
        return array.size - position
    }
}

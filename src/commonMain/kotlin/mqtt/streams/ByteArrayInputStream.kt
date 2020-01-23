package mqtt.streams

class ByteArrayInputStream(private val array: ByteArray) {

    private var position = 0

    fun read(): UByte {
        return if (position < array.size)
            array[position++].toUByte()
        else
            throw EOFException()
    }

    fun readBytes(length: Int): UByteArray {
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

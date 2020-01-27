package socket.streams

import leftShift

class DynamicByteBuffer : InputStream, OutputStream {

    private var writePosition = 0
    private var readCounter = 0
    private var array: UByteArray = UByteArray(32)

    private fun ensureCapacity(length: Int) {
        if (writePosition + length > array.size)
            array = array.copyOf(array.size + length * 2)
    }

    override fun read(): UByte {
        if (writePosition == 0)
            throw EOFException()
        val byte = array[readCounter]
        readCounter++
        return byte
    }

    override fun readBytes(length: Int): UByteArray {
        val result = UByteArray(length)
        for (i in 0 until length)
            result[i] = read()
        return result
    }

    fun clearReadCounter() {
        readCounter = 0
    }

    fun shift() {
        if (readCounter == writePosition) {
            writePosition = 0
        } else {
            array.leftShift(readCounter)
            writePosition -= readCounter
        }
        clearReadCounter()
    }

    override fun write(b: UByte) {
        ensureCapacity(1)
        array[writePosition] = b
        writePosition += 1
    }

    private fun write(b: UByteArray, off: Int, len: Int) {
        ensureCapacity(len)
        array = b.copyInto(array, writePosition, off, len)
        writePosition += len
    }

    override fun write(b: UByteArray) {
        write(b, 0, b.size)
    }

}

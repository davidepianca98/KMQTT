package io.github.davidepianca98.socket.streams

public class DynamicByteBuffer : InputStream, OutputStream {

    private var writePosition = 0
    private var readCounter = 0
    private var lastReadComplete = 0
    private var array: UByteArray = UByteArray(32)

    private fun ensureCapacity(length: Int) {
        if (writePosition + length > array.size)
            array = array.copyOf(array.size + (length * 2))
    }

    override fun read(): UByte {
        if (readCounter >= writePosition)
            throw EOFException()
        return array[readCounter++]
    }

    override fun readBytes(length: Int): UByteArray {
        val endIndex = readCounter + length
        if (endIndex > writePosition)
            throw EOFException()
        val data = array.copyOfRange(readCounter, endIndex)
        readCounter = endIndex
        return data
    }

    public fun clearReadCounter() {
        readCounter = lastReadComplete
    }

    public fun shift() {
        if (readCounter == writePosition) {
            writePosition = 0
            readCounter = 0
        }
        lastReadComplete = readCounter
    }

    override fun write(b: UByte) {
        ensureCapacity(1)
        array[writePosition++] = b
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

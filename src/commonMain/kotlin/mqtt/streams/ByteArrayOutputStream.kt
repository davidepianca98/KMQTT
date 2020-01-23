package mqtt.streams

class ByteArrayOutputStream {

    private var count = 0
    private var array: UByteArray = UByteArray(32)

    private fun ensureCapacity(length: Int) {
        if (count + length > array.size)
            array = array.copyOf(array.size + length * 2)
    }

    fun write(b: UInt) {
        ensureCapacity(1)
        array[count] = b.toUByte()
        count += 1
    }

    fun write(b: UByteArray, off: Int, len: Int) {
        ensureCapacity(len)
        array = b.copyInto(array, count, off, len)
        count += len
    }

    fun write(b: UByteArray) {
        ensureCapacity(b.size)
        write(b, 0, b.size)
    }

    fun size(): Int {
        return count
    }

    fun toByteArray(): UByteArray {
        return array.copyOfRange(0, count)
    }

    override fun toString(): String {
        return array.toByteArray().decodeToString() // UTF-8
    }
}

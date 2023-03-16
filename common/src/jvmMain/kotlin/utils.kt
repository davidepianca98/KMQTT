import java.nio.ByteBuffer

fun ByteBuffer.toUByteArray(): UByteArray {
    val length = remaining()
    val array = ByteArray(length)
    get(array, 0, length)
    clear()
    return array.toUByteArray()
}

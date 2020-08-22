import java.nio.ByteBuffer

actual fun currentTimeMillis(): Long {
    return System.currentTimeMillis()
}

fun ByteBuffer.toUByteArray(): UByteArray {
    val length = remaining()
    val array = ByteArray(length)
    get(array, 0, length)
    return array.toUByteArray()
}

import org.khronos.webgl.get
import org.khronos.webgl.set
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.js.Date
import kotlin.js.Promise


fun UByteArray.toBuffer(): Buffer {
    val result = Buffer(this.size)
    for (i in 0 until this.size) {
        result[i] = this[i].toByte()
    }
    return result
}

fun Buffer.toUByteArray(): UByteArray {
    val result = UByteArray(this.byteLength)
    for (i in 0 until this.byteLength) {
        result[i] = this[i].toUByte()
    }
    return result
}

suspend fun <T> Promise<T>.await(): T = suspendCoroutine { cont ->
    then({ cont.resume(it) }, { cont.resumeWithException(it) })
}

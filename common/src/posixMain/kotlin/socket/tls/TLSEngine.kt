package socket.tls

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer

interface TLSEngine {

    val isInitFinished: Boolean
    val bioShouldRetry: Boolean

    fun accept(): Int

    fun write(buffer: CPointer<ByteVar>, length: Int): Int

    fun read(buffer: CPointer<ByteVar>, length: Int): Int

    fun bioRead(buffer: CPointer<ByteVar>, length: Int): Int

    fun getError(result: Int): TLSError

    fun close()
}
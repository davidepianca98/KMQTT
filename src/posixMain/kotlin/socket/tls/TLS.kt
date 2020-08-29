package socket.tls

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import mqtt.broker.Broker

enum class TLSError {
    OK,
    WANT_READ,
    ERROR
}

expect class TLSEngine(serverContext: TLSServerContext) {
    val isInitFinished: Boolean
    val bioShouldRetry: Boolean

    fun accept(): Int

    fun write(buffer: CPointer<ByteVar>, length: Int): Int

    fun read(buffer: CPointer<ByteVar>, length: Int): Int

    fun bioRead(buffer: CPointer<ByteVar>, length: Int): Int

    fun getError(result: Int): TLSError

    fun close()
}

expect class TLSServerContext(broker: Broker) {
    fun close()
}

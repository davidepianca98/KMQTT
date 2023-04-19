import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import socket.tls.TLSEngine

internal actual class TLSClientEngine actual constructor(tlsSettings: TLSClientSettings) : TLSEngine {
    override val isInitFinished: Boolean
        get() = TODO("Not yet implemented")
    override val bioShouldRetry: Boolean
        get() = TODO("Not yet implemented")

    override fun write(buffer: CPointer<ByteVar>, length: Int): Int {
        TODO("Not yet implemented")
    }

    override fun read(buffer: CPointer<ByteVar>, length: Int): Int {
        TODO("Not yet implemented")
    }

    override fun bioRead(buffer: CPointer<ByteVar>, length: Int): Int {
        TODO("Not yet implemented")
    }

    override fun bioWrite(buffer: CPointer<ByteVar>, length: Int): Int {
        TODO("Not yet implemented")
    }

    override fun getError(result: Int): Int {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }
}

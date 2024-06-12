import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import socket.tls.TLSClientSettings
import socket.tls.TLSEngine

internal expect class TLSClientEngine(tlsSettings: TLSClientSettings) : TLSEngine {

    override val isInitFinished: Boolean
    override val bioShouldRetry: Boolean

    override fun write(buffer: CPointer<ByteVar>, length: Int): Int

    override fun read(buffer: CPointer<ByteVar>, length: Int): Int

    override fun bioRead(buffer: CPointer<ByteVar>, length: Int): Int

    override fun bioWrite(buffer: CPointer<ByteVar>, length: Int): Int

    override fun getError(result: Int): Int

    override fun close()
}
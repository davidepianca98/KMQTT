package io.github.davidepianca98.socket.tls

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer

internal expect class TLSServerEngine(serverContext: TLSServerContext) : TLSEngine {

    override val isInitFinished: Boolean
    override val bioShouldRetry: Boolean

    override fun write(buffer: CPointer<ByteVar>, length: Int): Int

    override fun read(buffer: CPointer<ByteVar>, length: Int): Int

    override fun bioRead(buffer: CPointer<ByteVar>, length: Int): Int

    override fun bioWrite(buffer: CPointer<ByteVar>, length: Int): Int

    override fun getError(result: Int): Int

    override fun close()
}

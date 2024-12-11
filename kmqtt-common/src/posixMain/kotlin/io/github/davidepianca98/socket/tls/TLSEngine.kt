package io.github.davidepianca98.socket.tls

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer

public interface TLSEngine {

    public val isInitFinished: Boolean
    public val bioShouldRetry: Boolean

    public fun write(buffer: CPointer<ByteVar>, length: Int): Int

    public fun read(buffer: CPointer<ByteVar>, length: Int): Int

    public fun bioRead(buffer: CPointer<ByteVar>, length: Int): Int

    public fun bioWrite(buffer: CPointer<ByteVar>, length: Int): Int

    public fun getError(result: Int): Int

    public fun close()
}
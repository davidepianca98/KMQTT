package socket

import socket.streams.EOFException
import socket.streams.InputStream
import socket.streams.OutputStream
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.nio.channels.InterruptedByTimeoutException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@ExperimentalUnsignedTypes
actual class Socket(private val socket: AsynchronousSocketChannel) {

    actual var soTimeout: Int = 30000

    private val inputStream = object : InputStream {

        private val buf = ByteBuffer.allocate(8192)
        private var bufSize = 0
        private var position = 0

        override suspend fun read(): UByte {
            return readBytes(1)[0]
        }

        override suspend fun readBytes(length: Int): UByteArray {
            if (position == bufSize)
                buf.clear()
            if (position + length > bufSize) {
                tryJavaSocket {
                    bufSize = socket.readSuspend(buf)
                    when {
                        bufSize < 0 -> throw EOFException()
                        bufSize >= 0 -> position = 0
                    }
                }
            }

            val array = buf.array().toUByteArray().copyOfRange(position, position + length)
            position += length
            return array
        }
    }

    private suspend fun AsynchronousSocketChannel.readSuspend(data: ByteBuffer) = suspendCoroutine<Int> { c ->
        this.read(data, soTimeout.toLong(), TimeUnit.MILLISECONDS, Unit, object : CompletionHandler<Int, Unit> {
            override fun completed(result: Int, attachment: Unit) = Unit.apply { c.resume(result) }
            override fun failed(exc: Throwable, attachment: Unit) = Unit.apply { c.resumeWithException(exc) }
        })
    }

    private val outputStream = object : OutputStream {
        override suspend fun write(b: UByte) {
            val array = UByteArray(1)
            array[0] = b
            write(array)
        }

        override suspend fun write(b: UByteArray) {
            tryJavaSocket {
                socket.writeSuspend(ByteBuffer.wrap(b.toByteArray()))
            }
        }
    }

    private suspend fun AsynchronousSocketChannel.writeSuspend(data: ByteBuffer) = suspendCoroutine<Int> { c ->
        this.write(data, soTimeout.toLong(), TimeUnit.MILLISECONDS, Unit, object : CompletionHandler<Int, Unit> {
            override fun completed(result: Int, attachment: Unit) = Unit.apply { c.resume(result) }
            override fun failed(exc: Throwable, attachment: Unit) = Unit.apply { c.resumeWithException(exc) }
        })
    }

    suspend fun tryJavaSocket(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: InterruptedByTimeoutException) {
            throw SocketTimeoutException()
        } catch (e: java.io.IOException) {
            throw IOException(e.message)
        } catch (e: Exception) {
            throw e
        }
    }

    actual fun close() {
        socket.close()
    }

    actual fun getInputStream(): InputStream {
        return inputStream
    }

    actual fun getOutputStream(): OutputStream {
        return outputStream
    }

}

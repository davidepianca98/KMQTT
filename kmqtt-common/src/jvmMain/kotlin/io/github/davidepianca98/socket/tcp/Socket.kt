package io.github.davidepianca98.socket.tcp

import io.github.davidepianca98.socket.IOException
import io.github.davidepianca98.socket.SocketClosedException
import io.github.davidepianca98.socket.SocketInterface
import io.github.davidepianca98.toUByteArray
import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

public actual open class Socket(
    protected val channel: SocketChannel,
    private val key: SelectionKey?,
    private var sendBuffer: ByteBuffer,
    private val receiveBuffer: ByteBuffer
) : SocketInterface {

    private val lock = ReentrantLock()

    actual override fun send(data: UByteArray) {
        lock.withLock {
            val byteArray = data.toByteArray()
            try {
                sendBuffer.put(byteArray)
            } catch (e: BufferOverflowException) {
                sendBuffer = ByteBuffer.allocate(sendBuffer.capacity() + data.size)
                sendBuffer.put(byteArray)
            }
            sendFromBuffer()
        }
    }

    protected fun sendFromBuffer() {
        lock.withLock {
            sendBuffer.flip()
            val size = sendBuffer.remaining()
            try {
                val count = channel.write(sendBuffer)
                if (count < size) {
                    key?.interestOps(SelectionKey.OP_WRITE)
                } else {
                    key?.interestOps(SelectionKey.OP_READ)
                }
                sendBuffer.compact()
            } catch (e: java.io.IOException) {
                close()
                throw IOException(e.message)
            }/* catch (e: IllegalArgumentException) {
                close()
                throw IOException(e.message)
            }*/
        }
    }

    protected fun readToBuffer(): Int {
        try {
            val length = channel.read(receiveBuffer)
            when {
                length >= 0 -> return length
                else -> {
                    close()
                    throw SocketClosedException("Read to buffer error End Of Stream ($length)")
                }
            }
        } catch (e: java.io.IOException) {
            close()
            throw IOException(e.message)
        }
    }

    actual override fun read(): UByteArray? {
        return if (readToBuffer() > 0) {
            receiveBuffer.flip()
            receiveBuffer.toUByteArray()
        }
        else null
    }

    actual override fun close() {
        key?.cancel()
        if (channel.isOpen) {
            channel.close()
        }
    }

    actual override fun sendRemaining() {
        sendFromBuffer()
    }

}
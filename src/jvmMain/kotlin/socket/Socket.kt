package socket

import toUByteArray
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel

@ExperimentalUnsignedTypes
actual open class Socket(private val sendBuffer: ByteBuffer, private val receiveBuffer: ByteBuffer) : SocketImpl {

    var key: SelectionKey? = null

    private val pendingSendData = mutableListOf<ByteArray>()

    actual override fun send(data: UByteArray) {
        sendBuffer.clear()
        sendBuffer.put(data.toByteArray())
        sendFromBuffer()
    }

    private fun send(data: ByteArray) {
        sendBuffer.clear()
        sendBuffer.put(data)
        sendFromBuffer()
    }

    protected fun sendFromBuffer() {
        sendBuffer.flip()
        val selectionKey = key!!
        val channel = selectionKey.channel() as SocketChannel
        val size = sendBuffer.remaining()
        try {
            val count = channel.write(sendBuffer)
            if (count < size) {
                val array = ByteArray(sendBuffer.remaining())
                sendBuffer.get(array)
                pendingSendData.add(array)
                selectionKey.interestOps(SelectionKey.OP_READ or SelectionKey.OP_WRITE)
            } else {
                selectionKey.interestOps(SelectionKey.OP_READ)
            }
        } catch (e: java.io.IOException) {
            close()
            throw IOException()
        }
    }

    protected fun readToBuffer() {
        val channel = key?.channel() as SocketChannel
        receiveBuffer.clear()
        try {
            val length = channel.read(receiveBuffer)
            if (length >= 0) {
                receiveBuffer.flip()
            } else {
                close()
                throw SocketClosedException()
            }
        } catch (e: java.io.IOException) {
            close()
            throw IOException()
        }
    }

    actual override fun read(): UByteArray? {
        readToBuffer()
        return receiveBuffer.toUByteArray()
    }

    fun close() {
        val channel = key?.channel() as SocketChannel
        key?.cancel()
        channel.close()
    }

    actual override fun sendRemaining() {
        pendingSendData.forEach {
            send(it)
        }
    }

}


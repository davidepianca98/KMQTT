package socket

import toUByteArray
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel

@ExperimentalUnsignedTypes
actual open class Socket(private val sendBuffer: ByteBuffer, private val receiveBuffer: ByteBuffer) : SocketInterface {

    var key: SelectionKey? = null

    private var pendingSendData = mutableListOf<ByteArray>()

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
            throw IOException(e.message)
        }
    }

    protected fun readToBuffer(): Int {
        val channel = key?.channel() as SocketChannel
        receiveBuffer.clear()
        try {
            val length = channel.read(receiveBuffer)
            when {
                length > 0 -> receiveBuffer.flip()
                length == 0 -> return length
                else -> {
                    close()
                    throw SocketClosedException()
                }
            }
            return length
        } catch (e: java.io.IOException) {
            close()
            throw IOException(e.message)
        }
    }

    actual override fun read(): UByteArray? {
        return if (readToBuffer() > 0)
            receiveBuffer.toUByteArray()
        else null
    }

    fun close() {
        val channel = key?.channel() as SocketChannel
        key?.cancel()
        channel.close()
    }

    actual override fun sendRemaining() {
        val sendData = pendingSendData
        pendingSendData = mutableListOf()
        sendData.forEach {
            send(it)
        }
    }

}


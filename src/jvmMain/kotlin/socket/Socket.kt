package socket

import toUByteArray
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel

@ExperimentalUnsignedTypes
actual open class Socket(
    private val key: SelectionKey,
    private val sendBuffer: ByteBuffer,
    private val receiveBuffer: ByteBuffer
) : SocketInterface {

    private var pendingSendData = mutableListOf<ByteArray>()
    private var canWrite = true

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

    private fun addToPending() {
        val array = ByteArray(sendBuffer.remaining())
        sendBuffer.get(array)
        pendingSendData.add(array)
    }

    protected fun sendFromBuffer() {
        sendBuffer.flip()

        if (!canWrite) {
            addToPending()
            return
        }

        val channel = key.channel() as SocketChannel
        val size = sendBuffer.remaining()
        try {
            val count = channel.write(sendBuffer)
            if (count < size) {
                canWrite = false
                addToPending()
                key.interestOps(SelectionKey.OP_WRITE)
            } else {
                key.interestOps(SelectionKey.OP_READ)
            }
        } catch (e: java.io.IOException) {
            close()
            throw IOException(e.message)
        }
    }

    protected fun readToBuffer(): Int {
        val channel = key.channel() as SocketChannel
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

    actual override fun close() {
        val channel = key.channel() as SocketChannel
        key.cancel()
        channel.close()
    }

    actual override fun sendRemaining() {
        canWrite = true
        val sendData = mutableListOf<ByteArray>()
        sendData.addAll(pendingSendData)
        pendingSendData.clear()
        sendData.forEach {
            send(it)
        }
    }

}


package socket

import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel

@ExperimentalUnsignedTypes
actual class Socket(private val buf: ByteBuffer) {

    var key: SelectionKey? = null

    private val pendingSendData = mutableListOf<UByteArray>()

    actual fun send(data: UByteArray) {
        val selectionKey = key!!
        val channel = selectionKey.channel() as SocketChannel
        try {
            val count = channel.write(ByteBuffer.wrap(data.toByteArray()))
            if (count < data.size) {
                pendingSendData.add(data.copyOfRange(count, data.size))
                selectionKey.interestOps(SelectionKey.OP_READ or SelectionKey.OP_WRITE)
            } else {
                selectionKey.interestOps(SelectionKey.OP_READ)
            }
        } catch (e: java.io.IOException) {
            selectionKey.cancel()
            channel.close()
            throw IOException()
        }
    }

    actual fun read(): UByteArray? {
        val channel = key?.channel() as SocketChannel
        buf.clear()
        try {
            val length = channel.read(buf)
            return if (length >= 0) {
                buf.flip()
                val array = ByteArray(length)
                buf.get(array, 0, length)
                array.toUByteArray()
            } else {
                key?.cancel()
                channel.close()
                throw SocketClosedException()
            }
        } catch (e: java.io.IOException) {
            key?.cancel()
            channel.close()
            throw IOException()
        }
    }

    actual fun sendRemaining() {
        pendingSendData.forEach {
            send(it)
        }
    }

}

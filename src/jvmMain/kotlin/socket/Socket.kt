package socket

import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel

@ExperimentalUnsignedTypes
actual class Socket {

    var key: SelectionKey? = null

    private val pendingSendData = mutableListOf<UByteArray>()

    actual fun send(data: UByteArray) {
        val selectionKey = key!!
        val channel = selectionKey.channel() as SocketChannel
        try {
            val count = channel.write(ByteBuffer.wrap(data.toByteArray()))
            if (count == 0) {
                pendingSendData.add(data)
                selectionKey.interestOps(SelectionKey.OP_READ or SelectionKey.OP_WRITE)
            }
        } catch (e: java.io.IOException) {
            selectionKey.cancel()
            channel.close()
            throw IOException()
        }
    }

    fun sendRemaining() {
        pendingSendData.forEach {
            send(it)
        }
    }

}

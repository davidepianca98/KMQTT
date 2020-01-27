package socket

import leftShift
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel

@ExperimentalUnsignedTypes
actual class Socket(maximumPacketSize: UInt) {

    var key: SelectionKey? = null

    private val pendingSendData = UByteArray(maximumPacketSize.toInt())
    private var position = 0

    actual fun send(data: UByteArray) {
        data.copyInto(pendingSendData, position)
        position += data.size
        sendRemaining()
    }

    fun sendRemaining() {
        val selectionKey = key!!
        val channel = selectionKey.channel() as SocketChannel
        try {
            val count = channel.write(ByteBuffer.wrap(pendingSendData.toByteArray(), 0, position))
            if (count < pendingSendData.size) {
                pendingSendData.leftShift(count)
                position -= count
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

}

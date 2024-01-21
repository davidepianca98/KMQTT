package socket.tcp

import node.buffer.Buffer
import node.net.Socket
import node.net.SocketEvent
import socket.SocketInterface
import socket.SocketState
import toBuffer

public actual open class Socket(
    protected val socket: Socket,
    private val selectCallback: (attachment: Any?, state: SocketState) -> Boolean
) : SocketInterface {

    private val queue = ArrayDeque<UByteArray>()
    private var attachment: Any? = null

    init {
        socket.on(SocketEvent.DATA) { data: Buffer ->
            queue.add(data.toUByteArray())
            try {
                selectCallback(attachment, SocketState.READ)
            } catch (e: dynamic) {
                close()
            }
        }
        socket.on(SocketEvent.DRAIN) {
            socket.resume()
        }
    }

    actual override fun send(data: UByteArray) {
        socket.write(data.toBuffer())
        selectCallback(attachment, SocketState.WRITE)
    }

    actual override fun read(): UByteArray? {
        return queue.removeFirstOrNull()
    }

    actual override fun close() {
        socket.end()
    }

    actual override fun sendRemaining() {

    }

    public fun setAttachment(attachment: Any?) {
        this.attachment = attachment
    }

}
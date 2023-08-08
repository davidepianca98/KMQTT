package socket.tcp

import node.buffer.Buffer
import node.events.Event
import node.net.Socket
import socket.SocketInterface
import socket.SocketState
import toBuffer
import toUByteArray

public actual open class Socket(
    protected val socket: Socket,
    private val selectCallback: (attachment: Any?, state: SocketState) -> Boolean
) : SocketInterface {

    private val queue = ArrayDeque<UByteArray>()
    private var attachment: Any? = null

    init {
        socket.on(Event.DATA) { data: Buffer ->
            queue.add(data.toUByteArray())
            selectCallback(attachment, SocketState.READ)
        }
        socket.on(Event.DRAIN) {
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
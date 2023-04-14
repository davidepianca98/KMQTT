package socket.tcp

import Buffer
import socket.SocketInterface
import socket.SocketState
import toBuffer
import toUByteArray

public actual open class Socket(
    protected val socket: net.Socket,
    private val selectCallback: (attachment: Any?, state: SocketState) -> Boolean
) : SocketInterface {

    private val queue = ArrayDeque<UByteArray>()
    private var attachment: Any? = null

    init {
        socket.on("data") { data: Buffer ->
            queue.add(data.toUByteArray())
            selectCallback(attachment, SocketState.READ)
        }
        socket.on("drain", {
            socket.resume()
            Unit
        } as () -> Unit)
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
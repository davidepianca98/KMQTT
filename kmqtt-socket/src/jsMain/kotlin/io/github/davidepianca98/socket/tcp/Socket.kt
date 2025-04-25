package io.github.davidepianca98.socket.tcp

import node.buffer.Buffer
import node.net.Socket
import node.net.SocketEvent
import io.github.davidepianca98.socket.SocketInterface
import io.github.davidepianca98.socket.SocketState
import io.github.davidepianca98.toBuffer

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
        try {
            selectCallback(attachment, SocketState.WRITE)
        } catch (e: dynamic) {
            close()
        }
    }

    actual override fun read(): UByteArray? {
        return queue.removeFirstOrNull()
    }

    actual override fun close() {
        socket.end()
        socket._destroy(null) {
            socket.unref()
        }
    }

    actual override fun sendRemaining() {

    }

    public fun setAttachment(attachment: Any?) {
        this.attachment = attachment
    }

}
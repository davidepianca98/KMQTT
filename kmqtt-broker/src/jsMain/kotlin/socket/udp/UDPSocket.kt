package socket.udp

import socket.ServerSocketLoop
import toBuffer
import toUByteArray

actual class UDPSocket(
    private val socket: dgram.Socket,
    private val selectCallback: (attachment: Any?, state: ServerSocketLoop.SocketState) -> Boolean
) {

    private val queue = ArrayDeque<UDPReadData>()
    private var attachment: Any? = null

    init {
        socket.on("message") { msg, rinfo ->
            queue.add(UDPReadData(msg.toUByteArray(), rinfo.address, rinfo.port.toInt()))
            selectCallback(attachment, ServerSocketLoop.SocketState.READ)
        }
    }

    actual fun send(data: UByteArray, address: String, port: Int) {
        socket.send(data.toBuffer(), port, address)
        selectCallback(attachment, ServerSocketLoop.SocketState.WRITE)
    }

    actual fun read(): UDPReadData? {
        return queue.removeFirstOrNull()
    }

    fun setAttachment(attachment: Any?) {
        this.attachment = attachment
    }

}

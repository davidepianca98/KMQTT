package io.github.davidepianca98.socket.udp

import node.dgram.Socket
import node.dgram.SocketEvent
import io.github.davidepianca98.socket.SocketState
import io.github.davidepianca98.toBuffer

internal actual class UDPSocket(
    private val socket: Socket,
    private val selectCallback: (attachment: Any?, state: SocketState) -> Boolean
) {

    private val queue = ArrayDeque<UDPReadData>()
    private var attachment: Any? = null

    init {
        socket.on(SocketEvent.MESSAGE) { msg, rinfo ->
            queue.add(UDPReadData(msg.toUByteArray(), rinfo.address, rinfo.port.toInt()))
            selectCallback(attachment, SocketState.READ)
        }
    }

    actual fun send(data: UByteArray, address: String, port: Int) {
        socket.send(data.toBuffer(), port, address) { _, _ -> }
        selectCallback(attachment, SocketState.WRITE)
    }

    actual fun read(): UDPReadData? {
        return queue.removeFirstOrNull()
    }

    fun setAttachment(attachment: Any?) {
        this.attachment = attachment
    }

}

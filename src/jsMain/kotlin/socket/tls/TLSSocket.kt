package socket.tls

import socket.ServerSocketLoop
import socket.tcp.Socket

actual class TLSSocket(
    socket: net.Socket,
    private val selectCallback: (attachment: Any?, state: ServerSocketLoop.SocketState) -> Boolean
) : Socket(socket, selectCallback) {

    override fun send(data: UByteArray) {
        TODO("TLS in JS not yet implemented")
    }

    override fun read(): UByteArray? {
        TODO("TLS in JS not yet implemented")
    }
}

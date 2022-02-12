package socket.tls

import socket.ServerSocketLoop
import socket.tcp.Socket

actual class TLSSocket(
    socket: net.Socket,
    private val selectCallback: (attachment: Any?, state: ServerSocketLoop.SocketState) -> Boolean
) : Socket(socket, selectCallback) {

}

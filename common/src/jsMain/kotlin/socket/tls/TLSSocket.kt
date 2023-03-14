package socket.tls

import socket.SocketState
import socket.tcp.Socket

actual class TLSSocket(
    socket: net.Socket,
    selectCallback: (attachment: Any?, state: SocketState) -> Boolean
) : Socket(socket, selectCallback)

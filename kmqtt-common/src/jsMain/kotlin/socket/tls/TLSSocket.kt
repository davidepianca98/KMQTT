package socket.tls

import socket.SocketState
import socket.tcp.Socket

public actual open class TLSSocket(
    socket: net.Socket,
    selectCallback: (attachment: Any?, state: SocketState) -> Boolean
) : Socket(socket, selectCallback)

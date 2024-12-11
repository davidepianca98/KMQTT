package io.github.davidepianca98.socket.tls

import io.github.davidepianca98.socket.SocketState
import io.github.davidepianca98.socket.tcp.Socket

public actual open class TLSSocket(
    socket: node.net.Socket,
    selectCallback: (attachment: Any?, state: SocketState) -> Boolean
) : Socket(socket, selectCallback)

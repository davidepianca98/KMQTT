package socket

import java.net.InetSocketAddress
import java.net.ServerSocket

actual class ServerSocket actual constructor(host: String, port: Int, backlog: Int) {

    private val socket = ServerSocket()

    init {
        socket.bind(InetSocketAddress(host, port), backlog)
    }

    actual fun accept(): Socket {
        return Socket(socket.accept())
    }

    actual fun close() {
        socket.close()
    }

}

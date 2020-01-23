package socket

import java.net.InetSocketAddress
import java.net.ServerSocket

actual class ServerSocket actual constructor(private val host: String, private val port: Int) {

    private val socket = ServerSocket()

    actual fun accept(): Socket {
        return Socket(socket.accept())
    }

    actual fun bind(backlog: Int) {
        socket.bind(InetSocketAddress(host, port), backlog)
    }

}

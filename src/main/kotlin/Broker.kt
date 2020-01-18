import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.SocketAddress
import kotlin.concurrent.thread

// TODO 3.2.2.3
class Broker(local: SocketAddress, backlog: Int = 128) {

    constructor(port: Int, host: String = "127.0.0.1") : this(InetSocketAddress(host, port))

    private val server = ServerSocket()

    init {
        server.bind(local, backlog)
    }

    fun listen() {
        while (true) {
            val client = server.accept()
            thread { ClientHandler(client).run() }
        }
    }
}

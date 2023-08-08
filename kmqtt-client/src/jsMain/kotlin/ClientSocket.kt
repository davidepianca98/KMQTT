import socket.tcp.Socket
import web.timers.setTimeout

public actual class ClientSocket actual constructor(
    address: String,
    port: Int,
    maximumPacketSize: Int,
    readTimeOut: Int,
    private val checkCallback: () -> Unit
) : Socket(node.net.Socket(), { _, _ ->
    checkCallback()
    true
}) {

    init {
        socket.connect(port, address)
        doLater()
    }

    private fun doLater() {
        setTimeout({
            checkCallback()
            doLater()
        }, 250)
    }
}

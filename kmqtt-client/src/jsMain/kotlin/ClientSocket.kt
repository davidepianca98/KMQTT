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

    private var open = true

    init {
        socket.connect(port, address)
        doLater()
    }

    private fun doLater() {
        if (open) {
            setTimeout({
                checkCallback()
                doLater()
            }, 250)
        }
    }

    override fun close() {
        open = false
        super.close()
    }
}

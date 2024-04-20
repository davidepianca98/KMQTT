import socket.tcp.Socket
import web.timers.setTimeout

public actual class ClientSocket actual constructor(
    address: String,
    port: Int,
    maximumPacketSize: Int,
    private val readTimeOut: Int,
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

    protected fun doLater() {
        if (open) {
            setTimeout({
                try {
                    checkCallback()
                    doLater()
                } catch (e: dynamic) {
                    close()
                }
            }, readTimeOut)
        }
    }

    override fun close() {
        open = false
        super.close()
    }
}

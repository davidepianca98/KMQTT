package socket.tls

import close
import mqtt.broker.Broker
import mqtt.broker.ClientConnection
import socket.ServerSocket
import socket.ServerSocketLoop
import socket.tcp.IOException

actual class TLSServerSocket actual constructor(
    private val broker: Broker,
    selectCallback: (attachment: Any?, state: ServerSocketLoop.SocketState) -> Boolean
) : ServerSocket(broker, selectCallback) {

    private val tlsServerContext = TLSServerContext(broker)

    override fun accept(socket: Int) {
        try {
            val engine = TLSEngine(tlsServerContext)
            clients[socket] = ClientConnection(TLSSocket(socket, engine, writeRequest, buffer), broker)
        } catch (e: IOException) {
            close(socket)
        }
    }

    override fun close() {
        super.close()
        tlsServerContext.close()
    }
}

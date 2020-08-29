package socket.tls

import close
import mqtt.broker.Broker
import mqtt.broker.ClientConnection
import socket.ServerSocket
import socket.tcp.IOException

actual class TLSServerSocket actual constructor(private val broker: Broker) : ServerSocket(broker) {

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

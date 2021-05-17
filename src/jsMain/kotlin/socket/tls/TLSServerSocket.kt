package socket.tls

import mqtt.broker.Broker
import socket.ServerSocket
import socket.ServerSocketLoop
import socket.tcp.Socket

actual class TLSServerSocket actual constructor(
    private val broker: Broker,
    private val selectCallback: (attachment: Any?, state: ServerSocketLoop.SocketState) -> Boolean
) : ServerSocket(broker, selectCallback) {

    init {
        TODO("TLS in JS not yet implemented")
    }

    override fun createSocket(socket: net.Socket): Socket {
        TODO("TLS in JS not yet implemented")
    }
}

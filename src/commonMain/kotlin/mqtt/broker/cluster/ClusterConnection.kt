package mqtt.broker.cluster

import socket.tcp.Socket
import socket.tcp.TCPEventHandler

class ClusterConnection(private val socket: Socket) : TCPEventHandler {
    // TODO implement subscribe, unsubscribe and publish and save retained messages

    override fun read(): UByteArray? {
        return socket.read()
    }

    override fun dataReceived(data: UByteArray) {
        TODO("Not yet implemented")
    }

    override fun sendRemaining() {
        socket.sendRemaining()
    }

    override fun closedGracefully() {
        socket.close()
    }

    override fun closedWithException() {
        socket.close()
    }
}

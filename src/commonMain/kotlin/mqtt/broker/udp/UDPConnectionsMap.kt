package mqtt.broker.udp

import mqtt.broker.Broker
import mqtt.broker.ClientConnection
import mqtt.broker.ClientConnection5
import socket.SocketInterface
import socket.udp.UDPEventHandler
import socket.udp.UDPSocket

class UDPConnectionsMap(private val socket: UDPSocket, private val broker: Broker) : UDPEventHandler, SocketInterface {

    private val udpSessions = mutableMapOf<String, ClientConnection>()
    private var currentKey = ""

    override fun dataReceived() {
        socket.read()?.let { data ->
            currentKey = data.sourceAddress + ":" + data.sourcePort
            if (udpSessions.containsKey(currentKey)) {
                udpSessions[currentKey]?.dataReceived(data.data)
            } else {
                udpSessions[currentKey] = ClientConnection5(this@UDPConnectionsMap, broker)
            }
        }
    }

    override fun send(data: UByteArray) {
        val ip = currentKey.split(":")
        socket.send(data, ip[0], ip[1].toInt())
    }

    override fun sendRemaining() {

    }

    override fun read(): UByteArray? {
        return socket.read()?.data
    }

    override fun close() {
        udpSessions.remove(currentKey)
    }
}

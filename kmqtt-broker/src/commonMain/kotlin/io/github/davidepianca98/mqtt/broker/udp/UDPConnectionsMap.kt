package io.github.davidepianca98.mqtt.broker.udp

import io.github.davidepianca98.mqtt.broker.Broker
import io.github.davidepianca98.mqtt.broker.ClientConnection
import io.github.davidepianca98.socket.SocketInterface
import io.github.davidepianca98.socket.udp.UDPEventHandler
import io.github.davidepianca98.socket.udp.UDPSocket

internal class UDPConnectionsMap(private val socket: UDPSocket, private val broker: Broker) : UDPEventHandler, SocketInterface {

    private val udpSessions = mutableMapOf<String, ClientConnection>()
    private var currentKey = ""

    override fun dataReceived() {
        socket.read()?.let { data ->
            currentKey = data.sourceAddress + ":" + data.sourcePort
            if (udpSessions.containsKey(currentKey)) {
                udpSessions[currentKey]?.dataReceived(data.data)
            } else {
                udpSessions[currentKey] = ClientConnection(this@UDPConnectionsMap, broker)
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

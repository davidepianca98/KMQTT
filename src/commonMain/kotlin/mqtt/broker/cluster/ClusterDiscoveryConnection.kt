package mqtt.broker.cluster

import kotlinx.serialization.protobuf.ProtoBuf
import mqtt.broker.Broker
import socket.UDPSocket

class ClusterDiscoveryConnection(private val socket: UDPSocket, private val broker: Broker) {

    fun dataReceived() {
        socket.read()?.let { data ->
            val packet = ProtoBuf.load(DiscoveryPacket.serializer(), data.data.toByteArray())
            if (packet.name != broker.cluster!!.name) {
                broker.addClusterConnection(data.address)
            }
        }
    }

    fun sendDiscovery(port: Int) {
        val packet = ProtoBuf.dump(DiscoveryPacket.serializer(), DiscoveryPacket(broker.cluster!!.name)).toUByteArray()
        socket.send(packet, "255.255.255.255", port)
    }
}

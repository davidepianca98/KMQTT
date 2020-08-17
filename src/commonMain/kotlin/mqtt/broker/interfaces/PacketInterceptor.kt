package mqtt.broker.interfaces

import mqtt.packets.MQTTPacket

interface PacketInterceptor {

    fun packetReceived(packet: MQTTPacket)
}

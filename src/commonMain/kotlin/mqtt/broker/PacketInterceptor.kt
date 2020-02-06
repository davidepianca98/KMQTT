package mqtt.broker

import mqtt.packets.mqttv5.MQTT5Packet

interface PacketInterceptor {

    fun packetReceived(packet: MQTT5Packet)
}

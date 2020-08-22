package mqtt.packets.mqttv4

import mqtt.packets.MQTTControlPacketType
import mqtt.packets.MQTTDeserializer
import mqtt.packets.mqtt.MQTTDisconnect
import socket.streams.ByteArrayOutputStream

class MQTT4Disconnect : MQTTDisconnect() {

    override fun toByteArray(): UByteArray {
        return ByteArrayOutputStream().wrapWithFixedHeader(MQTTControlPacketType.DISCONNECT, 0)
    }

    companion object : MQTTDeserializer {

        override fun fromByteArray(flags: Int, data: UByteArray): MQTT4Disconnect {
            checkFlags(flags)
            return MQTT4Disconnect()
        }
    }
}

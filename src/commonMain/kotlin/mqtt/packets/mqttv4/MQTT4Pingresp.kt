package mqtt.packets.mqttv4

import mqtt.packets.MQTTControlPacketType
import mqtt.packets.MQTTDeserializer
import mqtt.packets.mqttv5.MQTT5Pingresp
import socket.streams.ByteArrayOutputStream

class MQTT4Pingresp : MQTT4Packet {

    override fun toByteArray(): UByteArray {
        return ByteArrayOutputStream().wrapWithFixedHeader(MQTTControlPacketType.PINGRESP, 0)
    }

    companion object : MQTTDeserializer {

        override fun fromByteArray(flags: Int, data: UByteArray): MQTT5Pingresp {
            MQTT5Pingresp.checkFlags(flags)
            return MQTT5Pingresp()
        }
    }
}

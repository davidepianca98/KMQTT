package mqtt.packets.mqttv4

import mqtt.packets.MQTTControlPacketType
import mqtt.packets.MQTTDeserializer
import mqtt.packets.mqtt.MQTTPingreq
import socket.streams.ByteArrayOutputStream

class MQTT4Pingreq : mqtt.packets.mqtt.MQTTPingreq(), MQTT4Packet {

    override fun toByteArray(): UByteArray {
        return ByteArrayOutputStream().wrapWithFixedHeader(MQTTControlPacketType.PINGREQ, 0)
    }

    companion object : MQTTDeserializer {

        override fun fromByteArray(flags: Int, data: UByteArray): MQTT4Pingreq {
            checkFlags(flags)
            return MQTT4Pingreq()
        }
    }
}

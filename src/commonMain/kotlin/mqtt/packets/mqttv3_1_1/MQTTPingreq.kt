package mqtt.packets.mqttv3_1_1

import mqtt.packets.MQTTControlPacketType
import mqtt.packets.MQTTDeserializer
import socket.streams.ByteArrayOutputStream

class MQTTPingreq : MQTT3Packet {

    override fun toByteArray(): UByteArray {
        return ByteArrayOutputStream().wrapWithFixedHeader(MQTTControlPacketType.PINGREQ, 0)
    }

    companion object : MQTTDeserializer {

        override fun fromByteArray(flags: Int, data: UByteArray): MQTTPingreq {
            MQTTPingreq.checkFlags(flags)
            return MQTTPingreq()
        }
    }
}

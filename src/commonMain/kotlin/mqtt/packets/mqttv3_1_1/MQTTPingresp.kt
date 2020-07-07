package mqtt.packets.mqttv3_1_1

import mqtt.packets.MQTTControlPacketType
import mqtt.packets.MQTTDeserializer
import mqtt.packets.mqttv5.MQTTPingresp
import socket.streams.ByteArrayOutputStream

class MQTTPingresp : MQTT3Packet {

    override fun toByteArray(): UByteArray {
        return ByteArrayOutputStream().wrapWithFixedHeader(MQTTControlPacketType.PINGRESP, 0)
    }

    companion object : MQTTDeserializer {

        override fun fromByteArray(flags: Int, data: UByteArray): MQTTPingresp {
            MQTTPingresp.checkFlags(flags)
            return MQTTPingresp()
        }
    }
}

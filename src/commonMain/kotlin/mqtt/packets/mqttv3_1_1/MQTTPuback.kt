package mqtt.packets.mqttv3_1_1

import mqtt.packets.MQTTControlPacketType
import mqtt.packets.MQTTDeserializer
import socket.streams.ByteArrayInputStream
import socket.streams.ByteArrayOutputStream

class MQTTPuback(
    val packetId: UInt
) : MQTT3Packet {

    companion object : MQTTDeserializer {

        override fun fromByteArray(flags: Int, data: UByteArray): MQTTPuback {
            mqtt.packets.mqttv5.MQTTPuback.checkFlags(flags)
            val inStream = ByteArrayInputStream(data)
            val packetId = inStream.read2BytesInt()
            return MQTTPuback(packetId)
        }
    }

    override fun toByteArray(): UByteArray {
        val outStream = ByteArrayOutputStream()

        outStream.write2BytesInt(packetId)

        return outStream.wrapWithFixedHeader(MQTTControlPacketType.PUBACK, 0)
    }
}

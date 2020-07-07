package mqtt.packets.mqttv3_1_1

import mqtt.packets.MQTTControlPacketType
import mqtt.packets.MQTTDeserializer
import socket.streams.ByteArrayInputStream
import socket.streams.ByteArrayOutputStream

class MQTTPubcomp(
    val packetId: UInt
) : MQTT3Packet {

    override fun toByteArray(): UByteArray {
        val outStream = ByteArrayOutputStream()

        outStream.write2BytesInt(packetId)

        return outStream.wrapWithFixedHeader(MQTTControlPacketType.PUBCOMP, 0)
    }

    companion object : MQTTDeserializer {

        override fun fromByteArray(flags: Int, data: UByteArray): MQTTPubcomp {
            MQTTPubcomp.checkFlags(flags)
            val inStream = ByteArrayInputStream(data)
            val packetId = inStream.read2BytesInt()
            return MQTTPubcomp(packetId)
        }
    }
}

package mqtt.packets.mqttv4

import mqtt.packets.MQTTControlPacketType
import mqtt.packets.MQTTDeserializer
import mqtt.packets.mqtt.MQTTPubcomp
import socket.streams.ByteArrayInputStream
import socket.streams.ByteArrayOutputStream

class MQTT4Pubcomp(
    packetId: UInt
) : MQTTPubcomp(packetId) {

    override fun toByteArray(): UByteArray {
        val outStream = ByteArrayOutputStream()

        outStream.write2BytesInt(packetId)

        return outStream.wrapWithFixedHeader(MQTTControlPacketType.PUBCOMP, 0)
    }

    companion object : MQTTDeserializer {

        override fun fromByteArray(flags: Int, data: UByteArray): MQTT4Pubcomp {
            MQTT4Pubcomp.checkFlags(flags)
            val inStream = ByteArrayInputStream(data)
            val packetId = inStream.read2BytesInt()
            return MQTT4Pubcomp(packetId)
        }
    }
}
